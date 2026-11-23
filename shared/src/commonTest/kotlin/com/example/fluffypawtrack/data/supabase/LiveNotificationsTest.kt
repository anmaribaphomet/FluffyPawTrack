package com.example.fluffypawtrack.data.supabase

import com.example.fluffypawtrack.domain.model.AppNotification
import com.example.fluffypawtrack.domain.model.NotificationType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class LiveNotificationsTest {

    private fun notification(id: String, minute: Int, read: Boolean = false) = AppNotification(
        id = id,
        title = "Nuevo reporte",
        message = "Hay un reporte cerca de ti",
        receivedAt = Instant.fromEpochSeconds(minute * 60L),
        type = NotificationType.Report,
        isRead = read,
    )

    @Test
    fun insertedNotificationIsAddedInDateOrder() {
        val current = listOf(notification("a", 1), notification("c", 3))

        val result = applyNotificationChange(current, notification("b", 2))

        assertEquals(listOf("a", "b", "c"), result.map { it.id })
    }

    @Test
    fun updatedNotificationReplacesTheOldRow() {
        val current = listOf(notification("a", 1), notification("b", 2))

        val result = applyNotificationChange(current, notification("a", 1, read = true))

        assertEquals(listOf("a", "b"), result.map { it.id })
        assertEquals(listOf(true, false), result.map { it.isRead })
    }

    @Test
    fun repeatedChangeIsAppliedOnce() {
        val once = applyNotificationChange(emptyList(), notification("a", 1))

        assertEquals(once, applyNotificationChange(once, notification("a", 1)))
    }

    @Test
    fun deletedNotificationIsRemovedAndUnknownIdsChangeNothing() {
        val current = listOf(notification("a", 1), notification("b", 2))

        assertEquals(listOf("b"), removeNotification(current, "a").map { it.id })
        assertEquals(current, removeNotification(current, "missing"))
    }

    @Test
    fun liveUpdateChangesTheObservedQueryInPlace() = runTest {
        var loads = 0
        val queries = QueryRegistry()
        val flow = queries.observe("notifications:u1", emptyList<AppNotification>()) {
            loads++
            listOf(notification("a", 1))
        }
        val seen = mutableListOf<List<String>>()
        val job = launch { flow.take(2).toList().mapTo(seen) { list -> list.map { it.id } } }
        advanceUntilIdle()

        queries.update<List<AppNotification>>("notifications:u1") { applyNotificationChange(it, notification("b", 2)) }
        job.join()

        assertEquals(listOf(listOf("a"), listOf("a", "b")), seen)
        assertEquals(1, loads)
    }

    @Test
    fun updateOfAnUnknownQueryDoesNothing() = runTest {
        val queries = QueryRegistry()

        queries.update<List<AppNotification>>("missing") { error("must not run") }
    }

    @Test
    fun failedSessionsAreRetriedWithBackoffAndThenGivenUp() = runTest {
        val attemptsAt = mutableListOf<Long>()

        retryingLive(delays = listOf(1.seconds, 2.seconds, 4.seconds)) {
            attemptsAt += currentTime
            error("cannot subscribe")
        }

        assertEquals(listOf(0L, 1_000L, 3_000L, 7_000L), attemptsAt)
    }

    @Test
    fun connectedSessionResetsTheRetryBudget() = runTest {
        var attempts = 0

        retryingLive(delays = listOf(1.seconds)) { onConnected ->
            attempts++
            // The first three sessions connect and then drop, so each one earns a retry; the
            // fourth never connects and spends the single retry left.
            if (attempts <= 3) onConnected()
            error("dropped")
        }

        assertEquals(4, attempts)
    }

    @Test
    fun cancellingStopsTheSessionWithoutRetrying() = runTest {
        var attempts = 0
        val started = CompletableDeferred<Unit>()
        val job = launch {
            retryingLive(delays = listOf(1.seconds)) {
                attempts++
                started.complete(Unit)
                awaitCancellation()
            }
        }
        started.await()

        job.cancel()
        advanceUntilIdle()

        assertEquals(1, attempts)
    }

    @Test
    fun failedLoadIsReportedUntilAReloadSucceeds() = runTest {
        var offline = true
        val queries = QueryRegistry()
        val flow = queries.observe("notifications:u1", emptyList<String>()) {
            if (offline) error("offline") else listOf("a")
        }
        val failed = queries.failed("notifications:u1")
        assertEquals(false, failed.first())

        assertEquals(emptyList(), flow.first())
        assertEquals(true, failed.first())

        offline = false
        queries.refresh("notifications:u1")

        assertEquals(false, failed.first())
        assertEquals(listOf("a"), flow.first())
    }

    @Test
    fun refreshOfOneQueryReloadsIt() = runTest {
        var value = 1
        val queries = QueryRegistry()
        val flow = queries.observe("count", 0) { value }
        val seen = mutableListOf<Int>()
        val job = launch { flow.take(2).toList(seen) }
        advanceUntilIdle()

        value = 2
        queries.refresh("count")
        job.join()

        assertEquals(listOf(1, 2), seen)
    }
}
