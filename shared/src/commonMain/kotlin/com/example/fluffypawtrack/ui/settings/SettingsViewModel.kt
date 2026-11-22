package com.example.fluffypawtrack.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Toggles are local for Phase 1; they are not persisted yet. */
data class SettingsUiState(
    val language: String = "ES",
    val darkMode: Boolean = false,
    val notifications: Boolean = true,
    val signedOut: Boolean = false,
)

class SettingsViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val state = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = state.asStateFlow()

    fun toggleDarkMode() = state.update { it.copy(darkMode = !it.darkMode) }

    fun toggleNotifications() = state.update { it.copy(notifications = !it.notifications) }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            state.update { it.copy(signedOut = true) }
        }
    }
}
