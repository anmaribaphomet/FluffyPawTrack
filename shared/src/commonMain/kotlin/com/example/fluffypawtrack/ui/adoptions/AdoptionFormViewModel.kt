package com.example.fluffypawtrack.ui.adoptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.repository.AdoptionRepository
import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.domain.repository.OrganizationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Housing(val label: String) { House("Casa"), Apartment("Departamento") }

data class AdoptionFormUiState(
    val centerName: String? = null,
    val listingName: String? = null,
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val housing: Housing = Housing.House,
    val hasOtherPets: Boolean = false,
    val message: String = "",
    val errors: AdoptionFormErrors = AdoptionFormErrors(),
    val submitted: Boolean = false,
)

/**
 * ASSUMPTION: there is no adoption-request table in REQUIREMENTS, so the form only
 * validates locally and confirms; it makes no repository call. Phase 2 can add one.
 */
class AdoptionFormViewModel(
    organizationId: String,
    listingId: String?,
    authRepository: AuthRepository,
    organizationRepository: OrganizationRepository,
    adoptionRepository: AdoptionRepository,
) : ViewModel() {

    private val state = MutableStateFlow(initialState(authRepository))
    val uiState: StateFlow<AdoptionFormUiState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            val centerName = organizationRepository.getOrganization(organizationId)?.name
            val listingName = listingId?.let { adoptionRepository.getListing(it)?.name }
            state.update { it.copy(centerName = centerName, listingName = listingName) }
        }
    }

    fun onNameChange(value: String) = state.update { it.copy(name = value, errors = it.errors.copy(name = null)) }

    fun onPhoneChange(value: String) = state.update { it.copy(phone = value, errors = it.errors.copy(contact = null)) }

    fun onEmailChange(value: String) = state.update { it.copy(email = value, errors = it.errors.copy(contact = null)) }

    fun onHousingChange(value: Housing) = state.update { it.copy(housing = value) }

    fun onHasOtherPetsChange(value: Boolean) = state.update { it.copy(hasOtherPets = value) }

    fun onMessageChange(value: String) =
        state.update { it.copy(message = value, errors = it.errors.copy(message = null)) }

    fun submit() = state.update { current ->
        val errors = validateAdoptionForm(current.name, current.phone, current.email, current.message)
        current.copy(errors = errors, submitted = !errors.hasErrors)
    }

    private companion object {
        fun initialState(authRepository: AuthRepository): AdoptionFormUiState {
            val user = authRepository.currentUser.value?.takeUnless { it.isAnonymous }
                ?: return AdoptionFormUiState()
            return AdoptionFormUiState(
                name = "${user.firstName} ${user.lastName}".trim(),
                phone = user.phone.orEmpty(),
                email = user.email.orEmpty(),
            )
        }
    }
}
