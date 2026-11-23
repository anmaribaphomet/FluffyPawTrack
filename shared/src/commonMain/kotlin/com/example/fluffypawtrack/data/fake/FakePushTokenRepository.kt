package com.example.fluffypawtrack.data.fake

import com.example.fluffypawtrack.data.PushTokenFailures
import com.example.fluffypawtrack.domain.model.UserProfile
import com.example.fluffypawtrack.domain.repository.PushTokenRepository

/**
 * In-memory `device_tokens`. [currentUser] stands in for `auth.uid()`: like RLS, a token is only
 * written or deleted for the signed-in account. [deviceToken] stands in for the FCM token of this
 * device; when it is unknown, the token registered last is used.
 */
class FakePushTokenRepository(
    private val currentUser: () -> UserProfile?,
    private val deviceToken: suspend () -> String? = { null },
    initialTokens: Map<String, Set<String>> = emptyMap(),
) : PushTokenRepository {
    private val tokensByUser = initialTokens.mapValues { it.value.toMutableSet() }.toMutableMap()
    private var registeredToken: String? = null

    /** Tokens saved for [userId], for tests. */
    fun tokensOf(userId: String): Set<String> = tokensByUser[userId].orEmpty().toSet()

    override suspend fun register(token: String): Result<Unit> {
        val user = currentUser() ?: return failure(PushTokenFailures.NO_SESSION)
        if (user.isAnonymous) return failure(PushTokenFailures.ANONYMOUS_SESSION)
        if (token.isBlank()) return Result.failure(IllegalArgumentException(PushTokenFailures.EMPTY_TOKEN))
        tokensByUser.getOrPut(user.id) { mutableSetOf() } += token
        registeredToken = token
        return Result.success(Unit)
    }

    override suspend fun unregister(): Result<Unit> {
        val user = currentUser() ?: return failure(PushTokenFailures.NO_SESSION)
        val token = deviceToken()?.takeIf { it.isNotBlank() } ?: registeredToken
            ?: return failure(PushTokenFailures.UNKNOWN_DEVICE_TOKEN)
        tokensByUser[user.id]?.remove(token)
        registeredToken = null
        return Result.success(Unit)
    }

    private fun failure(message: String): Result<Unit> = Result.failure(IllegalStateException(message))
}
