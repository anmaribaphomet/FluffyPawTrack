package com.example.fluffypawtrack.data.supabase

import com.example.fluffypawtrack.domain.repository.PushTokenRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

/**
 * `device_tokens` through PostgREST; RLS (`device_tokens_own`) limits each account to its own rows.
 * The token registered last is remembered so sign-out deletes only this device's row.
 */
internal class SupabasePushTokenRepository(private val client: SupabaseClient) : PushTokenRepository {
    private var deviceToken: String? = null

    override suspend fun register(token: String): Result<Unit> = remote {
        val user = client.auth.currentUserOrNull()
        if (user == null || user.isAnonymous == true || token.isBlank()) throw RuleViolation(ACCOUNT_REQUIRED)
        client.from(TABLE).upsert(DeviceTokenDto(userId = user.id, fcmToken = token, platform = PLATFORM)) {
            onConflict = "user_id,fcm_token"
        }
        deviceToken = token
    }

    override suspend fun unregister(): Result<Unit> = remote {
        val token = deviceToken ?: return@remote
        val user = client.auth.currentUserOrNull() ?: return@remote
        client.from(TABLE).delete {
            filter {
                eq("user_id", user.id)
                eq("fcm_token", token)
            }
        }
        deviceToken = null
    }

    private companion object {
        const val TABLE = "device_tokens"
        const val PLATFORM = "android"
        const val ACCOUNT_REQUIRED = "Only accounts with a valid FCM token are registered"
    }
}
