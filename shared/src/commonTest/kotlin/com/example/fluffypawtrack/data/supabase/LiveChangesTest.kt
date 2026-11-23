package com.example.fluffypawtrack.data.supabase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class LiveChangesTest {

    /** A channel driven by the test; [events] records what the live session did, in order. */
    private class FakeChannel(
        private val id: Int,
        private val events: MutableList<String>,
        private val failSubscribe: Boolean = false,
        changesAfterSubscribe: List<String>? = null,
    ) : LiveChannel<String> {
        val status = MutableStateFlow(false)

        override val changes: Flow<String> = if (changesAfterSubscribe == null) {
            flow { awaitCancellation() }
        } else {
            // Emits once subscribed, then completes like a channel whose change flow ended.
            flow {
                status.first { it }
                changesAfterSubscribe.forEach { emit(it) }
            }
        }

        override val subscribed: Flow<Boolean> = status

        override suspend fun subscribe() {
            events += "subscribe $id"
            if (failSubscribe) error("cannot subscribe $id")
            status.value = true
        }

        override suspend fun close() {
            events += "close $id"
        }
    }

    private fun TestScope.runKeepLive(
        events: MutableList<String>,
        retryRequests: Flow<Unit> = emptyFlow(),
        open: (Int) -> FakeChannel,
    ) = backgroundScope.launch {
        var opened = 0
        keepLive(
            open = { open(++opened) },
            catchUp = { events += "catch-up" },
            apply = { events += "apply $it" },
            onGiveUp = { events += "gave up: ${it.message}" },
            retryRequests = retryRequests,
            delays = listOf(1.seconds),
        )
    }

    @Test
    fun endedChangeFlowIsRetriedAndEachResubscribeCatchesUp() = runTest {
        val events = mutableListOf<String>()

        runKeepLive(events) { id -> FakeChannel(id, events, changesAfterSubscribe = listOf("x$id")) }
        runCurrent()
        advanceTimeBy(1_500)

        assertEquals(
            listOf(
                "subscribe 1", "catch-up", "apply x1", "close 1",
                "subscribe 2", "catch-up", "apply x2", "close 2",
            ),
            events,
        )
    }

    @Test
    fun channelLeavingTheSubscribedStateIsRetried() = runTest {
        val events = mutableListOf<String>()
        val channels = mutableListOf<FakeChannel>()

        runKeepLive(events) { id -> FakeChannel(id, events).also { channels += it } }
        runCurrent()
        channels[0].status.value = false
        advanceTimeBy(1_500)

        assertEquals(listOf("subscribe 1", "catch-up", "close 1", "subscribe 2", "catch-up"), events)
    }

    @Test
    fun exhaustedRetriesReportTheFailureAndARetryRequestRestarts() = runTest {
        val events = mutableListOf<String>()
        val retryRequests = MutableSharedFlow<Unit>()
        var offline = true

        runKeepLive(events, retryRequests) { id -> FakeChannel(id, events, failSubscribe = offline) }
        // Background work is not part of advanceUntilIdle, so time moves on explicitly.
        advanceTimeBy(5_000)

        assertEquals(
            listOf("subscribe 1", "close 1", "subscribe 2", "close 2", "gave up: cannot subscribe 2"),
            events,
        )

        events.clear()
        offline = false
        retryRequests.emit(Unit)
        runCurrent()

        assertEquals(listOf("subscribe 3", "catch-up"), events)
    }
}
