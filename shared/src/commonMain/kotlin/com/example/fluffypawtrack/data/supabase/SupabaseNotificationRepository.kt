package com.example.fluffypawtrack.data.supabase

import com.example.fluffypawtrack.domain.model.AppNotification
import com.example.fluffypawtrack.domain.model.NotificationType
import com.example.fluffypawtrack.domain.repository.NotificationRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

/**
 * The `notifications` table. The server creates every notification (RPCs and triggers), and the
 * API only lets the owner change `read`.
 */
internal class SupabaseNotificationRepository(
    private val client: SupabaseClient,
    private val queries: QueryRegistry,
) : NotificationRepository {

    /**
     * The rows of [userId], loaded when collection starts and kept live while it lasts: a
     * Realtime channel applies every INSERT and UPDATE of those rows in place. Each collection
     * owns its channel and removes it when it stops (sign-out, screen closed), so channels
     * never outlive their screen.
     */
    override fun observeNotifications(userId: String): Flow<List<AppNotification>> {
        val key = key(userId)
        val rows = queries.observe(key, emptyList()) {
            client.from(TABLE).select {
                filter { eq("user_id", userId) }
                order("created_at", Order.ASCENDING)
            }.decodeList<NotificationDto>().map { it.toDomain() }
        }
        return channelFlow {
            launch {
                retryingLive { onConnected ->
                    liveChanges(userId, onSubscribed = {
                        onConnected()
                        // Rows created between the first load and the subscription.
                        queries.refresh(key)
                    }) { changed ->
                        queries.update<List<AppNotification>>(key) { applyNotificationChange(it, changed) }
                    }
                }
            }
            rows.collect { send(it) }
        }
    }

    override fun observeLoadFailed(userId: String): Flow<Boolean> = queries.failed(key(userId))

    override suspend fun reload(userId: String) = queries.refresh(key(userId))

    private fun key(userId: String) = "notifications:$userId"

    /** Subscribes one channel to the rows of [userId] and applies changes until cancelled. */
    private suspend fun liveChanges(
        userId: String,
        onSubscribed: suspend () -> Unit,
        apply: suspend (AppNotification) -> Unit,
    ) {
        // Unique per collection: the client keys channels by topic.
        val channel = client.channel("notifications-$userId-${Random.nextLong().toULong().toString(16)}")
        try {
            val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = TABLE
                filter("user_id", FilterOperator.EQ, userId)
            }
            coroutineScope {
                // Listening starts before subscribing, so no change between both is lost.
                launch(start = CoroutineStart.UNDISPATCHED) {
                    changes.collect { action -> action.notificationOrNull()?.let { apply(it) } }
                }
                // A timeout is a failure to retry, not a cancellation (withTimeout would throw one).
                withTimeoutOrNull(SUBSCRIBE_TIMEOUT) { channel.subscribe(blockUntilSubscribed = true) }
                    ?: throw IllegalStateException("Realtime subscription timed out")
                onSubscribed()
                awaitCancellation()
            }
        } finally {
            withContext(NonCancellable) { runCatching { client.realtime.removeChannel(channel) } }
        }
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
        val SUBSCRIBE_TIMEOUT = 15.seconds
    }
}

/** The row of an INSERT or UPDATE; other events and rows that do not decode are ignored. */
private fun PostgresAction.notificationOrNull(): AppNotification? = runCatching {
    when (this) {
        is PostgresAction.Insert -> decodeRecord<NotificationDto>().toDomain()
        is PostgresAction.Update -> decodeRecord<NotificationDto>().toDomain()
        else -> null
    }
}.getOrNull()

/**
 * [changed] replaces the row with its id, or joins the list when it is new; the list stays in
 * date order like the initial load. Applying the same change twice changes nothing.
 */
internal fun applyNotificationChange(current: List<AppNotification>, changed: AppNotification): List<AppNotification> =
    (current.filterNot { it.id == changed.id } + changed).sortedBy { it.receivedAt }
