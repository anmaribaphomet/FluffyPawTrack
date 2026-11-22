package com.example.fluffypawtrack.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** How the login screen was left; the route turns it into a navigation callback. */
enum class LoginResult { SignedIn, Anonymous }

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val result: LoginResult? = null,
)

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val state = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = state.asStateFlow()

    fun onEmailChange(value: String) = state.update { it.copy(email = value, errorMessage = null) }

    fun onPasswordChange(value: String) = state.update { it.copy(password = value, errorMessage = null) }

    fun onSubmit() {
        val current = state.value
        if (current.isLoading) return
        if (current.email.isBlank() || current.password.isEmpty()) {
            state.update { it.copy(errorMessage = MISSING_FIELDS) }
            return
        }
        state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            authRepository.signIn(current.email.trim(), current.password)
                .onSuccess { state.update { it.copy(isLoading = false, result = LoginResult.SignedIn) } }
                .onFailure { error -> state.update { it.copy(isLoading = false, errorMessage = error.readableMessage()) } }
        }
    }

    fun onReportAnonymously() {
        if (state.value.isLoading) return
        state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            authRepository.signInAnonymously()
                .onSuccess { state.update { it.copy(isLoading = false, result = LoginResult.Anonymous) } }
                .onFailure { error -> state.update { it.copy(isLoading = false, errorMessage = error.readableMessage()) } }
        }
    }

    /** Called by the route once it has navigated, so returning to this screen does not navigate again. */
    fun onResultHandled() = state.update { it.copy(result = null) }

    private companion object {
        const val MISSING_FIELDS = "Ingresa tu correo y contraseña"
    }
}

/** Repository errors already carry user-facing Spanish copy; anything else gets a generic message. */
internal fun Throwable.readableMessage(): String =
    message?.takeIf { it.isNotBlank() } ?: "Algo salió mal. Inténtalo de nuevo."
