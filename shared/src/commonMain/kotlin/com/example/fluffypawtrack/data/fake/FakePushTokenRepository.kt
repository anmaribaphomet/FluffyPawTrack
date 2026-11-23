package com.example.fluffypawtrack.data.fake

import com.example.fluffypawtrack.domain.model.UserProfile
import com.example.fluffypawtrack.domain.repository.PushTokenRepository

/**
 * In-memory `device_tokens`. [currentUser] stands in for `auth.uid()`: like RLS, a token is only
 * written or deleted for the signed-in account, so unregistering after sign-out deletes nothing.
 */
class FakePushTokenRepository(
    private val currentUser: () -> UserProfile?,
    initialTokens: Map<String, Set<String>> = emptyMap(),
) : PushTokenRepository {
    private val tokensByUser = initialTokens.mapValues { it.value.toMutableSet() }.toMutableMap()
    private var deviceToken: String? = null

    /** Tokens saved for [userId], for tests. */
    fun tokensOf(userId: String): Set<String> = tokensByUser[userId].orEmpty().toSet()

    override suspend fun register(token: String): Result<Unit> {
        val user = currentUser()
        if (user == null || user.isAnonymous) return Result.failure(IllegalStateException(ACCOUNT_REQUIRED))
        if (token.isBlank()) return Result.failure(IllegalArgumentException("Empty FCM token"))
        tokensByUser.getOrPut(user.id) { mutableSetOf() } += token
        deviceToken = token
        return Result.success(Unit)
    }

    override suspend fun unregister(): Result<Unit> {
        val user = currentUser() ?: return Result.success(Unit)
        deviceToken?.let { tokensByUser[user.id]?.remove(it) }
        deviceToken = null
        return Result.success(Unit)
    }

    private companion object {
        const val ACCOUNT_REQUIRED = "Only accounts receive push notifications"
    }
}
