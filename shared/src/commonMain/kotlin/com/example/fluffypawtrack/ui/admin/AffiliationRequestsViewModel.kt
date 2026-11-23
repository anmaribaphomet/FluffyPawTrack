package com.example.fluffypawtrack.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.model.Organization
import com.example.fluffypawtrack.domain.model.OrganizationRules
import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.domain.repository.OrganizationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The approve or reject decision waiting for the admin's confirmation. Rejecting needs a
 * [reason], which the requester receives; [error] is the repository's answer to the last try.
 */
data class AffiliationReview(
    val organization: Organization,
    val approve: Boolean,
    val reason: String = "",
    val error: String? = null,
) {
    val canConfirm: Boolean get() = approve || reason.isNotBlank()
}

data class AffiliationRequestsUiState(
    val loaded: Boolean = false,
    /** Set when the signed-in account is not the admin; the screen shows only this text. */
    val blockedMessage: String? = null,
    /** Pending affiliations, oldest first. */
    val pending: List<Organization> = emptyList(),
    val confirming: AffiliationReview? = null,
    val isWorking: Boolean = false,
    /** Readable error from the repository or the server. */
    val message: String? = null,
    /** Result of the last review. */
    val notice: String? = null,
)

/** Admin → "Solicitudes de afiliación" (RF-07): approve or reject pending organizations. */
class AffiliationRequestsViewModel(
    authRepository: AuthRepository,
    private val organizationRepository: OrganizationRepository,
) : ViewModel() {

    private val state = MutableStateFlow(AffiliationRequestsUiState())
    val uiState: StateFlow<AffiliationRequestsUiState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.whileAdmin { organizationRepository.observePendingAffiliations() }.collect { pending ->
                state.update {
                    if (pending == null) {
                        it.copy(loaded = true, blockedMessage = ADMIN_ONLY_MESSAGE, pending = emptyList(), confirming = null)
                    } else {
                        it.copy(loaded = true, blockedMessage = null, pending = pending)
                    }
                }
            }
        }
    }

    fun requestReview(organizationId: String, approve: Boolean) {
        val organization = state.value.pending.firstOrNull { it.id == organizationId } ?: return
        state.update { it.copy(confirming = AffiliationReview(organization, approve), notice = null) }
    }

    fun onReasonChange(value: String) = state.update {
        val reason = value.take(OrganizationRules.MAX_REJECTION_REASON_LENGTH)
        it.copy(confirming = it.confirming?.copy(reason = reason, error = null))
    }

    fun dismissReview() {
        // Keep the dialog while a request is in flight so a failure is never shown to nobody.
        if (state.value.isWorking) return
        state.update { it.copy(confirming = null) }
    }

    fun confirmReview() {
        val review = state.value.confirming ?: return
        if (state.value.isWorking || !review.canConfirm) return
        // A rejection keeps its dialog open until it succeeds, so a failure shows next to the reason.
        state.update {
            it.copy(confirming = if (review.approve) null else review, isWorking = true, message = null, notice = null)
        }
        viewModelScope.launch {
            val reason = if (review.approve) null else review.reason.trim()
            organizationRepository.reviewAffiliation(review.organization.id, review.approve, reason)
                .onSuccess { reviewed ->
                    val notice = if (review.approve) {
                        "Se aprobó ${reviewed.name}. Ya aparece en el mapa."
                    } else {
                        "Se rechazó la solicitud de ${reviewed.name}."
                    }
                    state.update { it.copy(confirming = null, isWorking = false, notice = notice) }
                }
                .onFailure { e ->
                    val message = e.message ?: "No se pudo revisar la solicitud"
                    state.update {
                        if (review.approve) {
                            it.copy(isWorking = false, message = message)
                        } else {
                            it.copy(isWorking = false, confirming = it.confirming?.copy(error = message))
                        }
                    }
                }
        }
    }

    fun dismissMessage() = state.update { it.copy(message = null, notice = null) }
}
