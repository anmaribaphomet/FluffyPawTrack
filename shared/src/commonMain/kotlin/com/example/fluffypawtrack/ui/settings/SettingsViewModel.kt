package com.example.fluffypawtrack.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.domain.repository.PushTokenRepository
import com.example.fluffypawtrack.platform.deleteDevicePushToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Toggles are local for Phase 1; they are not persisted yet. */
data class SettingsUiState(
    val language: String = "ES",
    val darkMode: Boolean = false,
    val notifications: Boolean = true,
    val signedOut: Boolean = false,
)

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val pushTokenRepository: PushTokenRepository,
    private val unregisterTimeout: Duration = 3.seconds,
    private val deleteDeviceToken: suspend () -> Unit = ::deleteDevicePushToken,
) : ViewModel() {

    private val state = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = state.asStateFlow()

    fun toggleDarkMode() = state.update { it.copy(darkMode = !it.darkMode) }

    fun toggleNotifications() = state.update { it.copy(notifications = !it.notifications) }

    fun signOut() {
        viewModelScope.launch {
            // RLS only lets the account delete its own token, so it goes before the session ends.
            // A failure or a slow network (timeout) does not keep the user signed in.
            withTimeoutOrNull(unregisterTimeout) { pushTokenRepository.unregister() }
            authRepository.signOut()
            // Deleting the FCM token on the phone makes a row the unregister missed useless: FCM
            // reports it UNREGISTERED and send-push removes it. It runs after the session ends so
            // the token sync cannot register a rotated token for the old account.
            try {
                withTimeoutOrNull(unregisterTimeout) { deleteDeviceToken() }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Best effort: sign-out never fails because of the device token.
            }
            state.update { it.copy(signedOut = true) }
        }
    }
}
