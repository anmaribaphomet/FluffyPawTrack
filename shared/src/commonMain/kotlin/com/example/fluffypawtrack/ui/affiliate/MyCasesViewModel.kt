package com.example.fluffypawtrack.ui.affiliate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.model.Report
import com.example.fluffypawtrack.domain.model.ReportStatus
import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.domain.repository.ReportRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MyCasesUiState(
    val cases: List<Report> = emptyList(),
    val blockedMessage: String? = null,
    val loaded: Boolean = false,
)

/** "Mis casos en curso": cases of the organization that are not closed yet. */
@OptIn(ExperimentalCoroutinesApi::class)
class MyCasesViewModel(
    authRepository: AuthRepository,
    reportRepository: ReportRepository,
) : ViewModel() {

    val uiState: StateFlow<MyCasesUiState> = authRepository.currentUser.flatMapLatest { user ->
        val organizationId = user.affiliateOrganizationId
        if (organizationId == null) {
            flowOf(MyCasesUiState(blockedMessage = AFFILIATE_ONLY_MESSAGE, loaded = true))
        } else {
            reportRepository.observeCasesOf(organizationId).map { cases ->
                MyCasesUiState(cases = cases.filter { it.status != ReportStatus.Closed }, loaded = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyCasesUiState())
}
