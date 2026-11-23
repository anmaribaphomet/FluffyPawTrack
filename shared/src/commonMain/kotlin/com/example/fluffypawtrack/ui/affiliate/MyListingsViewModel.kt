package com.example.fluffypawtrack.ui.affiliate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.model.AdoptionListing
import com.example.fluffypawtrack.domain.repository.AdoptionRepository
import com.example.fluffypawtrack.domain.repository.AuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MyListingsUiState(
    val listings: List<AdoptionListing> = emptyList(),
    val blockedMessage: String? = null,
    /** Readable repository failure. */
    val message: String? = null,
    val loaded: Boolean = false,
)

/** "Mis fichas de adopción": the center's animals, available and adopted. */
@OptIn(ExperimentalCoroutinesApi::class)
class MyListingsViewModel(
    private val authRepository: AuthRepository,
    private val adoptionRepository: AdoptionRepository,
) : ViewModel() {

    private val message = MutableStateFlow<String?>(null)

    private val listings = authRepository.currentUser.flatMapLatest { user ->
        val organizationId = user.affiliateOrganizationId
        if (organizationId == null) flowOf(null) else adoptionRepository.observeListingsBy(organizationId)
    }

    val uiState: StateFlow<MyListingsUiState> = combine(listings, message) { list, error ->
        MyListingsUiState(
            listings = list.orEmpty(),
            blockedMessage = AFFILIATE_ONLY_MESSAGE.takeIf { list == null },
            message = error,
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyListingsUiState())

    fun markAdopted(listingId: String) {
        val userId = authRepository.currentUser.value?.id ?: return
        viewModelScope.launch {
            adoptionRepository.markAdopted(listingId, userId)
                .onSuccess { message.value = null }
                .onFailure { message.value = it.message ?: "No se pudo actualizar el animal" }
        }
    }

    fun dismissMessage() {
        message.value = null
    }
}
