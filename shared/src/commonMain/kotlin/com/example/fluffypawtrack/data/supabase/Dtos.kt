package com.example.fluffypawtrack.data.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Rows of the tables in supabase/migrations/20261122000100_schema.sql. Enums and timestamps
// stay as the strings PostgREST sends; Mappers.kt converts them to the domain types.

@Serializable
internal data class ProfileDto(
    val id: String,
    @SerialName("first_name") val firstName: String = "",
    @SerialName("last_name") val lastName: String = "",
    val phone: String? = null,
    val role: String = "user",
    @SerialName("organization_id") val organizationId: String? = null,
)

@Serializable
internal data class OrganizationDto(
    val id: String,
    val type: String,
    val name: String,
    val description: String = "",
    @SerialName("photo_url") val photoUrl: String,
    val lat: Double,
    val lng: Double,
    val address: String = "",
    val schedule: String = "",
    val phone: String = "",
    val email: String = "",
    val social: String? = null,
    val status: String,
    val active: Boolean,
    val capacity: Int? = null,
)

/** Columns the admin writes when creating or editing a place; status and visibility are separate. */
@Serializable
internal data class OrganizationWriteDto(
    val type: String,
    val name: String,
    val description: String,
    @SerialName("photo_url") val photoUrl: String,
    val lat: Double,
    val lng: Double,
    val address: String,
    val schedule: String,
    val capacity: Int?,
    val phone: String,
    val email: String,
    val social: String?,
)

/** Insert body of an admin-created place: approved and active from the start. */
@Serializable
internal data class NewOrganizationDto(
    val type: String,
    val name: String,
    val description: String,
    @SerialName("photo_url") val photoUrl: String,
    val lat: Double,
    val lng: Double,
    val address: String,
    val schedule: String,
    val capacity: Int?,
    val phone: String,
    val email: String,
    val social: String?,
    val status: String,
    val active: Boolean,
)

/** The columns `organizations_guard_update` lets an affiliate change. */
@Serializable
internal data class OrganizationProfileDto(
    val description: String,
    val schedule: String,
    val phone: String,
    val email: String,
    val social: String?,
    @SerialName("photo_url") val photoUrl: String,
)

@Serializable
internal data class ReportDto(
    val id: String,
    @SerialName("reporter_id") val reporterId: String? = null,
    @SerialName("reporter_is_anonymous") val reporterIsAnonymous: Boolean = false,
    val category: String,
    val priority: String,
    val description: String = "",
    @SerialName("photo_url") val photoUrl: String,
    val lat: Double,
    val lng: Double,
    val address: String? = null,
    val reference: String? = null,
    val status: String,
    @SerialName("assigned_org_id") val assignedOrgId: String? = null,
    @SerialName("created_at") val createdAt: String,
    val hidden: Boolean = false,
)

/** A row of `report_status_history`, written only by the `reports_track_status` trigger. */
@Serializable
internal data class StatusHistoryDto(
    val status: String,
    @SerialName("changed_by") val changedBy: String? = null,
    @SerialName("created_at") val createdAt: String,
)

/** Insert body of `reports`; status, flags and timestamps come from column defaults and triggers. */
@Serializable
internal data class NewReportDto(
    @SerialName("reporter_id") val reporterId: String,
    val category: String,
    val priority: String,
    val description: String,
    @SerialName("photo_url") val photoUrl: String,
    val lat: Double,
    val lng: Double,
    val address: String?,
    val reference: String?,
)

@Serializable
internal data class ListingDto(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    val name: String,
    val species: String,
    val breed: String,
    val age: String,
    val vaccination: String,
    val description: String,
    @SerialName("photo_urls") val photoUrls: List<String>,
    val status: String,
)

/** Insert body of `adoption_listings`; status and timestamps come from column defaults. */
@Serializable
internal data class NewListingDto(
    @SerialName("organization_id") val organizationId: String,
    val name: String,
    val species: String,
    val breed: String,
    val age: String,
    val vaccination: String,
    val description: String,
    @SerialName("photo_urls") val photoUrls: List<String>,
)

/** The columns an affiliate edits on a listing; the organization and status stay. */
@Serializable
internal data class ListingUpdateDto(
    val name: String,
    val species: String,
    val breed: String,
    val age: String,
    val vaccination: String,
    val description: String,
    @SerialName("photo_urls") val photoUrls: List<String>,
)

@Serializable
internal data class AdoptionRequestDto(
    val id: String,
    @SerialName("listing_id") val listingId: String,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("requester_id") val requesterId: String,
    @SerialName("full_name") val fullName: String,
    val phone: String,
    val email: String,
    @SerialName("housing_type") val housingType: String,
    @SerialName("housing_tenure") val housingTenure: String,
    @SerialName("has_other_pets") val hasOtherPets: Boolean,
    @SerialName("other_pets") val otherPets: String? = null,
    val reason: String,
    val status: String,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("responded_at") val respondedAt: String? = null,
)

@Serializable
internal data class ContactRequestDto(
    val id: String,
    /** Null once the report was deleted. */
    @SerialName("report_id") val reportId: String? = null,
    @SerialName("requester_id") val requesterId: String,
    @SerialName("requester_first_name") val requesterFirstName: String = "",
    @SerialName("recipient_user_id") val recipientUserId: String? = null,
    @SerialName("recipient_org_id") val recipientOrgId: String? = null,
    val message: String,
    @SerialName("photo_url") val photoUrl: String? = null,
    val status: String,
    @SerialName("status_note") val statusNote: String? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
internal data class NotificationDto(
    val id: String,
    val type: String = "general",
    val title: String,
    val body: String,
    val read: Boolean = false,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
internal data class FavoriteDto(
    @SerialName("user_id") val userId: String,
    @SerialName("listing_id") val listingId: String,
)

/** One row of the `get_contact_details` RPC. */
@Serializable
internal data class ContactDetailsDto(
    @SerialName("full_name") val fullName: String,
    val phone: String,
    val email: String,
)
