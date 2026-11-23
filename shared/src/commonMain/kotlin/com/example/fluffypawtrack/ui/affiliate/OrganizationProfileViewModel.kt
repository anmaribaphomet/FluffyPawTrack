package com.example.fluffypawtrack.ui.affiliate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.model.Organization
import com.example.fluffypawtrack.domain.model.OrganizationProfile
import com.example.fluffypawtrack.domain.model.OrganizationRules
import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.domain.repository.OrganizationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrganizationProfileUiState(
    /** Read-only data (name, type, address, location, capacity) of the organization. */
    val organization: Organization? = null,
    val description: String = "",
    val schedule: String = "",
    val phone: String = "",
    val email: String = "",
    val social: String = "",
    val photoRef: String? = null,
    val photoError: String? = null,
    val blockedMessage: String? = null,
    val isSaving: Boolean = false,
    /** Readable repository failure. */
    val message: String? = null,
    /** True after the last save succeeded, until the next edit. */
    val saved: Boolean = false,
)

/** "Perfil de la organización": the affiliate edits the public data of their own organization. */
class OrganizationProfileViewModel(
    private val authRepository: AuthRepository,
    private val organizationRepository: OrganizationRepository,
) : ViewModel() {

    private val state = MutableStateFlow(OrganizationProfileUiState())
    val uiState: StateFlow<OrganizationProfileUiState> = state.asStateFlow()

    init {
        val organizationId = authRepository.currentUser.value.affiliateOrganizationId
        if (organizationId == null) {
            state.update { it.copy(blockedMessage = AFFILIATE_ONLY_MESSAGE) }
        } else {
            viewModelScope.launch {
                val organization = organizationRepository.getOrganization(organizationId)
                if (organization == null) {
                    state.update { it.copy(blockedMessage = "No encontramos los datos de tu organización.") }
                } else {
                    state.update { it.from(organization) }
                }
            }
        }
    }

    fun onDescriptionChange(value: String) = edit { it.copy(description = value) }

    fun onScheduleChange(value: String) = edit { it.copy(schedule = value) }

    fun onPhoneChange(value: String) = edit { it.copy(phone = value) }

    fun onEmailChange(value: String) = edit { it.copy(email = value) }

    fun onSocialChange(value: String) = edit { it.copy(social = value) }

    fun onPhotoSelected(ref: String) = edit { it.copy(photoRef = ref, photoError = null) }

    fun onPhotoRemoved() = edit { it.copy(photoRef = null) }

    fun onSave() {
        val current = state.value
        val organization = current.organization ?: return
        if (current.isSaving) return
        val profile = OrganizationProfile(
            description = current.description.trim(),
            schedule = current.schedule.trim(),
            phone = current.phone.trim(),
            email = current.email.trim(),
            social = current.social.trim().takeIf { it.isNotEmpty() },
            photoUrl = current.photoRef.orEmpty(),
        )
        OrganizationRules.problemOf(profile)?.let { problem ->
            state.update { it.copy(photoError = problem) }
            return
        }
        state.update { it.copy(isSaving = true, message = null) }
        viewModelScope.launch {
            organizationRepository.updateOwnOrganization(organization.id, profile)
                .onSuccess { updated -> state.update { it.from(updated).copy(isSaving = false, saved = true) } }
                .onFailure { error ->
                    state.update { it.copy(isSaving = false, message = error.message ?: "No se pudieron guardar los cambios") }
                }
        }
    }

    private fun edit(transform: (OrganizationProfileUiState) -> OrganizationProfileUiState) =
        state.update { transform(it).copy(saved = false) }

    private fun OrganizationProfileUiState.from(organization: Organization) = copy(
        organization = organization,
        description = organization.description,
        schedule = organization.schedule,
        phone = organization.phone,
        email = organization.email,
        social = organization.social.orEmpty(),
        photoRef = organization.photoUrl.takeIf { it.isNotBlank() },
    )
}
