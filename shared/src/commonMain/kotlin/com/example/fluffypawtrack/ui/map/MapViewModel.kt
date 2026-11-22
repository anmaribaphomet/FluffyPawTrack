package com.example.fluffypawtrack.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.model.OrganizationType
import com.example.fluffypawtrack.domain.repository.AdoptionRepository
import com.example.fluffypawtrack.domain.repository.OrganizationRepository
import com.example.fluffypawtrack.domain.repository.ReportRepository
import com.example.fluffypawtrack.ui.common.label
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class MapViewModel(
    reportRepository: ReportRepository,
    organizationRepository: OrganizationRepository,
    adoptionRepository: AdoptionRepository,
    initialFilter: MapFilter = MapFilter.All,
) : ViewModel() {

    private val filter = MutableStateFlow(initialFilter)
    private val query = MutableStateFlow("")
    private val selectedId = MutableStateFlow<String?>(null)

    private val allItems = combine(
        reportRepository.observeActiveReports(),
        organizationRepository.observeActiveOrganizations(),
        adoptionRepository.observeAvailableListings(),
    ) { reports, organizations, listings ->
        // One pin per place. Adoption centers with available animals get the adoption pin;
        // their animals are never pinned on their own (SC).
        val listingsByOrganization = listings.groupBy { it.organizationId }
        val places = organizations.map { organization ->
            val available = listingsByOrganization[organization.id].orEmpty()
            if (organization.type == OrganizationType.AdoptionCenter && available.isNotEmpty()) {
                MapItem.AdoptionCenter(organization, available)
            } else {
                MapItem.Place(organization)
            }
        }
        reports.map { MapItem.Report(it) } + places
    }

    val uiState: StateFlow<MapUiState> = combine(allItems, filter, query, selectedId) { items, filter, query, selectedId ->
        MapUiState(
            filter = filter,
            query = query,
            items = items.filter { it.matches(filter) && it.matches(query) },
            selected = selectedId?.let { id -> items.firstOrNull { it.id == id } },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MapUiState(filter = initialFilter))

    fun onFilterSelected(filter: MapFilter) {
        this.filter.value = filter
    }

    fun onQueryChange(query: String) {
        this.query.value = query
    }

    fun onPinClick(id: String) {
        selectedId.value = id
    }

    fun onDismissDetail() {
        selectedId.value = null
    }
}

private fun MapItem.matches(filter: MapFilter): Boolean = when (filter) {
    MapFilter.All -> true
    is MapFilter.Category -> this is MapItem.Report && report.category == filter.category
    MapFilter.Adoptions -> this is MapItem.AdoptionCenter
    MapFilter.SupportPlaces -> this is MapItem.Place
}

private fun MapItem.matches(query: String): Boolean {
    val needle = query.normalizedForSearch()
    if (needle.isEmpty()) return true
    return searchText().normalizedForSearch().contains(needle)
}

private fun MapItem.searchText(): String = when (this) {
    is MapItem.Report -> listOf(report.description, report.address, report.reference.orEmpty(), report.category.label)
    is MapItem.Place -> listOf(organization.name, organization.address, organization.type.label, organization.description)
    is MapItem.AdoptionCenter ->
        listOf(organization.name, organization.address, organization.type.label, organization.description) +
            listings.flatMap { listOf(it.name, it.species, it.breed) }
}.joinToString(" ")

/** Lowercase, trimmed, and without Spanish accents, so "suarez" finds "Suárez". */
private fun String.normalizedForSearch(): String = trim().lowercase().map { char ->
    when (char) {
        'á', 'à', 'ä' -> 'a'
        'é', 'è', 'ë' -> 'e'
        'í', 'ì', 'ï' -> 'i'
        'ó', 'ò', 'ö' -> 'o'
        'ú', 'ù', 'ü' -> 'u'
        else -> char
    }
}.joinToString("")
