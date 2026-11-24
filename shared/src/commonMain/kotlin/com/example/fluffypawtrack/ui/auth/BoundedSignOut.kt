package com.example.fluffypawtrack.ui.auth

import com.example.fluffypawtrack.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/** Longest wait for the remote sign-out before the app leaves to the login anyway. */
internal const val SIGN_OUT_TIMEOUT_MILLIS = 3_000L

/**
 * Signs out without ever throwing or waiting longer than [timeoutMillis]. Leaving to the login
 * must not depend on the network (REQUIREMENTS §4: the interface never stays blocked).
 */
internal suspend fun AuthRepository.signOutBounded(timeoutMillis: Long = SIGN_OUT_TIMEOUT_MILLIS) {
    try {
        withTimeoutOrNull(timeoutMillis) { signOut() }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Throwable) {
        // The repository already forgets the local session when the remote call fails.
    }
}
