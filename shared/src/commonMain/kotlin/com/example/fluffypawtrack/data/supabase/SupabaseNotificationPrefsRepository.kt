package com.example.fluffypawtrack.data.supabase

import com.example.fluffypawtrack.domain.model.NotificationPrefs
import com.example.fluffypawtrack.domain.model.Priority
import com.example.fluffypawtrack.domain.repository.NotificationPrefsRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow

/** `notification_prefs` through PostgREST; RLS (`notification_prefs_own`) limits it to the caller's row. */
internal class SupabaseNotificationPrefsRepository(
    private val client: SupabaseClient,
    private val queries: QueryRegistry,
) : NotificationPrefsRepository {

    override fun observe(userId: String): Flow<NotificationPrefs> =
        queries.observe("notification-prefs:$userId", NotificationPrefs()) {
            client.from(TABLE).select { filter { eq("user_id", userId) } }
                .decodeList<NotificationPrefsDto>().firstOrNull()?.toDomain()
                ?: NotificationPrefs()
        }

    override suspend fun update(
        userId: String,
        radiusKm: Double,
        priorities: Set<Priority>,
        pushEnabled: Boolean,
    ): Result<NotificationPrefs> = remote {
        // Same check as the `radius_km > 0` constraint, with a readable message.
        if (radiusKm <= 0.0) throw RuleViolation(NotificationPrefs.INVALID_RADIUS)
        val row = NotificationPrefs(radiusKm, priorities, pushEnabled).toDto(userId)
        client.from(TABLE).upsert(row) {
            onConflict = "user_id"
            select()
        }.decodeList<NotificationPrefsDto>().first().toDomain()
    }.thenRefresh(queries)

    private companion object {
        const val TABLE = "notification_prefs"
    }
}
