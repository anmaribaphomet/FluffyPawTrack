package com.example.fluffypawtrack.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.domain.repository.PushTokenRepository
import com.example.fluffypawtrack.platform.deleteDevicePushToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
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
    /** Bounds both sign-out cleanups, which run at the same time, so sign-out waits at most this. */
    private val pushCleanupTimeout: Duration = 3.seconds,
    private val deleteDeviceToken: suspend () -> Unit = ::deleteDevicePushToken,
) : ViewModel() {

    private val state = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = state.asStateFlow()

    fun toggleDarkMode() = state.update { it.copy(darkMode = !it.darkMode) }

    fun toggleNotifications() = state.update { it.copy(notifications = !it.notifications) }

    fun signOut() {
        viewModelScope.launch {
            // Both cleanups run at once while the session is open (RLS only lets the account
            // delete its own row), each bounded, so a slow network delays sign-out by at most
            // pushCleanupTimeout. Deleting the FCM token on the phone makes a row the unregister
            // missed useless: FCM reports it UNREGISTERED and send-push removes it. Firebase only
            // mints a new token on the next app start, so the token sync cannot register a
            // rotated token for this account in the meantime.
            coroutineScope {
                launch { bestEffort { pushTokenRepository.unregister() } }
                launch { bestEffort { deleteDeviceToken() } }
            }
            authRepository.signOut()
            state.update { it.copy(signedOut = true) }
        }
    }

    /** Sign-out never fails or hangs because of a push cleanup. */
    private suspend fun bestEffort(cleanup: suspend () -> Unit) {
        try {
            withTimeoutOrNull(pushCleanupTimeout) { cleanup() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Best effort: the other cleanup still covers this device.
        }
    }
}
