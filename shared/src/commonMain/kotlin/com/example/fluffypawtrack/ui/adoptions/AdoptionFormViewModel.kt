package com.example.fluffypawtrack.ui.adoptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.model.AdoptionListing
import com.example.fluffypawtrack.domain.model.HousingTenure
import com.example.fluffypawtrack.domain.model.HousingType
import com.example.fluffypawtrack.domain.model.ListingStatus
import com.example.fluffypawtrack.domain.model.NewAdoptionRequest
import com.example.fluffypawtrack.domain.model.Role
import com.example.fluffypawtrack.domain.model.UserProfile
import com.example.fluffypawtrack.domain.repository.AdoptionRepository
import com.example.fluffypawtrack.domain.repository.AdoptionRequestRepository
import com.example.fluffypawtrack.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal const val ADOPTION_ACCOUNT_REQUIRED =
    "Inicia sesión con tu cuenta para enviar una solicitud de adopción. Las sesiones anónimas solo pueden reportar animales."
internal const val ADOPTION_ONLY_USERS =
    "Solo las cuentas de usuario pueden enviar solicitudes de adopción."
internal const val ADOPTION_LISTING_UNAVAILABLE = "Este animal ya no está disponible para adopción."

data class AdoptionFormUiState(
    /** Locked "Interesado en:" value: `Nombre · Tipo · Raza`. */
    val interestedIn: String = "",
    /** Why the form cannot be sent (no account, not a `user`, animal unavailable); null shows the form. */
    val blockedMessage: String? = null,
    val fullName: String = "",
    val phone: String = "",
    val email: String = "",
    val housingType: HousingType = HousingType.House,
    val housingTenure: HousingTenure = HousingTenure.Owned,
    val hasOtherPets: Boolean = false,
    val otherPets: String = "",
    val reason: String = "",
    val errors: AdoptionFormErrors = AdoptionFormErrors(),
    /** Readable repository failure, for example a second active request. */
    val submitError: String? = null,
    val isSubmitting: Boolean = false,
    val submitted: Boolean = false,
)

/** RF-06 adoption form for one listing; the request is saved through [AdoptionRequestRepository]. */
class AdoptionFormViewModel(
    private val listingId: String,
    private val authRepository: AuthRepository,
    adoptionRepository: AdoptionRepository,
    private val adoptionRequestRepository: AdoptionRequestRepository,
) : ViewModel() {

    private val state = MutableStateFlow(initialState(authRepository.currentUser.value))
    val uiState: StateFlow<AdoptionFormUiState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            val listing = adoptionRepository.getListing(listingId)
            state.update {
                it.copy(
                    interestedIn = listing?.interestedInText().orEmpty(),
                    blockedMessage = it.blockedMessage
                        ?: ADOPTION_LISTING_UNAVAILABLE.takeIf { listing?.status != ListingStatus.Available },
                )
            }
        }
    }

    fun onFullNameChange(value: String) =
        state.update { it.copy(fullName = value, errors = it.errors.copy(fullName = null)) }

    fun onPhoneChange(value: String) = state.update { it.copy(phone = value, errors = it.errors.copy(phone = null)) }

    fun onEmailChange(value: String) = state.update { it.copy(email = value, errors = it.errors.copy(email = null)) }

    fun onHousingTypeChange(value: HousingType) = state.update { it.copy(housingType = value) }

    fun onHousingTenureChange(value: HousingTenure) = state.update { it.copy(housingTenure = value) }

    fun onHasOtherPetsChange(value: Boolean) =
        state.update { it.copy(hasOtherPets = value, errors = it.errors.copy(otherPets = null)) }

    fun onOtherPetsChange(value: String) =
        state.update { it.copy(otherPets = value, errors = it.errors.copy(otherPets = null)) }

    fun onReasonChange(value: String) = state.update { it.copy(reason = value, errors = it.errors.copy(reason = null)) }

    fun dismissSubmitError() = state.update { it.copy(submitError = null) }

    fun submit() {
        val current = state.value
        if (current.blockedMessage != null || current.isSubmitting || current.submitted) return
        val errors = validateAdoptionForm(current)
        if (errors.hasErrors) {
            state.update { it.copy(errors = errors) }
            return
        }
        state.update { it.copy(errors = errors, isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            val result = adoptionRequestRepository.submit(
                requesterId = authRepository.currentUser.value?.id,
                request = NewAdoptionRequest(
                    listingId = listingId,
                    fullName = current.fullName,
                    phone = current.phone,
                    email = current.email,
                    housingType = current.housingType,
                    housingTenure = current.housingTenure,
                    hasOtherPets = current.hasOtherPets,
                    otherPets = current.otherPets.takeIf { current.hasOtherPets },
                    reason = current.reason,
                ),
            )
            state.update {
                it.copy(
                    isSubmitting = false,
                    submitted = result.isSuccess,
                    submitError = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    private companion object {
        fun initialState(user: UserProfile?): AdoptionFormUiState = when {
            user == null || user.isAnonymous -> AdoptionFormUiState(blockedMessage = ADOPTION_ACCOUNT_REQUIRED)
            user.role != Role.User -> AdoptionFormUiState(blockedMessage = ADOPTION_ONLY_USERS)
            else -> AdoptionFormUiState(
                fullName = "${user.firstName} ${user.lastName}".trim(),
                phone = user.phone.orEmpty(),
                email = user.email.orEmpty(),
            )
        }
    }
}

/** "Canela · Perro · Mestiza". */
internal fun AdoptionListing.interestedInText(): String = listOf(name, species, breed).joinToString(" · ")
