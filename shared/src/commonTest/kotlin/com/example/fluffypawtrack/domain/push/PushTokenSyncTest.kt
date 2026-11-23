package com.example.fluffypawtrack.domain.push

import com.example.fluffypawtrack.data.fake.FakeAuthRepository
import com.example.fluffypawtrack.data.fake.FakePushTokenRepository
import com.example.fluffypawtrack.data.fake.SampleData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
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
}
