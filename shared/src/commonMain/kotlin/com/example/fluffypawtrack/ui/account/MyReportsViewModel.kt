package com.example.fluffypawtrack.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.model.Report
import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.domain.repository.ReportRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyReportsUiState(
    val reports: List<Report> = emptyList(),
    val selectedId: String? = null,
    /** Readable error from the last delete or edit, shown once. */
    val message: String? = null,
    val loaded: Boolean = false,
) {
    val selected: Report? get() = reports.firstOrNull { it.id == selectedId }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MyReportsViewModel(
    authRepository: AuthRepository,
    private val reportRepository: ReportRepository,
) : ViewModel() {

    private val selectedId = MutableStateFlow<String?>(null)
    private val message = MutableStateFlow<String?>(null)

    private val reports = authRepository.currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else reportRepository.observeReportsBy(user.id)
    }

    val uiState: StateFlow<MyReportsUiState> = combine(reports, selectedId, message) { list, selected, error ->
        MyReportsUiState(
            // Newest first, like a history.
            reports = list.sortedByDescending { it.createdAt },
            selectedId = selected,
            message = error,
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyReportsUiState())

    fun select(id: String?) {
        selectedId.value = id
    }

    fun delete(id: String) {
        viewModelScope.launch {
            reportRepository.deleteReport(id)
                .onSuccess { if (selectedId.value == id) selectedId.value = null }
                .onFailure { message.value = it.message ?: "No se pudo eliminar el reporte" }
        }
    }

    fun update(id: String, description: String, reference: String?) {
        if (description.isBlank()) {
            message.value = "La descripción no puede quedar vacía"
            return
        }
        viewModelScope.launch {
            reportRepository.updateReport(id, description.trim(), reference?.trim()?.takeIf { it.isNotEmpty() })
                .onSuccess { message.value = null }
                .onFailure { message.value = it.message ?: "No se pudo guardar el reporte" }
        }
    }

    fun dismissMessage() {
        message.update { null }
    }
}
