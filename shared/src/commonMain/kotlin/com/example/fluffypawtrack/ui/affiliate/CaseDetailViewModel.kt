package com.example.fluffypawtrack.ui.affiliate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.model.Report
import com.example.fluffypawtrack.domain.model.ReportStatus
import com.example.fluffypawtrack.domain.model.StatusHistoryEntry
import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.domain.repository.OrganizationRepository
import com.example.fluffypawtrack.domain.repository.ReportRepository
import com.example.fluffypawtrack.ui.common.label
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal const val REPORT_NOT_FOUND = "Este reporte ya no está disponible."

data class CaseDetailUiState(
    val report: Report? = null,
    /** RF-04 history, oldest first. */
    val history: List<StatusHistoryEntry> = emptyList(),
    /** "Tomar caso": a pending case that no organization took yet. */
    val canTake: Boolean = false,
    /** The one forward step the own organization may set next, or null when there is none. */
    val nextStatus: ReportStatus? = null,
    /** "En camino (Albergue X)" when another organization has the case; no actions then. */
    val takenByOtherLabel: String? = null,
    val confirmingClose: Boolean = false,
    val isWorking: Boolean = false,
    /** Readable server or repository error. */
    val message: String? = null,
    val loaded: Boolean = false,
)

private data class CaseLocal(
    val report: Report? = null,
    val otherOrganizationName: String? = null,
    val confirmingClose: Boolean = false,
    val isWorking: Boolean = false,
    val message: String? = null,
    val loaded: Boolean = false,
    /** Bumped after each change so a one-shot history source is read again. */
    val version: Int = 0,
)

/** "Detalle del reporte → Tomar caso → cambiar estado" (§6 Afiliado, RF-04). */
@OptIn(ExperimentalCoroutinesApi::class)
class CaseDetailViewModel(
    private val reportId: String,
    private val authRepository: AuthRepository,
    private val reportRepository: ReportRepository,
    private val organizationRepository: OrganizationRepository,
) : ViewModel() {

    private val local = MutableStateFlow(CaseLocal())

    private val history = local
        .map { it.version }
        .distinctUntilChanged()
        .flatMapLatest { reportRepository.observeStatusHistory(reportId) }

    val uiState: StateFlow<CaseDetailUiState> =
        combine(local, history, authRepository.currentUser) { current, entries, user ->
            val report = current.report
            val ownOrganization = user.affiliateOrganizationId
            val assigned = report?.assignedOrganizationId
            val isOwn = assigned != null && assigned == ownOrganization
            val open = report != null && report.status != ReportStatus.Closed
            CaseDetailUiState(
                report = report,
                history = entries,
                canTake = ownOrganization != null && open && assigned == null && report?.status == ReportStatus.Pending,
                nextStatus = report?.status?.next()?.takeIf { isOwn && open },
                takenByOtherLabel = if (open && assigned != null && !isOwn) {
                    "${report?.status?.label} (${current.otherOrganizationName ?: "otro albergue"})"
                } else {
                    null
                },
                confirmingClose = current.confirmingClose,
                isWorking = current.isWorking,
                message = current.message,
                loaded = current.loaded,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CaseDetailUiState())

    init {
        viewModelScope.launch { reload(message = null) }
    }

    fun takeCase() = act { reportRepository.takeReport(reportId) }

    /** Advances one step; "Caso cerrado" asks for confirmation first. */
    fun advance() {
        val next = uiState.value.nextStatus ?: return
        if (next == ReportStatus.Closed) {
            local.update { it.copy(confirmingClose = true) }
        } else {
            act { reportRepository.setStatus(reportId, next) }
        }
    }

    fun confirmClose() {
        local.update { it.copy(confirmingClose = false) }
        act { reportRepository.setStatus(reportId, ReportStatus.Closed) }
    }

    fun dismissClose() = local.update { it.copy(confirmingClose = false) }

    fun dismissMessage() = local.update { it.copy(message = null) }

    private fun act(action: suspend () -> Result<Report>) {
        if (local.value.isWorking) return
        local.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            action()
                .onSuccess { report -> show(report, message = null) }
                // The case may have changed meanwhile (for example, taken by another shelter).
                .onFailure { error -> reload(message = error.message ?: "No se pudo actualizar el caso") }
        }
    }

    private suspend fun reload(message: String?) {
        val report = reportRepository.getReport(reportId)
        show(report, message ?: REPORT_NOT_FOUND.takeIf { report == null })
    }

    private suspend fun show(report: Report?, message: String?) {
        val ownOrganization = authRepository.currentUser.value.affiliateOrganizationId
        val otherName = report?.assignedOrganizationId
            ?.takeIf { it != ownOrganization }
            ?.let { organizationRepository.getOrganization(it)?.name }
        local.update {
            it.copy(
                report = report,
                otherOrganizationName = otherName,
                isWorking = false,
                message = message,
                loaded = true,
                version = it.version + 1,
            )
        }
    }
}

private fun ReportStatus.next(): ReportStatus? = ReportStatus.entries.getOrNull(ordinal + 1)
