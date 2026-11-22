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

        val errors = viewModel.uiState.value.errors
        assertEquals("Ingresa tu nombre", errors.firstName)
        assertEquals("Ingresa tus apellidos", errors.lastName)
        assertEquals("Ingresa tu correo", errors.email)
        assertEquals("Ingresa una contraseña", errors.password)
        assertEquals("Confirma tu contraseña", errors.confirmPassword)
        assertFalse(viewModel.uiState.value.registered)
        assertNull(repository.currentUser.value)
    }

    @Test
    fun malformedEmailIsRejected() = runTest {
        fillValidForm(email = "ana@correo")

        viewModel.onSubmit()

        assertEquals("Ingresa un correo válido", viewModel.uiState.value.errors.email)
        assertNull(repository.currentUser.value)
    }

    @Test
    fun shortPasswordIsRejected() = runTest {
        fillValidForm()
        viewModel.onPasswordChange("corta")
        viewModel.onConfirmPasswordChange("corta")

        viewModel.onSubmit()

        assertEquals("La contraseña debe tener al menos 8 caracteres", viewModel.uiState.value.errors.password)
        assertNull(repository.currentUser.value)
    }

    @Test
    fun mismatchedPasswordsAreRejected() = runTest {
        fillValidForm()
        viewModel.onConfirmPasswordChange("otra-clave-123")

        viewModel.onSubmit()

        assertEquals("Las contraseñas no coinciden", viewModel.uiState.value.errors.confirmPassword)
        assertNull(repository.currentUser.value)
    }

    @Test
    fun existingEmailShowsTheRepositoryError() = runTest {
        fillValidForm(email = "demo@correo.com")

        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertEquals("Ya existe una cuenta con ese correo", state.errorMessage)
        assertFalse(state.registered)
        assertNull(repository.currentUser.value)
    }
}
