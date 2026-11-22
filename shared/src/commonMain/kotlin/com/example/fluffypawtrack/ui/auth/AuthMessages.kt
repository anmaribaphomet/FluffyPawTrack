package com.example.fluffypawtrack.ui.auth

import com.example.fluffypawtrack.domain.model.EmailConfirmationRequiredException
import com.example.fluffypawtrack.domain.model.InvalidAuthDataException

/**
 * The only message login and registration show for invalid data. It never reveals
 * whether the email exists or which field was wrong.
 */
const val INVALID_DATA_MESSAGE = "Datos inválidos, intenta de nuevo."

/** Shown for any failure that is not invalid data, such as a lost connection or a server error. */
const val CONNECTION_ERROR_MESSAGE = "No pudimos conectar. Intenta de nuevo más tarde."

/** Shown when the account exists but the email must be confirmed before signing in. */
const val EMAIL_CONFIRMATION_MESSAGE = "Revisa tu correo para confirmar tu cuenta."

/** Maps an auth failure to its user-facing message without exposing the error's own text. */
internal fun Throwable.authErrorMessage(): String = when (this) {
    is InvalidAuthDataException -> INVALID_DATA_MESSAGE
    is EmailConfirmationRequiredException -> EMAIL_CONFIRMATION_MESSAGE
    else -> CONNECTION_ERROR_MESSAGE
}
