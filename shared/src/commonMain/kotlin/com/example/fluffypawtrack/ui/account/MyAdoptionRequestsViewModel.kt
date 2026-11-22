package com.example.fluffypawtrack.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.model.AdoptionRequest
import com.example.fluffypawtrack.domain.model.coverPhotoUrl
import com.example.fluffypawtrack.domain.repository.AdoptionRepository
import com.example.fluffypawtrack.domain.repository.AdoptionRequestRepository
import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.domain.repository.OrganizationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One row of "Mis solicitudes de adopción": the request plus what the card shows about the animal. */
data class MyAdoptionRequestItem(
    val request: AdoptionRequest,
    val animalName: String,
    val photoUrl: String,
    val centerName: String,
)

data class MyAdoptionRequestsUiState(
    val items: List<MyAdoptionRequestItem> = emptyList(),
    val loaded: Boolean = false,
    /** Readable repository failure, for example cancelling a request that is no longer pending. */
    val message: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MyAdoptionRequestsViewModel(
    private val authRepository: AuthRepository,
    private val adoptionRequestRepository: AdoptionRequestRepository,
    private val adoptionRepository: AdoptionRepository,
    private val organizationRepository: OrganizationRepository,
) : ViewModel() {

    private val message = MutableStateFlow<String?>(null)

    private val items = authRepository.currentUser.flatMapLatest { user ->
        if (user == null) {
            flowOf(emptyList())
        } else {
            adoptionRequestRepository.observeByRequester(user.id).map { requests -> requests.map { it.toItem() } }
        }
    }

    val uiState: StateFlow<MyAdoptionRequestsUiState> = combine(items, message) { list, text ->
        MyAdoptionRequestsUiState(items = list, loaded = true, message = text)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyAdoptionRequestsUiState())

    fun cancel(requestId: String) {
        val userId = authRepository.currentUser.value?.id ?: return
        viewModelScope.launch {
            adoptionRequestRepository.cancel(requestId, userId).onFailure { message.value = it.message }
        }
    }

    fun dismissMessage() {
        message.value = null
    }

    private suspend fun AdoptionRequest.toItem(): MyAdoptionRequestItem {
        val listing = adoptionRepository.getListing(listingId)
        return MyAdoptionRequestItem(
            request = this,
            animalName = listing?.name ?: "Animal no disponible",
            photoUrl = listing?.coverPhotoUrl.orEmpty(),
            centerName = organizationRepository.getOrganization(organizationId)?.name.orEmpty(),
        )
    }
}
