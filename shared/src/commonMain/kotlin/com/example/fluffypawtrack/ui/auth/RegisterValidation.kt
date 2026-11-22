package com.example.fluffypawtrack.ui.auth

internal const val MIN_PASSWORD_LENGTH = 8

private val EmailShape = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

/**
 * Plain validation rules for the registration form: names are required, the email has a
 * valid shape, the password has at least [MIN_PASSWORD_LENGTH] characters and the
 * confirmation matches. The phone is optional.
 */
internal fun isRegistrationValid(
    firstName: String,
    lastName: String,
    email: String,
    password: String,
    confirmPassword: String,
): Boolean =
    firstName.isNotBlank() &&
        lastName.isNotBlank() &&
        EmailShape.matches(email.trim()) &&
        password.length >= MIN_PASSWORD_LENGTH &&
        confirmPassword == password
