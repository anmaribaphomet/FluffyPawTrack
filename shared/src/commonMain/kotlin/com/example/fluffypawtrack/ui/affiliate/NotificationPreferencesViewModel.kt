package com.example.fluffypawtrack.ui.affiliate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.model.NotificationPrefs
import com.example.fluffypawtrack.domain.model.Priority
import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.domain.repository.NotificationPrefsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal const val MISSING_PRIORITY = "Elige al menos una prioridad"
internal const val MIN_RADIUS_KM = 1
internal const val MAX_RADIUS_KM = 20

data class NotificationPreferencesUiState(
    val radiusKm: Int = NotificationPrefs.DEFAULT_RADIUS_KM.roundToInt(),
    val priorities: Set<Priority> = Priority.entries.toSet(),
    val pushEnabled: Boolean = true,
    val isSaving: Boolean = false,
    val message: String? = null,
    /** True after the last save succeeded, until the next edit. */
    val saved: Boolean = false,
    val loaded: Boolean = false,
)

/** RF-03 preferences: radius (1–20 km), priorities and push. */
class NotificationPreferencesViewModel(
    private val authRepository: AuthRepository,
    private val prefsRepository: NotificationPrefsRepository,
) : ViewModel() {

    private val state = MutableStateFlow(NotificationPreferencesUiState())
    val uiState: StateFlow<NotificationPreferencesUiState> = state.asStateFlow()

    init {
        authRepository.currentUser.value?.id?.let { userId ->
            viewModelScope.launch {
                val prefs = prefsRepository.observe(userId).first()
                state.update {
                    it.copy(
                        radiusKm = prefs.radiusKm.roundToInt().coerceIn(MIN_RADIUS_KM, MAX_RADIUS_KM),
                        priorities = prefs.priorities,
                        pushEnabled = prefs.pushEnabled,
                        loaded = true,
                    )
                }
            }
        }
    }

    fun onRadiusChange(value: Float) = edit { it.copy(radiusKm = value.roundToInt().coerceIn(MIN_RADIUS_KM, MAX_RADIUS_KM)) }

    fun togglePriority(priority: Priority) = edit {
        it.copy(priorities = if (priority in it.priorities) it.priorities - priority else it.priorities + priority)
    }

    fun onPushChange(enabled: Boolean) = edit { it.copy(pushEnabled = enabled) }

    fun onSave() {
        val userId = authRepository.currentUser.value?.id ?: return
        val current = state.value
        if (current.isSaving) return
        if (current.priorities.isEmpty()) {
            state.update { it.copy(message = MISSING_PRIORITY) }
            return
        }
        state.update { it.copy(isSaving = true, message = null) }
        viewModelScope.launch {
            prefsRepository.update(userId, current.radiusKm.toDouble(), current.priorities, current.pushEnabled)
                .onSuccess { state.update { it.copy(isSaving = false, saved = true) } }
                .onFailure { error ->
                    state.update { it.copy(isSaving = false, message = error.message ?: "No se pudieron guardar las preferencias") }
                }
        }
    }

    private fun edit(transform: (NotificationPreferencesUiState) -> NotificationPreferencesUiState) =
        state.update { transform(it).copy(saved = false, message = null) }
}
