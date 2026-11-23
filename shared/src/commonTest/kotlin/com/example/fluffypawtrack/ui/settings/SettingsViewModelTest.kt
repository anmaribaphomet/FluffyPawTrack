package com.example.fluffypawtrack.ui.settings

import com.example.fluffypawtrack.data.fake.FakeAuthRepository
import com.example.fluffypawtrack.data.fake.FakePushTokenRepository
import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.domain.repository.PushTokenRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun togglesFlipTheirLocalState() {
        val auth = FakeAuthRepository(initialUser = SampleData.demoUser)
        val viewModel = SettingsViewModel(auth, FakePushTokenRepository(currentUser = { auth.currentUser.value }), deleteDeviceToken = {})
        val initial = viewModel.uiState.value

        viewModel.toggleDarkMode()
        viewModel.toggleNotifications()

        assertTrue(viewModel.uiState.value.darkMode != initial.darkMode)
        assertTrue(viewModel.uiState.value.notifications != initial.notifications)
    }

    @Test
    fun signOutClearsTheSessionEvenWhenTheTokenCannotBeDeleted() = runTest {
        val auth = FakeAuthRepository(initialUser = SampleData.demoUser)
        val viewModel = SettingsViewModel(auth, FakePushTokenRepository(currentUser = { auth.currentUser.value }), deleteDeviceToken = {})
        assertFalse(viewModel.uiState.value.signedOut)

        viewModel.signOut()

        assertNull(auth.currentUser.value)
        assertTrue(viewModel.uiState.value.signedOut)
    }

    @Test
    fun signOutDeletesTheDeviceTokenWhileTheSessionIsStillOpen() = runTest {
        val auth = FakeAuthRepository(initialUser = SampleData.demoUser)
        val tokens = FakePushTokenRepository(currentUser = { auth.currentUser.value })
        tokens.register("device-token")
        val viewModel = SettingsViewModel(auth, tokens, deleteDeviceToken = {})

        viewModel.signOut()

        assertEquals(emptySet(), tokens.tokensOf(SampleData.demoUser.id))
        assertNull(auth.currentUser.value)
    }

    @Test
    fun signOutContinuesWhenDeletingTheTokenTimesOut() = runTest {
        val auth = FakeAuthRepository(initialUser = SampleData.demoUser)
        val hanging = object : PushTokenRepository {
            override suspend fun register(token: String) = Result.success(Unit)
            override suspend fun unregister(): Result<Unit> = awaitCancellation()
        }
        val viewModel = SettingsViewModel(auth, hanging, deleteDeviceToken = {})

        viewModel.signOut()
        assertFalse(viewModel.uiState.value.signedOut, "waits for the delete first")
        advanceTimeBy(3.seconds + 1.seconds)

        assertNull(auth.currentUser.value)
        assertTrue(viewModel.uiState.value.signedOut)
    }

    @Test
    fun signOutDeletesTheFirebaseTokenAfterTheSessionEndsEvenWhenUnregisterFails() = runTest {
        val auth = FakeAuthRepository(initialUser = SampleData.demoUser)
        val failing = object : PushTokenRepository {
            override suspend fun register(token: String) = Result.success(Unit)
            override suspend fun unregister(): Result<Unit> = Result.failure(IllegalStateException("offline"))
        }
        val sessionWhenDeleted = mutableListOf<Any?>()
        val viewModel = SettingsViewModel(auth, failing, deleteDeviceToken = { sessionWhenDeleted += auth.currentUser.value })

        viewModel.signOut()

        assertEquals(listOf<Any?>(null), sessionWhenDeleted, "deleted once, after the session ended")
        assertTrue(viewModel.uiState.value.signedOut)
    }

    @Test
    fun signOutDeletesTheFirebaseTokenWhenUnregisterTimesOut() = runTest {
        val auth = FakeAuthRepository(initialUser = SampleData.demoUser)
        val hanging = object : PushTokenRepository {
            override suspend fun register(token: String) = Result.success(Unit)
            override suspend fun unregister(): Result<Unit> = awaitCancellation()
        }
        var deletions = 0
        val viewModel = SettingsViewModel(auth, hanging, deleteDeviceToken = { deletions++ })

        viewModel.signOut()
        assertEquals(0, deletions)
        advanceTimeBy(3.seconds + 1.seconds)

        assertEquals(1, deletions)
        assertTrue(viewModel.uiState.value.signedOut)
    }

    @Test
    fun signOutFinishesWhenDeletingTheFirebaseTokenHangsOrFails() = runTest {
        val auth = FakeAuthRepository(initialUser = SampleData.demoUser)
        val hangs = SettingsViewModel(auth, FakePushTokenRepository(currentUser = { auth.currentUser.value }), deleteDeviceToken = { awaitCancellation() })
        hangs.signOut()
        advanceTimeBy(3.seconds + 1.seconds)
        assertTrue(hangs.uiState.value.signedOut)

        val auth2 = FakeAuthRepository(initialUser = SampleData.demoUser)
        val fails = SettingsViewModel(auth2, FakePushTokenRepository(currentUser = { auth2.currentUser.value }), deleteDeviceToken = { error("no Firebase") })
        fails.signOut()
        assertNull(auth2.currentUser.value)
        assertTrue(fails.uiState.value.signedOut)
    }
}
