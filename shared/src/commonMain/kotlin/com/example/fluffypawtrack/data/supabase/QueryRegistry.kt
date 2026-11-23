package com.example.fluffypawtrack.data.supabase

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Backs the repositories' `Flow` observers: one [MutableStateFlow] per query, loaded when a
 * collector starts and reloaded by [refreshAll] after every mutation made through any Supabase
 * repository. Realtime changes are applied in place with [update]. One registry is shared by all
 * repositories, so for example marking an animal as adopted also refreshes the open lists of
 * adoption requests.
 */
internal class QueryRegistry {
    private val mutex = Mutex()
    // Copy-on-write, so screens may register queries while a refresh runs on another thread.
    private val queries = MutableStateFlow<Map<String, Query<*>>>(emptyMap())

    private class Query<T>(private val fallback: T, private val load: suspend () -> T) {
        val state = MutableStateFlow<T?>(null)

        /** Whether the last load failed; the next successful load clears it. */
        val failed = MutableStateFlow(false)

        /**
         * Orders loads and live changes of this query: a change waits for a running load and is
         * applied on top of its result, so a load that started before the change cannot drop it.
         */
        private val lock = Mutex()

        /** A failed load keeps the last value; before any value it shows [fallback] (an empty list). */
        suspend fun refresh(): Unit = lock.withLock {
            try {
                state.value = load()
                failed.value = false
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                failed.value = true
                if (state.value == null) state.value = fallback
            }
        }

        /** Before the first load there is nothing to change: that load already reads the change. */
        suspend fun update(transform: (T) -> T): Unit = lock.withLock {
            state.value?.let { state.value = transform(it) }
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

    /** Whether the last load of [key] failed; false until the query is observed. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun failed(key: String): Flow<Boolean> =
        queries.map { it[key] }.distinctUntilChanged().flatMapLatest { it?.failed ?: flowOf(false) }

    /** Reloads the query [key], if it was ever observed. */
    suspend fun refresh(key: String) {
        queries.value[key]?.refresh()
    }

    /** Applies a live change to the loaded rows of [key]; the caller knows the query's type. */
    suspend fun <T> update(key: String, transform: (T) -> T) {
        @Suppress("UNCHECKED_CAST")
        (queries.value[key] as Query<T>?)?.update(transform)
    }

    /** Reloads every query that currently has a collector; the others reload on their next collection. */
    suspend fun refreshAll() = mutex.withLock {
        queries.value.values.filter { it.state.subscriptionCount.value > 0 }.forEach { it.refresh() }
    }
}

/** After a successful mutation, reloads the open queries so every screen sees the change. */
internal suspend fun <T> Result<T>.thenRefresh(queries: QueryRegistry): Result<T> =
    also { if (it.isSuccess) queries.refreshAll() }
