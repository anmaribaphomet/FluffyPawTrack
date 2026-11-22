package com.example.fluffypawtrack.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.model.AdoptionListing
import com.example.fluffypawtrack.domain.repository.AdoptionRepository
import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.domain.repository.FavoritesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val listings: List<AdoptionListing> = emptyList(),
    val loaded: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModel(
    private val authRepository: AuthRepository,
    private val favoritesRepository: FavoritesRepository,
    private val adoptionRepository: AdoptionRepository,
) : ViewModel() {

    val uiState: StateFlow<FavoritesUiState> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptySet()) else favoritesRepository.observeFavoriteIds(user.id)
        }
        .mapLatest { ids ->
            // Listings removed by their shelter simply drop out of the list.
            FavoritesUiState(listings = ids.mapNotNull { adoptionRepository.getListing(it) }, loaded = true)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavoritesUiState())

    fun remove(listingId: String) {
        val userId = authRepository.currentUser.value?.id ?: return
        if (uiState.value.listings.none { it.id == listingId }) return
        viewModelScope.launch { favoritesRepository.toggleFavorite(userId, listingId) }
    }
}
