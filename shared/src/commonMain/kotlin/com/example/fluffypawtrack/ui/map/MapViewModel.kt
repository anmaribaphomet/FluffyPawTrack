package com.example.fluffypawtrack.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.model.GeoPoint
import com.example.fluffypawtrack.domain.repository.AdoptionRepository
import com.example.fluffypawtrack.domain.repository.OrganizationRepository
import com.example.fluffypawtrack.domain.repository.ReportRepository
import com.example.fluffypawtrack.ui.common.label
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
        val byId = organizations.associateBy { it.id }
        val adoptions = listings
            .filter { it.organizationId in byId }
            .groupBy { it.organizationId }
            .flatMap { (organizationId, group) ->
                val organization = byId.getValue(organizationId)
                group.mapIndexed { index, listing ->
                    MapItem.Adoption(listing, organization, aroundPoint(organization.location, index, group.size))
                }
            }
        reports.map { MapItem.Report(it) } + organizations.map { MapItem.Place(it) } + adoptions
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
    MapFilter.Adoptions -> this is MapItem.Adoption
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
    is MapItem.Adoption -> listOf(listing.name, listing.species, listing.breed, organization.name, organization.address)
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

// Listings share their organization's coordinates; spread them on a small ring (~80 m)
// so each pin can be tapped and none hides the organization's own pin.
private const val AdoptionRingDegrees = 0.0008

private fun aroundPoint(center: GeoPoint, index: Int, count: Int): GeoPoint {
    val angle = 2 * PI * index / count
    return GeoPoint(
        latitude = center.latitude + AdoptionRingDegrees * sin(angle),
        longitude = center.longitude + AdoptionRingDegrees * cos(angle),
    )
}
