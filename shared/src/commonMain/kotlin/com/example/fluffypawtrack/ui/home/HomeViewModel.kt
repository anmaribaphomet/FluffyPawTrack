package com.example.fluffypawtrack.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.domain.repository.NotificationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    /** Unread notifications of the signed-in account, shown as a badge on the bottom bar. */
    val unreadNotifications: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    notificationRepository: NotificationRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    // Stops 5 s after Home leaves the screen, which also closes the live channel behind it.
    val uiState: StateFlow<HomeUiState> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else notificationRepository.observeNotifications(user.id)
        }
        .map { list -> HomeUiState(unreadNotifications = list.count { !it.isRead }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
