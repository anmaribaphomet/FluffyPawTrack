package com.example.fluffypawtrack.ui.affiliate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.model.OrganizationType
import com.example.fluffypawtrack.domain.model.ReportStatus
import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.domain.repository.OrganizationRepository
import com.example.fluffypawtrack.domain.repository.ReportRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class AffiliateHomeUiState(
    val organizationName: String = "",
    /** Adoption shortcuts ("Mis fichas", "Solicitudes de adopción") are only for adoption centers. */
    val isAdoptionCenter: Boolean = false,
    /** Reports waiting in the inbox that nobody took yet. */
    val pendingReports: Int = 0,
    val openCases: Int = 0,
    val blockedMessage: String? = null,
    val loaded: Boolean = false,
)

/** Affiliate home: the organization name and counters for the shortcuts. */
@OptIn(ExperimentalCoroutinesApi::class)
class AffiliateHomeViewModel(
    authRepository: AuthRepository,
    organizationRepository: OrganizationRepository,
    reportRepository: ReportRepository,
) : ViewModel() {

    val uiState: StateFlow<AffiliateHomeUiState> = authRepository.currentUser.flatMapLatest { user ->
        val organizationId = user.affiliateOrganizationId
        if (organizationId == null) {
            flowOf(AffiliateHomeUiState(blockedMessage = AFFILIATE_ONLY_MESSAGE, loaded = true))
        } else {
            combine(
                flow { emit(organizationRepository.getOrganization(organizationId)) },
                reportRepository.observeInbox(organizationId),
                reportRepository.observeCasesOf(organizationId),
            ) { organization, inbox, cases ->
                AffiliateHomeUiState(
                    organizationName = organization?.name.orEmpty(),
                    isAdoptionCenter = organization?.type == OrganizationType.AdoptionCenter,
                    pendingReports = inbox.count { it.assignedOrganizationId == null && it.status == ReportStatus.Pending },
                    openCases = cases.count { it.status != ReportStatus.Closed },
                    loaded = true,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AffiliateHomeUiState())
}
