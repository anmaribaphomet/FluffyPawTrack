package com.example.fluffypawtrack.data.supabase

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class AffiliationFailureTest {

    @Test
    fun aRefusedUploadOrCallShowsTheAccountRule() = runTest {
        listOf(401, 403).forEach { status ->
            val cause = RuntimeException("new row violates row-level security policy")
            val result = remote<Unit> { throw affiliationFailureOf(status, cause, hasAccount = false) }
            assertEquals("Necesitas una cuenta para solicitar la afiliación", result.exceptionOrNull()?.message)
        }
    }

    @Test
    fun aRefusalWithAnAccountMeansTheSessionExpired() = runTest {
        listOf(401, 403).forEach { status ->
            val cause = RuntimeException("JWT expired")
            val result = remote<Unit> { throw affiliationFailureOf(status, cause, hasAccount = true) }
            assertEquals("Tu sesión expiró. Inicia sesión de nuevo.", result.exceptionOrNull()?.message)
        }
    }

    @Test
    fun otherFailuresKeepTheirOwnMapping() {
        val cause = RuntimeException("timeout")
        assertSame(cause, affiliationFailureOf(500, cause, hasAccount = true))
        assertSame(cause, affiliationFailureOf(null, cause, hasAccount = true))
    }

    @Test
    fun aNetworkFailureShowsTheConnectionMessage() = runTest {
        val result = remote<Unit> { throw affiliationFailureOf(null, RuntimeException("Unable to resolve host"), hasAccount = true) }
        assertEquals(CONNECTION_FAILURE_MESSAGE, result.exceptionOrNull()?.message)
    }
}
