package com.example.fluffypawtrack.data.supabase

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class AffiliationFailureTest {

    @Test
    fun aRefusedUploadOrCallShowsTheAccountRule() = runTest {
        listOf(401, 403).forEach { status ->
            val result = remote<Unit> { throw affiliationFailureOf(status, RuntimeException("new row violates row-level security policy")) }
            assertEquals("Necesitas una cuenta para solicitar la afiliación", result.exceptionOrNull()?.message)
        }
    }

    @Test
    fun otherFailuresKeepTheirOwnMapping() {
        val cause = RuntimeException("timeout")
        assertSame(cause, affiliationFailureOf(500, cause))
        assertSame(cause, affiliationFailureOf(null, cause))
    }

    @Test
    fun aNetworkFailureShowsTheConnectionMessage() = runTest {
        val result = remote<Unit> { throw affiliationFailureOf(null, RuntimeException("Unable to resolve host")) }
        assertEquals(CONNECTION_FAILURE_MESSAGE, result.exceptionOrNull()?.message)
    }
}
