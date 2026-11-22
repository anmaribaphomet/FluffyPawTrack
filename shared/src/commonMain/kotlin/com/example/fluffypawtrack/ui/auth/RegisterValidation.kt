package com.example.fluffypawtrack.ui.auth

/** Per-field validation messages; null means the field is valid. */
data class RegisterErrors(
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val password: String? = null,
    val confirmPassword: String? = null,
) {
    val hasErrors: Boolean
        get() = listOf(firstName, lastName, email, password, confirmPassword).any { it != null }
}

internal const val MIN_PASSWORD_LENGTH = 8

private val EmailShape = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

/** Plain validation rules for the registration form. The phone is optional. */
internal fun validateRegistration(
    firstName: String,
    lastName: String,
    email: String,
    password: String,
    confirmPassword: String,
): RegisterErrors = RegisterErrors(
    firstName = if (firstName.isBlank()) "Ingresa tu nombre" else null,
    lastName = if (lastName.isBlank()) "Ingresa tus apellidos" else null,
    email = when {
        email.isBlank() -> "Ingresa tu correo"
        !EmailShape.matches(email.trim()) -> "Ingresa un correo válido"
        else -> null
    },
    password = when {
        password.isEmpty() -> "Ingresa una contraseña"
        password.length < MIN_PASSWORD_LENGTH -> "La contraseña debe tener al menos $MIN_PASSWORD_LENGTH caracteres"
        else -> null
    },
    confirmPassword = when {
        confirmPassword.isEmpty() -> "Confirma tu contraseña"
        confirmPassword != password -> "Las contraseñas no coinciden"
        else -> null
    },
)
