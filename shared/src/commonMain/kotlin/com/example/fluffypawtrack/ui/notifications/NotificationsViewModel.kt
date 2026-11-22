package com.example.fluffypawtrack.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.model.AppNotification
import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.domain.repository.NotificationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val notifications: List<AppNotification> = emptyList(),
) {
    val unreadCount: Int get() = notifications.count { !it.isRead }
}

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModel(
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val uiState: StateFlow<NotificationsUiState> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else notificationRepository.observeNotifications(user.id)
        }
        .map { list -> NotificationsUiState(list.sortedByDescending { it.receivedAt }) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, NotificationsUiState())

    fun onNotificationClick(notificationId: String) {
        viewModelScope.launch { notificationRepository.markAsRead(notificationId) }
    }

    fun onMarkAllAsRead() {
        val userId = authRepository.currentUser.value?.id ?: return
        viewModelScope.launch { notificationRepository.markAllAsRead(userId) }
    }
}
