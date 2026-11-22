package com.example.fluffypawtrack.ui.adoptions

internal const val FULL_NAME_ERROR = "Ingresa tu nombre completo"
internal const val PHONE_ERROR = "Ingresa tu teléfono"
internal const val EMAIL_ERROR = "Ingresa tu correo"
internal const val OTHER_PETS_ERROR = "Indica cuáles otras mascotas tienes"
internal const val ADOPTION_REASON_ERROR = "Cuéntanos por qué quieres adoptarlo"

/** Per-field validation messages; null means the field is valid. */
data class AdoptionFormErrors(
    val fullName: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val otherPets: String? = null,
    val reason: String? = null,
) {
    val hasErrors: Boolean
        get() = listOf(fullName, phone, email, otherPets, reason).any { it != null }
}

/** RF-06 form rules: contact data, which other pets when the answer is "Sí", and the reason. */
internal fun validateAdoptionForm(state: AdoptionFormUiState) = AdoptionFormErrors(
    fullName = FULL_NAME_ERROR.takeIf { state.fullName.isBlank() },
    phone = PHONE_ERROR.takeIf { state.phone.isBlank() },
    email = EMAIL_ERROR.takeIf { state.email.isBlank() },
    otherPets = OTHER_PETS_ERROR.takeIf { state.hasOtherPets && state.otherPets.isBlank() },
    reason = ADOPTION_REASON_ERROR.takeIf { state.reason.isBlank() },
)
