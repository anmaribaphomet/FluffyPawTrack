package com.example.fluffypawtrack.data.supabase

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Waits between attempts to (re)subscribe to a Realtime channel; after the last one, live updates stop. */
internal val LIVE_RETRY_DELAYS: List<Duration> = listOf(1.seconds, 2.seconds, 5.seconds, 10.seconds, 30.seconds)

/** One Realtime channel, opened for a single live session and closed when it ends. */
internal interface LiveChannel<T> {
    /** Changes of the watched rows. It may end only when the channel is gone. */
    val changes: Flow<T>

    /** Whether the channel is currently subscribed. */
    val subscribed: Flow<Boolean>

    /** Subscribes the channel; throws when it cannot, or does not in time. */
    suspend fun subscribe()

    suspend fun close()
}

/**
 * Keeps rows live until cancelled: opens a channel, reloads the rows with [catchUp] after every
 * successful (re)subscribe (changes made while unsubscribed are not replayed), and applies each
 * change. A failed subscribe, an ended change flow or a channel that leaves the subscribed state
 * opens a new channel after the next of [delays]. When they are spent, [onGiveUp] gets the last
 * failure and the session waits for [retryRequests] (the screen's "Reintentar") to start over.
 */
internal suspend fun <T> keepLive(
    open: () -> LiveChannel<T>,
    catchUp: suspend () -> Unit,
    apply: suspend (T) -> Unit,
    onGiveUp: suspend (Throwable) -> Unit,
    retryRequests: Flow<Unit>,
    delays: List<Duration> = LIVE_RETRY_DELAYS,
) {
    while (true) {
        val failure = retryingLive(delays) { onConnected ->
            liveSession(open(), apply) {
                onConnected()
                catchUp()
            }
        }
        onGiveUp(failure)
        retryRequests.first()
    }
}

/** Runs one channel until it fails; it never ends normally, cancellation aside. */
private suspend fun <T> liveSession(channel: LiveChannel<T>, apply: suspend (T) -> Unit, onSubscribed: suspend () -> Unit) {
    try {
        coroutineScope {
            // Listening starts before subscribing, so no change between both is lost.
            launch(start = CoroutineStart.UNDISPATCHED) {
                channel.changes.collect { apply(it) }
                throw LiveSessionEnded("Realtime changes ended")
            }
            channel.subscribe()
            onSubscribed()
            channel.subscribed.first { !it }
            throw LiveSessionEnded("Realtime channel is no longer subscribed")
        }
    } finally {
        withContext(NonCancellable) { runCatching { channel.close() } }
    }
}

/** A live session that stopped without an error of its own; it is retried like any failure. */
internal class LiveSessionEnded(message: String) : IllegalStateException(message)

/**
 * Runs [session] (subscribe to one Realtime channel and apply its changes) and runs it again
 * after a failure, waiting the next of [delays] each time. A session calls `onConnected` once
 * it is subscribed, which gives the next failures the whole budget again. A session that ends
 * normally counts as a failure too. When the budget is spent, this returns the last failure.
 * Cancellation is never retried.
 */
internal suspend fun retryingLive(
    delays: List<Duration> = LIVE_RETRY_DELAYS,
    session: suspend (onConnected: () -> Unit) -> Unit,
): Throwable {
    var failures = 0
    while (true) {
        val failure = try {
            session { failures = 0 }
            LiveSessionEnded("Live session ended")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            e
        }
        if (failures >= delays.size) return failure
        delay(delays[failures++])
    }
}
