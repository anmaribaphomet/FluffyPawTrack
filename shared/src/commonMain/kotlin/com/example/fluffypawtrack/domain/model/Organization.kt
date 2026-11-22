package com.example.fluffypawtrack.domain.model

enum class OrganizationType { Shelter, AdoptionCenter, Vet, SafePoint }

enum class OrganizationStatus { Pending, Approved, Rejected }

/** A support place shown on the map: shelter, adoption center, vet, or safe point. */
data class Organization(
    val id: String,
    val type: OrganizationType,
    val name: String,
    val description: String,
    val location: GeoPoint,
    val address: String,
    val schedule: String,
    val phone: String,
    val email: String,
    val social: String?,
    /** Required photo of the place or its logo; see `ui/common/AppImage` for the reference formats. */
    val photoUrl: String,
    val status: OrganizationStatus,
    val active: Boolean,
)
