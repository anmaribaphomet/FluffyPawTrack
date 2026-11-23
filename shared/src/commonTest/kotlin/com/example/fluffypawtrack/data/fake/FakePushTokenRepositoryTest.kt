package com.example.fluffypawtrack.data.fake

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
}
