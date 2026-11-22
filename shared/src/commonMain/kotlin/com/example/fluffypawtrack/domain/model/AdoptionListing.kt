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

/** What an adoption center enters to publish or edit an animal (RF-06). */
data class NewListing(
    val name: String,
    val species: String,
    val breed: String,
    val age: String,
    val vaccination: String,
    val description: String,
    /** At least one; local photos are uploaded first. The first one is the cover. */
    val photoUrls: List<String>,
)

/** RF-06 listing validation, shared by the fake and the Supabase repositories. */
object ListingRules {
    const val MISSING_FIELDS = "Completa todos los datos del animal"
    const val MISSING_PHOTO = "Agrega al menos una foto del animal"

    fun problemOf(listing: NewListing): String? = when {
        listOf(listing.name, listing.species, listing.breed, listing.age, listing.vaccination, listing.description)
            .any { it.isBlank() } -> MISSING_FIELDS
        listing.photoUrls.none { it.isNotBlank() } -> MISSING_PHOTO
        else -> null
    }
}

/** First photo, used on cards; blank if a backend ever breaks the non-empty rule. */
val AdoptionListing.coverPhotoUrl: String
    get() = photoUrls.firstOrNull().orEmpty()
