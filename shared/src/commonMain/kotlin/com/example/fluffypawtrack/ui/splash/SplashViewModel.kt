package com.example.fluffypawtrack.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** [isSignedIn] stays null while the loading animation runs. */
data class SplashUiState(val isSignedIn: Boolean? = null)

class SplashViewModel(
    private val authRepository: AuthRepository,
    minDurationMillis: Long = DEFAULT_DURATION_MILLIS,
) : ViewModel() {

    private val state = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            delay(minDurationMillis)
            val user = authRepository.currentUser.value
            // An anonymous session only exists to submit reports, so it still goes through login.
            state.value = SplashUiState(isSignedIn = user != null && !user.isAnonymous)
        }
    }

    companion object {
        const val DEFAULT_DURATION_MILLIS = 1_500L
    }
}
