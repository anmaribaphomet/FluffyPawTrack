package com.example.fluffypawtrack.ui.affiliate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.model.AdoptionListing
import com.example.fluffypawtrack.domain.model.AdoptionRequest
import com.example.fluffypawtrack.domain.model.ListingStatus
import com.example.fluffypawtrack.domain.model.Role
import com.example.fluffypawtrack.domain.repository.AdoptionRepository
import com.example.fluffypawtrack.domain.repository.AdoptionRequestRepository
import com.example.fluffypawtrack.domain.repository.AuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal const val CENTER_ONLY_MESSAGE = "Esta sección es solo para cuentas de centros de adopción."

/** One animal of the center with its requests, oldest first. */
data class AnimalRequestsGroup(
    val listing: AdoptionListing,
    val requests: List<AdoptionRequest>,
)

/** The reject dialog: which request and the reason typed so far. */
data class RejectDialogState(
    val requestId: String,
    val reason: String = "",
    val error: String? = null,
)

data class CenterAdoptionRequestsUiState(
    val groups: List<AnimalRequestsGroup> = emptyList(),
    val blockedMessage: String? = null,
    val rejecting: RejectDialogState? = null,
    /** Readable repository failure. */
    val message: String? = null,
    val loaded: Boolean = false,
)

/** Center side of RF-06: "Solicitudes de adopción" grouped by animal, for an affiliate account. */
@OptIn(ExperimentalCoroutinesApi::class)
class CenterAdoptionRequestsViewModel(
    private val authRepository: AuthRepository,
    private val adoptionRequestRepository: AdoptionRequestRepository,
    private val adoptionRepository: AdoptionRepository,
) : ViewModel() {

    private val local = MutableStateFlow(CenterAdoptionRequestsUiState())

    private val groups = authRepository.currentUser.flatMapLatest { user ->
        val organizationId = user?.organizationId?.takeIf { user.role == Role.Affiliate }
        if (organizationId == null) {
            flowOf<List<AnimalRequestsGroup>?>(null)
        } else {
            combine(
                adoptionRepository.observeListingsBy(organizationId),
                adoptionRequestRepository.observeByOrganization(organizationId),
            ) { listings, requests ->
                listings
                    .map { listing -> AnimalRequestsGroup(listing, requests.filter { it.listingId == listing.id }) }
                    // Adopted animals stay only while they still show their answered requests.
                    .filter { it.listing.status == ListingStatus.Available || it.requests.isNotEmpty() }
            }
        }
    }

    val uiState: StateFlow<CenterAdoptionRequestsUiState> = combine(groups, local) { list, current ->
        current.copy(
            groups = list.orEmpty(),
            blockedMessage = CENTER_ONLY_MESSAGE.takeIf { list == null },
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CenterAdoptionRequestsUiState())

    fun approve(requestId: String) = withAffiliate { userId ->
        adoptionRequestRepository.approve(requestId, userId).onFailure { showMessage(it) }
    }

    fun startReject(requestId: String) = local.update { it.copy(rejecting = RejectDialogState(requestId)) }

    fun onRejectReasonChange(value: String) =
        local.update { it.copy(rejecting = it.rejecting?.copy(reason = value, error = null)) }

    fun dismissReject() = local.update { it.copy(rejecting = null) }

    fun confirmReject() {
        val dialog = local.value.rejecting ?: return
        withAffiliate { userId ->
            adoptionRequestRepository.reject(dialog.requestId, userId, dialog.reason)
                .onSuccess { local.update { it.copy(rejecting = null) } }
                .onFailure { error -> local.update { it.copy(rejecting = it.rejecting?.copy(error = error.message)) } }
        }
    }

    fun markAdopted(listingId: String) = withAffiliate { userId ->
        adoptionRepository.markAdopted(listingId, userId).onFailure { showMessage(it) }
    }

    fun dismissMessage() = local.update { it.copy(message = null) }

    private fun showMessage(error: Throwable) = local.update { it.copy(message = error.message) }

    private fun withAffiliate(action: suspend (String) -> Unit) {
        val userId = authRepository.currentUser.value?.id ?: return
        viewModelScope.launch { action(userId) }
    }
}
