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

data class CenterDetailUiState(
    val organization: Organization? = null,
    val listings: List<AdoptionListing> = emptyList(),
    val loaded: Boolean = false,
)

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
