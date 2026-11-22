package com.example.fluffypawtrack.ui.adoptions

/** Per-field validation messages; null means the field is valid. */
data class AdoptionFormErrors(
    val name: String? = null,
    val contact: String? = null,
    val message: String? = null,
) {
    val hasErrors: Boolean
        get() = listOf(name, contact, message).any { it != null }
}

internal const val MIN_ADOPTION_MESSAGE_LENGTH = 20

/** Plain validation rules for the adoption form: name, a phone or an email, and a message. */
internal fun validateAdoptionForm(name: String, phone: String, email: String, message: String) =
    AdoptionFormErrors(
        name = if (name.isBlank()) "Ingresa tu nombre" else null,
        contact = if (phone.isBlank() && email.isBlank()) "Ingresa un teléfono o un correo" else null,
        message = if (message.trim().length < MIN_ADOPTION_MESSAGE_LENGTH) {
            "El mensaje debe tener al menos $MIN_ADOPTION_MESSAGE_LENGTH caracteres"
        } else {
            null
        },
    )
