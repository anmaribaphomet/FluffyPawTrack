package com.example.fluffypawtrack.data.supabase

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Backs the repositories' `Flow` observers until Realtime arrives (Phase 4): one
 * [MutableStateFlow] per query, loaded when a collector starts and reloaded by [refreshAll]
 * after every mutation made through any Supabase repository. One registry is shared by all
 * repositories, so for example marking an animal as adopted also refreshes the open lists of
 * adoption requests.
 */
internal class QueryRegistry {
    private val mutex = Mutex()
    // Copy-on-write, so screens may register queries while a refresh runs on another thread.
    private val queries = MutableStateFlow<Map<String, Query<*>>>(emptyMap())

    private class Query<T>(private val fallback: T, private val load: suspend () -> T) {
        val state = MutableStateFlow<T?>(null)

        /** A failed load keeps the last value; before any value it shows [fallback] (an empty list). */
        suspend fun refresh() {
            try {
                state.value = load()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                if (state.value == null) state.value = fallback
            }
        }
    }

    /**
     * Rows of [key], loaded with [load]. Every new collection reloads them, so a screen that
     * re-subscribes sees fresh data. [key] must identify the query, filters included.
     */
    fun <T> observe(key: String, fallback: T, load: suspend () -> T): Flow<T> {
        @Suppress("UNCHECKED_CAST")
        val query = queries.updateAndGet { all ->
            if (key in all) all else all + (key to Query(fallback, load))
        }.getValue(key) as Query<T>
        return query.state.onStart { query.refresh() }.filterNotNull()
    }

    /** Reloads every query that currently has a collector; the others reload on their next collection. */
    suspend fun refreshAll() = mutex.withLock {
        queries.value.values.filter { it.state.subscriptionCount.value > 0 }.forEach { it.refresh() }
    }
}

/** After a successful mutation, reloads the open queries so every screen sees the change. */
internal suspend fun <T> Result<T>.thenRefresh(queries: QueryRegistry): Result<T> =
    also { if (it.isSuccess) queries.refreshAll() }
