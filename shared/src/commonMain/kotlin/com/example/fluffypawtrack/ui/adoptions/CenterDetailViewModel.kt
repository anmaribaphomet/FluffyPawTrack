package com.example.fluffypawtrack.ui.adoptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.model.AdoptionListing
import com.example.fluffypawtrack.domain.model.ListingStatus
import com.example.fluffypawtrack.domain.model.Organization
import com.example.fluffypawtrack.domain.repository.AdoptionRepository
import com.example.fluffypawtrack.domain.repository.OrganizationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import com.example.fluffypawtrack.domain.model.coverPhotoUrl

/** Collapsed card of one available animal in the center detail (SC). */
data class AnimalCard(
    val listingId: String,
    val name: String,
    val species: String,
    val breed: String,
    val age: String,
    /** First of the listing's photos. */
    val coverPhotoUrl: String,
)

/**
 * Center detail: the center's info and its available animals. The adoption form is not
 * offered here; it only opens from "Quiero adoptarlo" on a specific animal (SC).
 */
data class CenterDetailUiState(
    val organization: Organization? = null,
    val listings: List<AdoptionListing> = emptyList(),
    val loaded: Boolean = false,
) {
    val animalCards: List<AnimalCard> = listings.map {
        AnimalCard(it.id, it.name, it.species, it.breed, it.age, it.coverPhotoUrl)
    }
}

class CenterDetailViewModel(
    organizationId: String,
    organizationRepository: OrganizationRepository,
    adoptionRepository: AdoptionRepository,
) : ViewModel() {

    val uiState: StateFlow<CenterDetailUiState> = combine(
        flow { emit(organizationRepository.getOrganization(organizationId)) },
        adoptionRepository.observeListingsBy(organizationId),
    ) { organization, listings ->
        CenterDetailUiState(
            organization = organization,
            listings = listings.filter { it.status == ListingStatus.Available },
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CenterDetailUiState())
}
