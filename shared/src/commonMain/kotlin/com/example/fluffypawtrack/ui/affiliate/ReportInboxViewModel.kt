package com.example.fluffypawtrack.ui.affiliate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.model.Priority
import com.example.fluffypawtrack.domain.model.Report
import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.domain.repository.ReportRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlin.time.ExperimentalTime

data class ReportInboxUiState(
    /** The reports after the priority filter and the chosen order. */
    val reports: List<Report> = emptyList(),
    /** Null means "Todas". */
    val priorityFilter: Priority? = null,
    val urgentFirst: Boolean = false,
    val blockedMessage: String? = null,
    val loaded: Boolean = false,
)

/** "Bandeja de reportes" (§6 Afiliado): open reports no other organization took, by priority. */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class ReportInboxViewModel(
    authRepository: AuthRepository,
    reportRepository: ReportRepository,
) : ViewModel() {

    private val filter = MutableStateFlow<Priority?>(null)
    private val urgentFirst = MutableStateFlow(false)

    private val inbox = authRepository.currentUser.flatMapLatest { user ->
        val organizationId = user.affiliateOrganizationId
        if (organizationId == null) flowOf(null) else reportRepository.observeInbox(organizationId)
    }

    val uiState: StateFlow<ReportInboxUiState> = combine(inbox, filter, urgentFirst) { reports, priority, urgent ->
        val filtered = reports.orEmpty().filter { priority == null || it.priority == priority }
        ReportInboxUiState(
            // The repository already returns the newest first; the sort is stable.
            reports = if (urgent) filtered.sortedByDescending { it.priority } else filtered,
            priorityFilter = priority,
            urgentFirst = urgent,
            blockedMessage = AFFILIATE_ONLY_MESSAGE.takeIf { reports == null },
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportInboxUiState())

    fun selectPriority(priority: Priority?) {
        filter.value = priority
    }

    fun setUrgentFirst(enabled: Boolean) {
        urgentFirst.value = enabled
    }
}
