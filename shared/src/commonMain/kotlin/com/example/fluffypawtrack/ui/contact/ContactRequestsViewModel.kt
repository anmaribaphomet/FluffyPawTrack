package com.example.fluffypawtrack.ui.contact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.model.ContactDetails
import com.example.fluffypawtrack.domain.model.ContactRequest
import com.example.fluffypawtrack.domain.model.ContactRequestStatus
import com.example.fluffypawtrack.domain.model.Report
import com.example.fluffypawtrack.domain.model.ReportStatus
import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.domain.repository.ContactRequestRepository
import com.example.fluffypawtrack.domain.repository.ReportRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

enum class ContactRequestsTab(val label: String) { Sent("Enviadas"), Received("Recibidas") }

/** One card: the request, its report for the thumbnail, and contact data once accepted. */
data class ContactRequestItem(
    val request: ContactRequest,
    val report: Report?,
    /** The other party's data; only present when the request is accepted. */
    val contact: ContactDetails?,
) {
    val canCancel: Boolean get() = request.status == ContactRequestStatus.Pending
    val canRespond: Boolean get() = request.status == ContactRequestStatus.Pending
    val canMarkDelivered: Boolean
        get() = request.status == ContactRequestStatus.Accepted && report != null && report.status != ReportStatus.Closed
    val isDelivered: Boolean
        get() = request.status == ContactRequestStatus.Accepted && report?.status == ReportStatus.Closed
}

data class ContactRequestsUiState(
    val tab: ContactRequestsTab = ContactRequestsTab.Sent,
    val sent: List<ContactRequestItem> = emptyList(),
    val received: List<ContactRequestItem> = emptyList(),
    /** Readable error from the last action, shown once. */
    val message: String? = null,
    val loaded: Boolean = false,
) {
    val visible: List<ContactRequestItem>
        get() = if (tab == ContactRequestsTab.Sent) sent else received
}

private data class Lists(val sent: List<ContactRequestItem>, val received: List<ContactRequestItem>)

/**
 * "Solicitudes de contacto" with the Enviadas and Recibidas tabs (RF-05). For an affiliate,
 * Recibidas also lists the requests sent to their organization.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class ContactRequestsViewModel(
    private val authRepository: AuthRepository,
    private val contactRequestRepository: ContactRequestRepository,
    private val reportRepository: ReportRepository,
    initialTab: ContactRequestsTab = ContactRequestsTab.Sent,
) : ViewModel() {

    private val tab = MutableStateFlow(initialTab)
    private val message = MutableStateFlow<String?>(null)

    // Bumped after "Mascota entregada": the report closes even if no request changes.
    private val refresh = MutableStateFlow(0)

    private val lists = combine(authRepository.currentUser, refresh) { user, _ -> user }
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(Lists(emptyList(), emptyList()))
            } else {
                combine(
                    contactRequestRepository.observeSent(user.id),
                    contactRequestRepository.observeReceived(user.id),
                ) { sent, received -> sent to received }
                    .mapLatest { (sent, received) -> Lists(sent.toItems(user.id), received.toItems(user.id)) }
            }
        }

    val uiState: StateFlow<ContactRequestsUiState> = combine(lists, tab, message) { all, selected, error ->
        ContactRequestsUiState(tab = selected, sent = all.sent, received = all.received, message = error, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContactRequestsUiState())

    private suspend fun List<ContactRequest>.toItems(userId: String): List<ContactRequestItem> =
        sortedByDescending { it.createdAt }.map { request ->
            ContactRequestItem(
                request = request,
                report = reportRepository.getReport(request.reportId),
                contact = if (request.status == ContactRequestStatus.Accepted) {
                    contactRequestRepository.getContactDetails(request.id, userId)
                } else {
                    null
                },
            )
        }

    fun selectTab(selected: ContactRequestsTab) {
        tab.value = selected
    }

    fun cancel(requestId: String) = act { contactRequestRepository.cancel(requestId, it) }

    fun accept(requestId: String) = act { contactRequestRepository.accept(requestId, it) }

    fun reject(requestId: String) = act { contactRequestRepository.reject(requestId, it) }

    fun markDelivered(requestId: String) = act { contactRequestRepository.markDelivered(requestId, it) }

    fun dismissMessage() {
        message.value = null
    }

    private fun act(action: suspend (userId: String) -> Result<*>) {
        val user = authRepository.currentUser.value ?: return
        viewModelScope.launch {
            action(user.id)
                .onSuccess {
                    message.value = null
                    refresh.value++
                }
                .onFailure { message.value = it.message ?: "No se pudo actualizar la solicitud" }
        }
    }
}
