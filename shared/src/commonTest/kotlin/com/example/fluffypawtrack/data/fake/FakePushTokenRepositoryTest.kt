package com.example.fluffypawtrack.data.fake

import com.example.fluffypawtrack.data.PushTokenFailures
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakePushTokenRepositoryTest {

    @Test
    fun registerStoresTheTokenForTheSignedInAccount() = runTest {
        val auth = FakeAuthRepository(initialUser = SampleData.demoUser)
        val tokens = FakePushTokenRepository(currentUser = { auth.currentUser.value })

        val result = tokens.register("token-1")

        assertTrue(result.isSuccess)
        assertEquals(setOf("token-1"), tokens.tokensOf(SampleData.demoUser.id))
    }

    @Test
    fun registerRejectsAnonymousSessionsAndSignedOutUsers() = runTest {
        val auth = FakeAuthRepository()
        val tokens = FakePushTokenRepository(currentUser = { auth.currentUser.value })

        assertTrue(tokens.register("token-1").isFailure)

        val anonymous = auth.signInAnonymously().getOrThrow()
        assertTrue(tokens.register("token-1").isFailure)
        assertEquals(emptySet(), tokens.tokensOf(anonymous.id))
    }

    @Test
    fun unregisterDeletesOnlyThisDeviceToken() = runTest {
        val auth = FakeAuthRepository(initialUser = SampleData.demoUser)
        val tokens = FakePushTokenRepository(
            currentUser = { auth.currentUser.value },
            initialTokens = mapOf(SampleData.demoUser.id to setOf("other-device")),
        )
        tokens.register("this-device")

        assertTrue(tokens.unregister().isSuccess)

        assertEquals(setOf("other-device"), tokens.tokensOf(SampleData.demoUser.id))
    }

    @Test
    fun registerFailuresNameTheirCause() = runTest {
        val auth = FakeAuthRepository()
        val tokens = FakePushTokenRepository(currentUser = { auth.currentUser.value })

        assertEquals(PushTokenFailures.NO_SESSION, tokens.register("token-1").exceptionOrNull()?.message)
        auth.signInAnonymously()
        assertEquals(PushTokenFailures.ANONYMOUS_SESSION, tokens.register("token-1").exceptionOrNull()?.message)
        auth.signIn(SampleData.demoUser.email!!, SampleData.DEMO_PASSWORD)
        assertEquals(PushTokenFailures.EMPTY_TOKEN, tokens.register(" ").exceptionOrNull()?.message)
    }

    @Test
    fun unregisterDeletesTheTokenThePlatformReportsEvenIfThisSessionNeverRegisteredIt() = runTest {
        val auth = FakeAuthRepository(initialUser = SampleData.demoUser)
        val tokens = FakePushTokenRepository(
            currentUser = { auth.currentUser.value },
            deviceToken = { "platform-token" },
            initialTokens = mapOf(SampleData.demoUser.id to setOf("platform-token", "other-device")),
        )

        assertTrue(tokens.unregister().isSuccess)

        assertEquals(setOf("other-device"), tokens.tokensOf(SampleData.demoUser.id))
    }

    @Test
    fun unregisterFailsWhenTheDeviceTokenIsUnknown() = runTest {
        val auth = FakeAuthRepository(initialUser = SampleData.demoUser)
        val tokens = FakePushTokenRepository(
            currentUser = { auth.currentUser.value },
            initialTokens = mapOf(SampleData.demoUser.id to setOf("stale-token")),
        )

        val result = tokens.unregister()

        assertEquals(PushTokenFailures.UNKNOWN_DEVICE_TOKEN, result.exceptionOrNull()?.message)
        assertEquals(setOf("stale-token"), tokens.tokensOf(SampleData.demoUser.id))
    }

    @Test
    fun unregisterFailsWithoutASession() = runTest {
        val tokens = FakePushTokenRepository(currentUser = { null }, deviceToken = { "platform-token" })

        assertEquals(PushTokenFailures.NO_SESSION, tokens.unregister().exceptionOrNull()?.message)
    }
}
