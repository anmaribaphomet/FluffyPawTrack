package com.example.fluffypawtrack.data.supabase

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Waits between attempts to (re)subscribe to a Realtime channel; after the last one, live updates stop. */
internal val LIVE_RETRY_DELAYS: List<Duration> = listOf(1.seconds, 2.seconds, 5.seconds, 10.seconds, 30.seconds)

/**
 * Runs [session] (subscribe to one Realtime channel and apply its changes) and runs it again
 * after a failure, waiting the next of [delays] each time. A session calls `onConnected` once
 * it is subscribed, which gives the next failures the whole budget again. When the budget is
 * spent, or a session ends normally, this returns quietly: the screen keeps its last rows and
 * still reloads them after every mutation. Cancellation is never retried.
 */
internal suspend fun retryingLive(
    delays: List<Duration> = LIVE_RETRY_DELAYS,
    session: suspend (onConnected: () -> Unit) -> Unit,
) {
    var failures = 0
    while (true) {
        try {
            session { failures = 0 }
            return
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            if (failures >= delays.size) return
            delay(delays[failures++])
        }
    }
}
