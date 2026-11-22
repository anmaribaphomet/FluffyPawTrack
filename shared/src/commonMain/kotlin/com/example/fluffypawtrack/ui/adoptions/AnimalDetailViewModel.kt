package com.example.fluffypawtrack.ui.adoptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.model.AdoptionListing
import com.example.fluffypawtrack.domain.repository.AdoptionRepository
import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.domain.repository.FavoritesRepository
import com.example.fluffypawtrack.domain.repository.OrganizationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** REQUIREMENTS §4: favorites need a real account; anonymous sessions only report. */
internal const val ACCOUNT_REQUIRED_MESSAGE =
    "Necesitas una cuenta para guardar favoritos. Inicia sesión o regístrate para hacerlo."

data class AnimalDetailUiState(
    val listing: AdoptionListing? = null,
    val organizationId: String? = null,
    val centerName: String? = null,
    val isFavorite: Boolean = false,
    val accountMessage: String? = null,
    val loaded: Boolean = false,
)

private data class ListingWithCenter(val listing: AdoptionListing?, val centerName: String?)

@OptIn(ExperimentalCoroutinesApi::class)
class AnimalDetailViewModel(
    private val listingId: String,
    adoptionRepository: AdoptionRepository,
    organizationRepository: OrganizationRepository,
    private val authRepository: AuthRepository,
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {

    private val accountMessage = MutableStateFlow<String?>(null)

    private val listingWithCenter = flow {
        val listing = adoptionRepository.getListing(listingId)
        val center = listing?.let { organizationRepository.getOrganization(it.organizationId) }
        emit(ListingWithCenter(listing, center?.name))
    }

    private val isFavorite = authRepository.currentUser.flatMapLatest { user ->
        if (user == null || user.isAnonymous) {
            flowOf(false)
        } else {
            favoritesRepository.observeFavoriteIds(user.id).map { listingId in it }
        }
    }

    val uiState: StateFlow<AnimalDetailUiState> =
        combine(listingWithCenter, isFavorite, accountMessage) { data, favorite, message ->
            AnimalDetailUiState(
                listing = data.listing,
                organizationId = data.listing?.organizationId,
                centerName = data.centerName,
                isFavorite = favorite,
                accountMessage = message,
                loaded = true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnimalDetailUiState())

    fun toggleFavorite() {
        val user = authRepository.currentUser.value
        if (user == null || user.isAnonymous) {
            accountMessage.value = ACCOUNT_REQUIRED_MESSAGE
            return
        }
        viewModelScope.launch { favoritesRepository.toggleFavorite(user.id, listingId) }
    }

    fun dismissAccountMessage() {
        accountMessage.value = null
    }
}
