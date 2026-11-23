package com.example.fluffypawtrack.ui.settings

import com.example.fluffypawtrack.data.fake.FakeAuthRepository
import com.example.fluffypawtrack.data.fake.FakePushTokenRepository
import com.example.fluffypawtrack.data.fake.SampleData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun togglesFlipTheirLocalState() {
        val auth = FakeAuthRepository(initialUser = SampleData.demoUser)
        val viewModel = SettingsViewModel(auth, FakePushTokenRepository(currentUser = { auth.currentUser.value }))
        val initial = viewModel.uiState.value

        viewModel.toggleDarkMode()
        viewModel.toggleNotifications()

        assertTrue(viewModel.uiState.value.darkMode != initial.darkMode)
        assertTrue(viewModel.uiState.value.notifications != initial.notifications)
    }

    @Test
    fun signOutClearsTheSession() = runTest {
        val auth = FakeAuthRepository(initialUser = SampleData.demoUser)
        val viewModel = SettingsViewModel(auth, FakePushTokenRepository(currentUser = { auth.currentUser.value }))
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
        val viewModel = SettingsViewModel(auth, tokens)

        viewModel.signOut()

        assertEquals(emptySet(), tokens.tokensOf(SampleData.demoUser.id))
        assertNull(auth.currentUser.value)
    }
}
