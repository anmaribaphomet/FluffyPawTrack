package com.example.fluffypawtrack.data.supabase

import com.example.fluffypawtrack.data.PushTokenFailures
import com.example.fluffypawtrack.domain.repository.PushTokenRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

/**
 * `device_tokens` through PostgREST; RLS (`device_tokens_own`) limits each account to its own rows.
 * Sign-out deletes the row of this device's current FCM token ([deviceToken]); the token registered
 * last is the fallback when FCM cannot report it (offline, no Firebase config).
 */
internal class SupabasePushTokenRepository(
    private val client: SupabaseClient,
    private val deviceToken: suspend () -> String?,
) : PushTokenRepository {
    private var registeredToken: String? = null

    override suspend fun register(token: String): Result<Unit> = remote {
        val user = client.auth.currentUserOrNull() ?: throw RuleViolation(PushTokenFailures.NO_SESSION)
        if (user.isAnonymous == true) throw RuleViolation(PushTokenFailures.ANONYMOUS_SESSION)
        if (token.isBlank()) throw RuleViolation(PushTokenFailures.EMPTY_TOKEN)
        client.from(TABLE).upsert(DeviceTokenDto(userId = user.id, fcmToken = token, platform = PLATFORM)) {
            onConflict = "user_id,fcm_token"
        }
        registeredToken = token
    }

    override suspend fun unregister(): Result<Unit> = remote {
        val user = client.auth.currentUserOrNull() ?: throw RuleViolation(PushTokenFailures.NO_SESSION)
        val token = deviceToken()?.takeIf { it.isNotBlank() } ?: registeredToken
            ?: throw RuleViolation(PushTokenFailures.UNKNOWN_DEVICE_TOKEN)
        client.from(TABLE).delete {
            filter {
                eq("user_id", user.id)
                eq("fcm_token", token)
            }
        }
        registeredToken = null
    }

    private companion object {
        const val TABLE = "device_tokens"
        const val PLATFORM = "android"
    }
}
