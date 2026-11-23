package com.example.fluffypawtrack.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.model.Organization
import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.domain.repository.OrganizationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The approve or reject decision waiting for the admin's confirmation. */
data class AffiliationReview(val organization: Organization, val approve: Boolean)

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

    fun dismissReview() = state.update { it.copy(confirming = null) }

    fun confirmReview() {
        val review = state.value.confirming ?: return
        if (state.value.isWorking) return
        state.update { it.copy(confirming = null, isWorking = true, message = null, notice = null) }
        viewModelScope.launch {
            organizationRepository.reviewAffiliation(review.organization.id, review.approve)
                .onSuccess { reviewed ->
                    val notice = if (review.approve) {
                        "Se aprobó ${reviewed.name}. Ya aparece en el mapa."
                    } else {
                        "Se rechazó la solicitud de ${reviewed.name}."
                    }
                    state.update { it.copy(isWorking = false, notice = notice) }
                }
                .onFailure { e ->
                    state.update { it.copy(isWorking = false, message = e.message ?: "No se pudo revisar la solicitud") }
                }
        }
    }

    fun dismissMessage() = state.update { it.copy(message = null, notice = null) }
}
