package com.example.fluffypawtrack.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.model.Report
import com.example.fluffypawtrack.domain.model.ReportCategory
import com.example.fluffypawtrack.domain.model.ReportStatus
import com.example.fluffypawtrack.domain.model.StatusHistoryEntry
import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.domain.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReportSupervisionUiState(
    val loaded: Boolean = false,
    val blockedMessage: String? = null,
    /** Every report, hidden and closed ones included, newest first. */
    val allReports: List<Report> = emptyList(),
    val filter: SupervisionFilter = SupervisionFilter.All,
    /** Null shows every category. */
    val category: ReportCategory? = null,
) {
    val reports: List<Report>
        get() = allReports.filter { report ->
            val matchesFilter = when (filter) {
                SupervisionFilter.All -> true
                SupervisionFilter.Hidden -> report.hidden
                SupervisionFilter.Closed -> report.status == ReportStatus.Closed
            }
            matchesFilter && (category == null || report.category == category)
        }
}

/** Admin → "Supervisión de reportes" (RF-07): every report with filters. */
class ReportSupervisionViewModel(
    authRepository: AuthRepository,
    reportRepository: ReportRepository,
) : ViewModel() {

    private val state = MutableStateFlow(ReportSupervisionUiState())
    val uiState: StateFlow<ReportSupervisionUiState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.whileAdmin { reportRepository.observeAllForAdmin() }.collect { reports ->
                state.update {
                    if (reports == null) {
                        it.copy(loaded = true, blockedMessage = ADMIN_ONLY_MESSAGE, allReports = emptyList())
                    } else {
                        it.copy(loaded = true, blockedMessage = null, allReports = reports)
                    }
                }
            }
        }
    }

    fun onFilter(filter: SupervisionFilter) = state.update { it.copy(filter = filter) }

    fun onCategory(category: ReportCategory?) = state.update { it.copy(category = category) }
}

data class AdminReportDetailUiState(
    val loaded: Boolean = false,
    /** Not the admin, or the report does not exist. */
    val blockedMessage: String? = null,
    val report: Report? = null,
    /** RF-04 history, oldest first. */
    val history: List<StatusHistoryEntry> = emptyList(),
    val confirmingClose: Boolean = false,
    val isWorking: Boolean = false,
    val message: String? = null,
    val notice: String? = null,
) {
    val canClose: Boolean get() = report != null && report.status != ReportStatus.Closed
}

/** Admin detail of one report: hide or show it, and close the case (RF-07). */
class AdminReportDetailViewModel(
    authRepository: AuthRepository,
    private val reportRepository: ReportRepository,
    private val reportId: String,
) : ViewModel() {

    private val state = MutableStateFlow(AdminReportDetailUiState())
    val uiState: StateFlow<AdminReportDetailUiState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.whileAdmin {
                combine(
                    reportRepository.observeAllForAdmin().map { list -> list.firstOrNull { it.id == reportId } },
                    reportRepository.observeStatusHistory(reportId),
                ) { report, history -> report to history }
            }.collect { loaded ->
                state.update {
                    val report = loaded?.first
                    when {
                        loaded == null -> it.copy(loaded = true, blockedMessage = ADMIN_ONLY_MESSAGE, report = null, history = emptyList())
                        report == null -> it.copy(loaded = true, blockedMessage = NOT_FOUND, report = null, history = emptyList())
                        else -> it.copy(loaded = true, blockedMessage = null, report = report, history = loaded.second)
                    }
                }
            }
        }
    }

    fun toggleHidden() {
        val report = state.value.report ?: return
        if (state.value.isWorking || state.value.blockedMessage != null) return
        val hidden = !report.hidden
        state.update { it.copy(isWorking = true, message = null, notice = null) }
        viewModelScope.launch {
            reportRepository.setHidden(report.id, hidden)
                .onSuccess {
                    val notice = if (hidden) "El reporte quedó oculto del mapa y de las bandejas." else "El reporte vuelve a estar visible."
                    state.update { it.copy(isWorking = false, notice = notice) }
                }
                .onFailure { e -> state.update { it.copy(isWorking = false, message = e.message ?: "No se pudo actualizar el reporte") } }
        }
    }

    fun requestClose() {
        if (!state.value.canClose) return
        state.update { it.copy(confirmingClose = true, notice = null) }
    }

    fun dismissClose() = state.update { it.copy(confirmingClose = false) }

    fun confirmClose() {
        val report = state.value.report ?: return
        if (!state.value.confirmingClose || state.value.isWorking) return
        state.update { it.copy(confirmingClose = false, isWorking = true, message = null, notice = null) }
        viewModelScope.launch {
            reportRepository.setStatus(report.id, ReportStatus.Closed)
                .onSuccess { state.update { it.copy(isWorking = false, notice = "El caso se cerró.") } }
                .onFailure { e -> state.update { it.copy(isWorking = false, message = e.message ?: "No se pudo cerrar el caso") } }
        }
    }

    fun dismissMessage() = state.update { it.copy(message = null, notice = null) }

    private companion object {
        const val NOT_FOUND = "El reporte no existe"
    }
}
