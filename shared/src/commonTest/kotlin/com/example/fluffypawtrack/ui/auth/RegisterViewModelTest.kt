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
class RegisterViewModelTest {

    private val repository = FakeAuthRepository()
    private lateinit var viewModel: RegisterViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = RegisterViewModel(repository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun fillValidForm(email: String = "ana@correo.com") {
        viewModel.onFirstNameChange("Ana")
        viewModel.onLastNameChange("López")
        viewModel.onEmailChange(email)
        viewModel.onPhoneChange("6621234567")
        viewModel.onPasswordChange("secreta123")
        viewModel.onConfirmPasswordChange("secreta123")
    }

    @Test
    fun validFormCreatesTheAccountAndSignsIn() = runTest {
        fillValidForm()

        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertTrue(state.registered)
        assertNull(state.errorMessage)
        assertEquals(RegisterErrors(), state.errors)
        val user = repository.currentUser.value
        assertEquals("ana@correo.com", user?.email)
        assertEquals("Ana", user?.firstName)
        assertEquals("López", user?.lastName)
        assertEquals("6621234567", user?.phone)
    }

    @Test
    fun blankPhoneIsStoredAsNull() = runTest {
        fillValidForm()
        viewModel.onPhoneChange("  ")

        viewModel.onSubmit()

        assertTrue(viewModel.uiState.value.registered)
        assertNull(repository.currentUser.value?.phone)
    }

    @Test
    fun missingRequiredFieldsAreReported() = runTest {
        viewModel.onSubmit()

        assertOnlyGenericError()
        assertFalse(viewModel.uiState.value.registered)
        assertNull(repository.currentUser.value)
    }

    @Test
    fun malformedEmailIsRejected() = runTest {
        fillValidForm(email = "ana@correo")

        viewModel.onSubmit()

        assertOnlyGenericError()
        assertNull(repository.currentUser.value)
    }

    @Test
    fun shortPasswordIsRejected() = runTest {
        fillValidForm()
        viewModel.onPasswordChange("corta")
        viewModel.onConfirmPasswordChange("corta")

        viewModel.onSubmit()

        assertOnlyGenericError()
        assertNull(repository.currentUser.value)
    }

    @Test
    fun mismatchedPasswordsAreRejected() = runTest {
        fillValidForm()
        viewModel.onConfirmPasswordChange("otra-clave-123")

        viewModel.onSubmit()

        assertOnlyGenericError()
        assertNull(repository.currentUser.value)
    }

    @Test
    fun existingEmailShowsOnlyTheGenericError() = runTest {
        fillValidForm(email = "demo@correo.com")

        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertOnlyGenericError()
        assertFalse(state.registered)
        assertNull(repository.currentUser.value)
    }

    /** Only "Datos inválidos, intenta de nuevo." is shown: no field-level hints. */
    private fun assertOnlyGenericError() {
        val state = viewModel.uiState.value
        assertEquals(INVALID_DATA_MESSAGE, state.errorMessage)
        assertEquals(RegisterErrors(), state.errors)
    }
}
