package com.example.fluffypawtrack.ui.navigation

import kotlinx.serialization.Serializable

/** Type-safe navigation destinations. New screens add their route here. */
sealed interface Route {
    @Serializable
    data object Splash : Route

    @Serializable
    data object Login : Route

    @Serializable
    data object Register : Route

    @Serializable
    data object Home : Route

    @Serializable
    data object Profile : Route

    /** [adoptionsOnly] opens the map pre-filtered on adoption listings. */
    @Serializable
    data class Map(val adoptionsOnly: Boolean = false) : Route

    @Serializable
    data object Notifications : Route

    @Serializable
    data object NewReport : Route

    @Serializable
    data object PersonalInfo : Route

    @Serializable
    data object MyReports : Route

    @Serializable
    data object Favorites : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data class CenterDetail(val organizationId: String) : Route

    @Serializable
    data class AnimalDetail(val listingId: String) : Route

    /** The organization comes from the listing. */
    @Serializable
    data class AdoptionForm(val listingId: String) : Route

    /** Cuenta → "Mis solicitudes de adopción" (role `user`). */
    @Serializable
    data object MyAdoptionRequests : Route

    /** Cuenta → "Solicitudes de adopción" of the affiliate's center. */
    @Serializable
    data object CenterAdoptionRequests : Route

    @Serializable
    data class ContactRequestForm(val reportId: String) : Route

    @Serializable
    data object ContactRequests : Route

    @Serializable
    data object AffiliationRequest : Route

    /** [topic] is a `SettingsTopic` name. */
    @Serializable
    data class SettingsInfo(val topic: String) : Route

    /** Home of role `affiliate`. */
    @Serializable
    data object AffiliateHome : Route

    @Serializable
    data object ReportInbox : Route

    @Serializable
    data class CaseDetail(val reportId: String) : Route

    @Serializable
    data object MyCases : Route

    /** Contact requests opened on the "Recibidas" tab (affiliates). */
    @Serializable
    data object ReceivedContactRequests : Route

    @Serializable
    data object MyListings : Route

    /** A null [listingId] creates a new listing. */
    @Serializable
    data class ListingEditor(val listingId: String? = null) : Route

    @Serializable
    data object OrganizationProfile : Route

    @Serializable
    data object NotificationPreferences : Route

    /** Home of role `admin`. */
    @Serializable
    data object AdminHome : Route

    @Serializable
    data object AffiliationRequests : Route

    @Serializable
    data object PlacesDirectory : Route

    /** A null [organizationId] creates a new place. */
    @Serializable
    data class PlaceEditor(val organizationId: String? = null) : Route

    @Serializable
    data object ReportSupervision : Route

    @Serializable
    data class AdminReportDetail(val reportId: String) : Route
}
