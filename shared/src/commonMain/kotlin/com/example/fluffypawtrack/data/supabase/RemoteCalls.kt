package com.example.fluffypawtrack.data.supabase

import com.example.fluffypawtrack.domain.model.EmailConfirmationRequiredException
import com.example.fluffypawtrack.domain.model.InvalidAuthDataException
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import kotlinx.coroutines.CancellationException

/** Shown for failures that are not a business rule: lost connection, server or permission errors. */
internal const val CONNECTION_FAILURE_MESSAGE = "No pudimos conectar. Intenta de nuevo más tarde."

/** SQLSTATE of `raise exception` in the RPC functions: its message is a readable Spanish rule. */
private const val RAISED_EXCEPTION = "P0001"

/**
 * The failure a repository returns for a PostgREST error. Rule violations raised by the RPCs
 * keep their Spanish message, like the fake repositories; anything else gets
 * [CONNECTION_FAILURE_MESSAGE] so screens never show technical text.
 */
internal fun failureOf(code: String?, serverMessage: String?, cause: Throwable? = null): Throwable =
    if (code == RAISED_EXCEPTION && !serverMessage.isNullOrBlank()) {
        IllegalStateException(serverMessage, cause)
    } else {
        IllegalStateException(CONNECTION_FAILURE_MESSAGE, cause)
    }

/** Maps auth errors caused by the entered data to [InvalidAuthDataException]; others stay as they are. */
internal fun authFailureOf(code: AuthErrorCode?, cause: Throwable): Throwable = when (code) {
    AuthErrorCode.InvalidCredentials,
    AuthErrorCode.EmailExists,
    AuthErrorCode.UserAlreadyExists,
    AuthErrorCode.WeakPassword,
    AuthErrorCode.EmailAddressInvalid,
    AuthErrorCode.ValidationFailed,
    -> InvalidAuthDataException()
    AuthErrorCode.EmailNotConfirmed -> EmailConfirmationRequiredException()
    else -> cause
}

/** A rule checked by the repository before calling Supabase; its message is already readable Spanish. */
internal class RuleViolation(message: String) : IllegalStateException(message)

/** Runs a Supabase call and turns its exceptions into a readable failure. */
internal suspend fun <T> remote(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: PostgrestRestException) {
    Result.failure(failureOf(e.code, e.error, e))
} catch (e: RuleViolation) {
    Result.failure(e)
} catch (e: Throwable) {
    Result.failure(failureOf(code = null, serverMessage = null, cause = e))
}

/** Like [remote] for auth calls: keeps [InvalidAuthDataException]-style failures typed. */
internal suspend fun <T> remoteAuth(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: AuthRestException) {
    Result.failure(authFailureOf(e.errorCode, e))
} catch (e: Throwable) {
    Result.failure(e)
}

/** For reads whose interface has no failure channel (`getX(): X?`): any error reads as "not found". */
internal suspend fun <T> remoteOrNull(block: suspend () -> T?): T? = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (_: Throwable) {
    null
}
