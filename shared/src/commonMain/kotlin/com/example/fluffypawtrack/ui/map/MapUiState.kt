package com.example.fluffypawtrack.ui.map

import androidx.compose.ui.graphics.Color
import com.example.fluffypawtrack.domain.model.AdoptionListing
import com.example.fluffypawtrack.domain.model.GeoPoint
import com.example.fluffypawtrack.domain.model.Organization
import com.example.fluffypawtrack.domain.model.ReportCategory
import com.example.fluffypawtrack.platform.MapPin
import com.example.fluffypawtrack.ui.common.PinColors
import com.example.fluffypawtrack.ui.common.label
import com.example.fluffypawtrack.ui.common.pinColor
import com.example.fluffypawtrack.domain.model.Report as DomainReport

/** Map filter options (RF-02): Todos, each report category, Adopciones, Lugares de apoyo. */
sealed interface MapFilter {
    val label: String

    /** Pin color shown next to the option; null for "Todos". */
    val color: Color?

    data object All : MapFilter {
        override val label = "Todos"
        override val color: Color? = null
    }

    data class Category(val category: ReportCategory) : MapFilter {
        override val label get() = category.label
        override val color: Color get() = category.pinColor
    }

    data object Adoptions : MapFilter {
        override val label = "Adopciones"
        override val color: Color = PinColors.Adoption
    }

    data object SupportPlaces : MapFilter {
        override val label = "Lugares de apoyo"
        override val color: Color = PinColors.SupportPlace
    }

    companion object {
        val options: List<MapFilter> =
            listOf(All) + ReportCategory.entries.map { Category(it) } + listOf(Adoptions, SupportPlaces)
    }
}

/** Something shown as a pin on the map. */
sealed interface MapItem {
    val id: String
    val location: GeoPoint
    val color: Color
    val title: String

    data class Report(val report: DomainReport) : MapItem {
        override val id get() = report.id
        override val location get() = report.location
        override val color get() = report.category.pinColor
        override val title get() = report.category.label
    }

    data class Place(val organization: Organization) : MapItem {
        override val id get() = organization.id
        override val location get() = organization.location
        override val color get() = PinColors.SupportPlace
        override val title get() = organization.name
    }

    /** An adoption listing, pinned next to its organization ([location] is slightly offset from it). */
    data class Adoption(
        val listing: AdoptionListing,
        val organization: Organization,
        override val location: GeoPoint,
    ) : MapItem {
        override val id get() = listing.id
        override val color get() = PinColors.Adoption
        override val title get() = listing.name
    }
}

data class MapUiState(
    val filter: MapFilter = MapFilter.All,
    val query: String = "",
    val items: List<MapItem> = emptyList(),
    val selected: MapItem? = null,
    val filterOptions: List<MapFilter> = MapFilter.options,
) {
    val pins: List<MapPin> = items.map { MapPin(it.id, it.location, it.color, it.title) }
}
