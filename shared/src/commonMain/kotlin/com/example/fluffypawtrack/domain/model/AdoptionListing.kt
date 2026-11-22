package com.example.fluffypawtrack.domain.model

enum class ListingStatus { Available, Adopted }

/** An animal an adoption center publishes. */
data class AdoptionListing(
    val id: String,
    val organizationId: String,
    val name: String,
    val species: String,
    val breed: String,
    val age: String,
    val vaccination: String,
    val description: String,
    /**
     * Photos of the animal; never empty (at least one is required). The first one is the cover
     * shown on cards. See `ui/common/AppImage` for the reference formats.
     */
    val photoUrls: List<String>,
    val status: ListingStatus,
)

/** First photo, used on cards; blank if a backend ever breaks the non-empty rule. */
val AdoptionListing.coverPhotoUrl: String
    get() = photoUrls.firstOrNull().orEmpty()
