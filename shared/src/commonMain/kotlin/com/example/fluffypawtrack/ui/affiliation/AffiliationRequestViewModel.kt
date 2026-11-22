package com.example.fluffypawtrack.ui.affiliation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.model.Organization
import com.example.fluffypawtrack.domain.model.OrganizationType
import com.example.fluffypawtrack.domain.repository.OrganizationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Per-field validation messages; null means the field is valid. */
data class AffiliationErrors(
    val name: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val description: String? = null,
    val photo: String? = null,
) {
    val hasErrors: Boolean get() = listOf(name, address, phone, email, description, photo).any { it != null }
}

data class AffiliationRequestUiState(
    val name: String = "",
    val type: OrganizationType = OrganizationType.Shelter,
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val description: String = "",
    /** Required photo of the place or its logo (local reference from the photo picker). */
    val photoRef: String? = null,
    val errors: AffiliationErrors = AffiliationErrors(),
    /** Form-level error from the repository. */
    val message: String? = null,
    val isSubmitting: Boolean = false,
    /** The pending organization once the request was sent. */
    val submitted: Organization? = null,
) {
    /** Affiliates are shelters, rescuers and adoption centers (REQUIREMENTS §4). */
    val typeOptions: List<OrganizationType> get() = AffiliationTypes
}

private val AffiliationTypes = listOf(OrganizationType.Shelter, OrganizationType.AdoptionCenter)
private val EmailShape = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

private fun digitsOf(phone: String) = phone.filter { it.isDigit() }

internal fun validateAffiliation(state: AffiliationRequestUiState) = AffiliationErrors(
    name = if (state.name.isBlank()) "Ingresa el nombre de la organización" else null,
    address = if (state.address.isBlank()) "Ingresa la dirección" else null,
    phone = if (digitsOf(state.phone).length != 10) "Ingresa un teléfono de 10 dígitos" else null,
    email = when {
        state.email.isBlank() -> "Ingresa el correo"
        !EmailShape.matches(state.email.trim()) -> "Ingresa un correo válido"
        else -> null
    },
    description = if (state.description.isBlank()) "Describe brevemente a tu organización" else null,
    photo = if (state.photoRef == null) "Agrega una foto del lugar o su logo" else null,
)

/** Affiliation request form for shelters, rescuers and adoption centers. */
class AffiliationRequestViewModel(
    private val organizationRepository: OrganizationRepository,
) : ViewModel() {

    private val state = MutableStateFlow(AffiliationRequestUiState())
    val uiState: StateFlow<AffiliationRequestUiState> = state.asStateFlow()

    fun onNameChange(value: String) = state.update { it.copy(name = value, errors = it.errors.copy(name = null)) }

    fun onTypeChange(value: OrganizationType) = state.update { it.copy(type = value) }

    fun onAddressChange(value: String) =
        state.update { it.copy(address = value, errors = it.errors.copy(address = null)) }

    fun onPhoneChange(value: String) = state.update { it.copy(phone = value, errors = it.errors.copy(phone = null)) }

    fun onEmailChange(value: String) = state.update { it.copy(email = value, errors = it.errors.copy(email = null)) }

    fun onDescriptionChange(value: String) =
        state.update { it.copy(description = value, errors = it.errors.copy(description = null)) }

    fun onPhotoSelected(ref: String) = state.update { it.copy(photoRef = ref, errors = it.errors.copy(photo = null)) }

    fun onPhotoRemoved() = state.update { it.copy(photoRef = null) }

    fun onSubmit() {
        val current = state.value
        if (current.isSubmitting) return
        val errors = validateAffiliation(current)
        val photo = current.photoRef
        if (errors.hasErrors || photo == null) {
            state.update { it.copy(errors = errors) }
            return
        }
        state.update { it.copy(isSubmitting = true, message = null) }
        viewModelScope.launch {
            organizationRepository.requestAffiliation(
                type = current.type,
                name = current.name.trim(),
                address = current.address.trim(),
                phone = digitsOf(current.phone),
                email = current.email.trim(),
                description = current.description.trim(),
                photoUrl = photo,
            )
                .onSuccess { org -> state.update { it.copy(isSubmitting = false, submitted = org) } }
                .onFailure { e ->
                    state.update { it.copy(isSubmitting = false, message = e.message ?: "No se pudo enviar la solicitud") }
                }
        }
    }
}
