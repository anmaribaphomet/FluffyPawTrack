package com.example.fluffypawtrack.domain.push

import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.domain.repository.PushTokenRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Keeps `device_tokens` in step with the session: registers this device's token whenever an
 * account (not an anonymous session) signs in or is restored, and again when FCM rotates it.
 * Sign-out deletes the token through [PushTokenRepository.unregister] before the session ends.
 *
 * Registration is best effort: a failed upsert is retried on the next sign-in, app start or rotation.
 */
class PushTokenSync(
    private val auth: AuthRepository,
    private val tokens: PushTokenRepository,
    private val currentToken: suspend () -> String?,
    private val refreshedTokens: Flow<String>,
) {
    fun start(scope: CoroutineScope): Job = scope.launch {
        launch {
            auth.currentUser
                .map { user -> user?.takeUnless { it.isAnonymous }?.id }
                .distinctUntilChanged()
                .collect { accountId ->
                    if (accountId != null) currentToken()?.let { tokens.register(it) }
                }
        }
        launch {
            refreshedTokens.collect { token ->
                val user = auth.currentUser.value
                if (user != null && !user.isAnonymous) tokens.register(token)
            }
        }
    }
}
