package com.example.fluffypawtrack.ui.auth

import com.example.fluffypawtrack.data.fake.FakeAuthRepository
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
class LoginViewModelTest {

    private val repository = FakeAuthRepository()
    private lateinit var viewModel: LoginViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = LoginViewModel(repository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun signInWithValidCredentialsSignsTheUserIn() = runTest {
        viewModel.onEmailChange("demo@correo.com")
        viewModel.onPasswordChange("demo1234")

        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertEquals(LoginResult.SignedIn, state.result)
        assertNull(state.errorMessage)
        assertFalse(state.isLoading)
        assertEquals("demo@correo.com", repository.currentUser.value?.email)
    }

    @Test
    fun wrongPasswordShowsTheInvalidDataMessage() = runTest {
        viewModel.onEmailChange("demo@correo.com")
        viewModel.onPasswordChange("wrong-password")

        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertEquals(INVALID_DATA_MESSAGE, state.errorMessage)
        assertNull(state.result)
        assertNull(repository.currentUser.value)
    }

    @Test
    fun unknownEmailShowsTheSameMessageAsWrongPassword() = runTest {
        viewModel.onEmailChange("demo@correo.com")
        viewModel.onPasswordChange("wrong-password")
        viewModel.onSubmit()
        val wrongPasswordMessage = viewModel.uiState.value.errorMessage

        viewModel.onEmailChange("nadie@correo.com")
        viewModel.onPasswordChange("demo1234")
        viewModel.onSubmit()

        assertEquals(INVALID_DATA_MESSAGE, wrongPasswordMessage)
        assertEquals(wrongPasswordMessage, viewModel.uiState.value.errorMessage)
        assertNull(repository.currentUser.value)
    }

    @Test
    fun connectionFailureShowsTheConnectionMessage() = runTest {
        val failing = LoginViewModel(FailingAuthRepository())
        failing.onEmailChange("demo@correo.com")
        failing.onPasswordChange("demo1234")

        failing.onSubmit()

        val state = failing.uiState.value
        assertEquals(CONNECTION_ERROR_MESSAGE, state.errorMessage)
        assertNull(state.result)
        assertFalse(state.isLoading)
    }

    @Test
    fun anonymousSessionFailureShowsTheConnectionMessage() = runTest {
        val failing = LoginViewModel(FailingAuthRepository())

        failing.onReportAnonymously()

        assertEquals(CONNECTION_ERROR_MESSAGE, failing.uiState.value.errorMessage)
        assertNull(failing.uiState.value.result)
    }

    @Test
    fun emptyFieldsAreRejectedWithoutCallingTheRepository() = runTest {
        viewModel.onSubmit()

        assertEquals(INVALID_DATA_MESSAGE, viewModel.uiState.value.errorMessage)
        assertNull(repository.currentUser.value)
    }

    @Test
    fun editingAFieldClearsThePreviousError() = runTest {
        viewModel.onEmailChange("demo@correo.com")
        viewModel.onPasswordChange("wrong-password")
        viewModel.onSubmit()

        viewModel.onPasswordChange("demo1234")

        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun reportWithoutAccountStartsAnAnonymousSession() = runTest {
        viewModel.onReportAnonymously()

        assertEquals(LoginResult.Anonymous, viewModel.uiState.value.result)
        assertTrue(repository.currentUser.value?.isAnonymous == true)
    }
}
