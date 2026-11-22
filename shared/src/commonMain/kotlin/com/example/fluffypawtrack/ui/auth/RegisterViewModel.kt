package com.example.fluffypawtrack.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val errors: RegisterErrors = RegisterErrors(),
    val isLoading: Boolean = false,
    /** Error returned by the repository (for example, the email is taken). */
    val errorMessage: String? = null,
    val registered: Boolean = false,
)

class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val state = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = state.asStateFlow()

    fun onFirstNameChange(value: String) =
        state.update { it.copy(firstName = value, errors = it.errors.copy(firstName = null), errorMessage = null) }

    fun onLastNameChange(value: String) =
        state.update { it.copy(lastName = value, errors = it.errors.copy(lastName = null), errorMessage = null) }

    fun onEmailChange(value: String) =
        state.update { it.copy(email = value, errors = it.errors.copy(email = null), errorMessage = null) }

    fun onPhoneChange(value: String) = state.update { it.copy(phone = value, errorMessage = null) }

    fun onPasswordChange(value: String) =
        state.update { it.copy(password = value, errors = it.errors.copy(password = null), errorMessage = null) }

    fun onConfirmPasswordChange(value: String) = state.update {
        it.copy(confirmPassword = value, errors = it.errors.copy(confirmPassword = null), errorMessage = null)
    }

    fun onSubmit() {
        val form = state.value
        if (form.isLoading) return
        val errors = validateRegistration(form.firstName, form.lastName, form.email, form.password, form.confirmPassword)
        if (errors.hasErrors) {
            state.update { it.copy(errors = errors, errorMessage = null) }
            return
        }
        state.update { it.copy(errors = RegisterErrors(), isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            authRepository.signUp(
                email = form.email.trim(),
                password = form.password,
                firstName = form.firstName.trim(),
                lastName = form.lastName.trim(),
                phone = form.phone.trim().ifEmpty { null },
            )
                .onSuccess { state.update { it.copy(isLoading = false, registered = true) } }
                .onFailure { error -> state.update { it.copy(isLoading = false, errorMessage = error.readableMessage()) } }
        }
    }

    /** Called by the route once it has navigated away. */
    fun onRegisteredHandled() = state.update { it.copy(registered = false) }
}
