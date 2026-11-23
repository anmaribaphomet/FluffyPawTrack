package com.example.fluffypawtrack.domain.push

import com.example.fluffypawtrack.data.fake.FakeAuthRepository
import com.example.fluffypawtrack.data.fake.FakePushTokenRepository
import com.example.fluffypawtrack.data.fake.SampleData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.example.fluffypawtrack.domain.repository.PushTokenRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.seconds
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class PushTokenSyncTest {
    private val auth = FakeAuthRepository()
    private val tokens = FakePushTokenRepository(currentUser = { auth.currentUser.value })
    private val refreshed = MutableSharedFlow<String>()
    private val sync = PushTokenSync(auth, tokens, currentToken = { "device-token" }, refreshedTokens = refreshed)

    @Test
    fun registersTheDeviceTokenWhenAnAccountSignsIn() = runTest(UnconfinedTestDispatcher()) {
        val job = sync.start(backgroundScope)

        val user = auth.signIn(SampleData.demoUser.email!!, SampleData.DEMO_PASSWORD).getOrThrow()

        assertEquals(setOf("device-token"), tokens.tokensOf(user.id))
        job.cancel()
    }

    @Test
    fun registersARestoredSessionOnStart() = runTest(UnconfinedTestDispatcher()) {
        auth.signIn(SampleData.demoUser.email!!, SampleData.DEMO_PASSWORD)

        sync.start(backgroundScope)

        assertEquals(setOf("device-token"), tokens.tokensOf(SampleData.demoUser.id))
    }

    @Test
    fun skipsAnonymousSessions() = runTest(UnconfinedTestDispatcher()) {
        sync.start(backgroundScope)

        val anonymous = auth.signInAnonymously().getOrThrow()
        refreshed.emit("rotated-token")

        assertEquals(emptySet(), tokens.tokensOf(anonymous.id))
    }

    @Test
    fun registersARotatedTokenForTheSignedInAccount() = runTest(UnconfinedTestDispatcher()) {
        sync.start(backgroundScope)
        auth.signIn(SampleData.demoUser.email!!, SampleData.DEMO_PASSWORD)

        refreshed.emit("rotated-token")

        assertEquals(setOf("device-token", "rotated-token"), tokens.tokensOf(SampleData.demoUser.id))
    }

    @Test
    fun retriesAFailedRegistrationWithBackoff() = runTest(UnconfinedTestDispatcher()) {
        val failing = AlwaysFailingTokens()
        PushTokenSync(auth, failing, currentToken = { "device-token" }, refreshedTokens = refreshed)
            .start(backgroundScope)

        auth.signIn(SampleData.demoUser.email!!, SampleData.DEMO_PASSWORD)
        assertEquals(1, failing.attempts)
        advanceTimeBy(2.seconds + 1.seconds)
        assertEquals(2, failing.attempts)
        advanceTimeBy(10.seconds)
        assertEquals(3, failing.attempts)
        advanceTimeBy(30.seconds)
        assertEquals(4, failing.attempts)
        advanceTimeBy(120.seconds)
        assertEquals(4, failing.attempts, "retries are bounded")
    }

    @Test
    fun signingOutCancelsPendingRetries() = runTest(UnconfinedTestDispatcher()) {
        val failing = AlwaysFailingTokens()
        PushTokenSync(auth, failing, currentToken = { "device-token" }, refreshedTokens = refreshed)
            .start(backgroundScope)
        auth.signIn(SampleData.demoUser.email!!, SampleData.DEMO_PASSWORD)

        auth.signOut()
        advanceTimeBy(120.seconds)

        assertEquals(1, failing.attempts)
    }

    private class AlwaysFailingTokens : PushTokenRepository {
        var attempts = 0

        override suspend fun register(token: String): Result<Unit> {
            attempts++
            return Result.failure(IllegalStateException("offline"))
        }

        override suspend fun unregister(): Result<Unit> = Result.success(Unit)
    }
}
