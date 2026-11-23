package com.example.fluffypawtrack.data.supabase

import com.example.fluffypawtrack.domain.model.AppNotification
import com.example.fluffypawtrack.domain.model.NotificationType
import com.example.fluffypawtrack.domain.repository.NotificationRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.HasRecord
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
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

    /** Keys of the queries whose user asked to reload; a live session that gave up restarts on it. */
    private val reloads = MutableSharedFlow<String>(extraBufferCapacity = 1)

    /**
     * The rows of [userId], loaded when collection starts and kept live while it lasts: a
     * Realtime channel applies every INSERT, UPDATE and DELETE of those rows in place. Each
     * collection owns its channel and removes it when it stops (sign-out, screen closed), so
     * channels never outlive their screen. When live updates stop for good, the rows are
     * reported as failed, and [reload] starts them again.
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
                keepLive(
                    open = { NotificationChannel(client, userId) },
                    // Rows created or changed while the channel was not subscribed.
                    catchUp = { queries.refresh(key) },
                    apply = { change -> queries.update(key, change) },
                    onGiveUp = { queries.markFailed(key) },
                    retryRequests = reloads.filter { it == key }.map { },
                )
            }
            rows.collect { send(it) }
        }
    }

    override fun observeLoadFailed(userId: String): Flow<Boolean> = queries.failed(key(userId))

    override suspend fun reload(userId: String) {
        queries.refresh(key(userId))
        reloads.tryEmit(key(userId))
    }

    private fun key(userId: String) = "notifications:$userId"

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
}

private const val TABLE = "notifications"
private val SUBSCRIBE_TIMEOUT = 15.seconds

/** A change to the loaded notifications of one user. */
private typealias NotificationEdit = (List<AppNotification>) -> List<AppNotification>

/** One Realtime channel on the notifications of [userId]. */
private class NotificationChannel(private val client: SupabaseClient, userId: String) : LiveChannel<NotificationEdit> {
    // Unique per session: the client keys channels by topic.
    private val channel: RealtimeChannel =
        client.channel("notifications-$userId-${Random.nextLong().toULong().toString(16)}")

    override val changes: Flow<NotificationEdit> = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
        table = TABLE
        filter("user_id", FilterOperator.EQ, userId)
    }.mapNotNull { it.toEdit() }

    override val subscribed: Flow<Boolean> = channel.status.map { it == RealtimeChannel.Status.SUBSCRIBED }

    override suspend fun subscribe() {
        // A timeout is a failure to retry, not a cancellation (withTimeout would throw one).
        withTimeoutOrNull(SUBSCRIBE_TIMEOUT) { channel.subscribe(blockUntilSubscribed = true) }
            ?: throw IllegalStateException("Realtime subscription timed out")
    }

    override suspend fun close() = client.realtime.removeChannel(channel)
}

/** The edit of an INSERT, UPDATE or DELETE; rows that do not decode are ignored. */
private fun PostgresAction.toEdit(): NotificationEdit? = runCatching<NotificationEdit?> {
    when (this) {
        is PostgresAction.Insert, is PostgresAction.Update -> {
            val changed = (this as HasRecord).decodeRecord<NotificationDto>().toDomain()
            ({ list -> applyNotificationChange(list, changed) })
        }
        is PostgresAction.Delete -> oldRecord["id"]?.jsonPrimitive?.contentOrNull?.let { id ->
            { list -> removeNotification(list, id) }
        }
        else -> null
    }
}.getOrNull()

/**
 * [changed] replaces the row with its id, or joins the list when it is new; the list stays in
 * date order like the initial load. Applying the same change twice changes nothing.
 */
internal fun applyNotificationChange(current: List<AppNotification>, changed: AppNotification): List<AppNotification> =
    (current.filterNot { it.id == changed.id } + changed).sortedBy { it.receivedAt }

/** Removes the row with [id]; an id that is not loaded changes nothing. */
internal fun removeNotification(current: List<AppNotification>, id: String): List<AppNotification> =
    current.filterNot { it.id == id }
