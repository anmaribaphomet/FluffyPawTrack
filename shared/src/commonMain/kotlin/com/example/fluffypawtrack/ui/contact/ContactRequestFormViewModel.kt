package com.example.fluffypawtrack.ui.contact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.model.ContactRequestRules
import com.example.fluffypawtrack.domain.model.Report
import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.domain.repository.ContactRequestRepository
import com.example.fluffypawtrack.domain.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ContactRequestFormUiState(
    val report: Report? = null,
    val message: String = "",
    /** Local reference from the photo picker, or null when no photo was added. */
    val photoRef: String? = null,
    val error: String? = null,
    val isSubmitting: Boolean = false,
    val sent: Boolean = false,
) {
    val messageLength: Int get() = message.trim().length
    val counter: String get() = "$messageLength/${ContactRequestRules.MAX_MESSAGE_LENGTH}"
    val canSubmit: Boolean get() = ContactRequestRules.isValidMessage(message) && !isSubmitting

    /** Hint under the message field while it is outside 20–500 characters. */
    val messageHint: String?
        get() = when {
            messageLength < ContactRequestRules.MIN_MESSAGE_LENGTH ->
                "Escribe al menos ${ContactRequestRules.MIN_MESSAGE_LENGTH} caracteres"
            messageLength > ContactRequestRules.MAX_MESSAGE_LENGTH ->
                "El mensaje no puede pasar de ${ContactRequestRules.MAX_MESSAGE_LENGTH} caracteres"
            else -> null
        }
}

/** "Es mi mascota" form: message with proof (20–500 characters) and an optional photo. */
class ContactRequestFormViewModel(
    private val reportId: String,
    private val authRepository: AuthRepository,
    private val contactRequestRepository: ContactRequestRepository,
    reportRepository: ReportRepository,
) : ViewModel() {

    private val state = MutableStateFlow(ContactRequestFormUiState())
    val uiState: StateFlow<ContactRequestFormUiState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            val report = reportRepository.getReport(reportId)
            state.update {
                it.copy(report = report, error = if (report == null) "El reporte ya no está disponible" else it.error)
            }
        }
    }

    fun onMessageChange(value: String) {
        state.update { it.copy(message = value, error = null) }
    }

    fun onPhotoSelected(photoRef: String) {
        state.update { it.copy(photoRef = photoRef) }
    }

    fun onRemovePhoto() {
        state.update { it.copy(photoRef = null) }
    }

    fun onSubmit() {
        val current = state.value
        if (!current.canSubmit) {
            state.update { it.copy(error = it.messageHint) }
            return
        }
        val user = authRepository.currentUser.value
        if (user == null || user.isAnonymous) {
            state.update { it.copy(error = "Necesitas una cuenta para enviar solicitudes de contacto") }
            return
        }
        state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            contactRequestRepository.send(reportId, user.id, current.message.trim(), current.photoRef)
                .onSuccess { state.update { it.copy(isSubmitting = false, sent = true) } }
                .onFailure { e ->
                    state.update { it.copy(isSubmitting = false, error = e.message ?: "No se pudo enviar la solicitud") }
                }
        }
    }
}
