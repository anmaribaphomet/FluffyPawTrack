package com.example.fluffypawtrack.domain.model

/**
 * RF-08 "Información personal editable": first name required; the phone is optional, but
 * when given it has exactly 10 digits. Spaces and dashes are allowed and dropped.
 */
object ProfileRules {
    const val FIRST_NAME_REQUIRED = "Ingresa tu nombre"
    const val PHONE_INVALID = "Ingresa un teléfono de 10 dígitos"
    const val SIGN_IN_REQUIRED = "Inicia sesión para editar tus datos"

    fun firstNameError(firstName: String): String? = if (firstName.isBlank()) FIRST_NAME_REQUIRED else null

    fun phoneError(phone: String?): String? {
        if (phone.isNullOrBlank()) return null
        val compact = compactPhone(phone)
        return if (compact.length == 10 && compact.all { it.isDigit() }) null else PHONE_INVALID
    }

    /** The first rule [firstName] or [phone] breaks, or null when both are valid. */
    fun problemOf(firstName: String, phone: String?): String? = firstNameError(firstName) ?: phoneError(phone)

    /** The phone as stored: its 10 digits, or null when left blank. Call after [phoneError]. */
    fun normalizedPhone(phone: String?): String? = phone?.takeIf { it.isNotBlank() }?.let(::compactPhone)

    private fun compactPhone(phone: String) = phone.filterNot { it == ' ' || it == '-' }
}
