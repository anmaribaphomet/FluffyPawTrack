package com.example.fluffypawtrack.data.supabase

import com.example.fluffypawtrack.domain.model.EmailConfirmationRequiredException
import com.example.fluffypawtrack.domain.model.InvalidAuthDataException
import io.github.jan.supabase.auth.exception.AuthErrorCode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SupabaseSupportTest {

    @Test
    fun queryLoadsOnFirstCollect() = runTest {
        var loads = 0
        val queries = QueryRegistry()
        val flow = queries.observe("reports", emptyList<Int>()) { loads++; listOf(1, 2) }

        assertEquals(0, loads)
        assertEquals(listOf(1, 2), flow.first())
        assertEquals(1, loads)
    }

    @Test
    fun refreshAfterMutationReloadsActiveQueries() = runTest {
        var value = 1
        val queries = QueryRegistry()
        val flow = queries.observe("count", 0) { value }

        val seen = mutableListOf<Int>()
        val job = launch { flow.take(2).toList(seen) }
        advanceUntilIdle()
        value = 2
        queries.refreshAll()
        advanceUntilIdle()
        job.join()

        assertEquals(listOf(1, 2), seen)
    }

    @Test
    fun failedFirstLoadEmitsTheFallback() = runTest {
        val queries = QueryRegistry()
        val flow = queries.observe("broken", emptyList<String>()) { error("offline") }

        assertEquals(emptyList(), flow.first())
    }

    @Test
    fun failedRefreshKeepsTheLastValue() = runTest {
        var fail = false
        val queries = QueryRegistry()
        val flow = queries.observe("k", "none") { if (fail) error("offline") else "loaded" }
        assertEquals("loaded", flow.first())

        fail = true

        assertEquals("loaded", flow.first())
    }

    @Test
    fun serverRuleMessagesAreShownAsIs() {
        val failure = failureOf(code = "P0001", serverMessage = "Ya tienes una solicitud activa en este reporte")

        assertIs<IllegalStateException>(failure)
        assertEquals("Ya tienes una solicitud activa en este reporte", failure.message)
    }

    @Test
    fun otherServerErrorsGetTheConnectionMessage() {
        val cause = RuntimeException("permission denied for table notifications")
        val failure = failureOf(code = "42501", serverMessage = "permission denied", cause = cause)

        assertEquals(CONNECTION_FAILURE_MESSAGE, failure.message)
        assertSame(cause, failure.cause)
    }

    @Test
    fun authCodesForBadDataMapToInvalidAuthData() {
        listOf(
            AuthErrorCode.InvalidCredentials,
            AuthErrorCode.EmailExists,
            AuthErrorCode.UserAlreadyExists,
            AuthErrorCode.WeakPassword,
            AuthErrorCode.EmailAddressInvalid,
            AuthErrorCode.ValidationFailed,
        ).forEach { assertIs<InvalidAuthDataException>(authFailureOf(it, RuntimeException()), it.name) }
    }

    @Test
    fun unconfirmedEmailMapsToConfirmationRequired() {
        assertIs<EmailConfirmationRequiredException>(authFailureOf(AuthErrorCode.EmailNotConfirmed, RuntimeException()))
    }

    @Test
    fun otherAuthFailuresKeepTheOriginalError() {
        val cause = RuntimeException("timeout")

        assertSame(cause, authFailureOf(AuthErrorCode.UnexpectedFailure, cause))
        assertSame(cause, authFailureOf(null, cause))
    }

    @Test
    fun photoPathsUseTheOwnerFolder() {
        assertEquals("u1/abc.jpg", StoragePaths.objectPath(folder = "u1", fileName = "abc.jpg"))
        assertEquals("report-photos", StoragePaths.REPORT_PHOTOS)
        assertEquals("adoption-photos", StoragePaths.ADOPTION_PHOTOS)
        assertEquals("place-photos", StoragePaths.PLACE_PHOTOS)
    }

    @Test
    fun generatedPhotoNamesAreUniqueJpegs() {
        val first = StoragePaths.newPhotoName()
        val second = StoragePaths.newPhotoName()

        assertTrue(first.endsWith(".jpg"))
        assertFalse(first == second)
    }

    @Test
    fun onlyDeviceRefsAreUploaded() {
        assertTrue(StoragePaths.needsUpload("content://media/picker/0/1"))
        assertTrue(StoragePaths.needsUpload("memory://photo/3"))
        assertFalse(StoragePaths.needsUpload("res:sample_report_labrador"))
        assertFalse(StoragePaths.needsUpload("https://x.supabase.co/storage/v1/object/public/report-photos/u1/a.jpg"))
    }
}
