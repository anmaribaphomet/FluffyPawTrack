package com.example.fluffypawtrack.domain.push

import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.domain.repository.PushTokenRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Registers this device's token in `device_tokens` while an account (not an anonymous session)
 * is signed in: when it signs in or a session is restored, and again when FCM rotates the token.
 * It only registers; deleting the token at sign-out is the caller's job
 * ([PushTokenRepository.unregister], before the session ends).
 *
 * A failed registration is retried after each delay in [retryDelays]; the retries stop as soon as
 * the account signs out or a newer token arrives.
 */
class PushTokenSync(
    private val auth: AuthRepository,
    private val tokens: PushTokenRepository,
    private val currentToken: suspend () -> String?,
    private val refreshedTokens: Flow<String>,
    private val retryDelays: List<Duration> = DEFAULT_RETRY_DELAYS,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun start(scope: CoroutineScope): Job = scope.launch {
        auth.currentUser
            .map { user -> user?.takeUnless { it.isAnonymous }?.id }
            .distinctUntilChanged()
            // collectLatest cancels the pending registration and its retries when the account changes.
            .collectLatest { accountId ->
                if (accountId == null) return@collectLatest
                merge(flow { currentToken()?.let { emit(it) } }, refreshedTokens)
                    .collectLatest { token -> registerWithRetry(token) }
            }
    }

    private suspend fun registerWithRetry(token: String) {
        if (tokens.register(token).isSuccess) return
        for (wait in retryDelays) {
            delay(wait)
            if (tokens.register(token).isSuccess) return
        }
    }

    companion object {
        val DEFAULT_RETRY_DELAYS: List<Duration> = listOf(2.seconds, 10.seconds, 30.seconds)
    }
}
