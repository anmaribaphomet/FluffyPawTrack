package com.example.fluffypawtrack.data.supabase

import com.example.fluffypawtrack.domain.model.AppNotification
import com.example.fluffypawtrack.domain.model.NotificationType
import com.example.fluffypawtrack.domain.repository.NotificationRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow

/**
 * The `notifications` table. The server creates every notification (RPCs and triggers), and the
 * API only lets the owner change `read`.
 */
internal class SupabaseNotificationRepository(
    private val client: SupabaseClient,
    private val queries: QueryRegistry,
) : NotificationRepository {

    override fun observeNotifications(userId: String): Flow<List<AppNotification>> =
        queries.observe("notifications:$userId", emptyList()) {
            client.from(TABLE).select {
                filter { eq("user_id", userId) }
                order("created_at", Order.ASCENDING)
            }.decodeList<NotificationDto>().map { it.toDomain() }
        }

    override suspend fun markAsRead(notificationId: String) {
        remote {
            client.from(TABLE).update({ set("read", true) }) { filter { eq("id", notificationId) } }
        }.thenRefresh(queries)
    }

    override suspend fun markAllAsRead(userId: String) {
        remote {
            client.from(TABLE).update({ set("read", true) }) {
                filter {
                    eq("user_id", userId)
                    eq("read", false)
                }
            }
        }.thenRefresh(queries)
    }

    /**
     * Not available with Supabase: clients have no insert grant on `notifications`; the RPCs and
     * triggers that change a request or report insert them. Only the fakes call this.
     */
    override suspend fun notify(
        userId: String,
        title: String,
        message: String,
        type: NotificationType,
    ): AppNotification = throw UnsupportedOperationException("Notifications are created by the server")

    private companion object {
        const val TABLE = "notifications"
    }
}
