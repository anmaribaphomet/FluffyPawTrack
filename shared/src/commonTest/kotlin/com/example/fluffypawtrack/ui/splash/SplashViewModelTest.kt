package com.example.fluffypawtrack.ui.splash

import com.example.fluffypawtrack.data.fake.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    private val repository = FakeAuthRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun waitsForTheAnimationBeforeDeciding() = runTest {
        val viewModel = SplashViewModel(repository, minDurationMillis = 1_500)

        advanceTimeBy(1_000)

        assertNull(viewModel.uiState.value.isSignedIn)
    }

    @Test
    fun reportsSignedOutWhenThereIsNoSession() = runTest {
        val viewModel = SplashViewModel(repository, minDurationMillis = 1_500)

        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isSignedIn)
    }

    @Test
    fun reportsSignedInWhenAUserHasASession() = runTest {
        repository.signIn("demo@correo.com", "demo1234")
        val viewModel = SplashViewModel(repository, minDurationMillis = 1_500)

        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.isSignedIn)
    }

    @Test
    fun anAnonymousSessionDoesNotCountAsSignedIn() = runTest {
        repository.signInAnonymously()
        val viewModel = SplashViewModel(repository, minDurationMillis = 1_500)

        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isSignedIn)
    }
}
