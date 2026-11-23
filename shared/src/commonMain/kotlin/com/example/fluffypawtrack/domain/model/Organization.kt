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
    /** How many animals the place can hold (RF-07); null when unknown or not applicable. */
    val capacity: Int? = null,
    /** Why the admin rejected the affiliation; null unless [status] is [OrganizationStatus.Rejected]. */
    val rejectionReason: String? = null,
)

/** What the admin enters to create or edit a place of the directory (RF-07). */
data class OrganizationDraft(
    val type: OrganizationType,
    val name: String,
    val description: String,
    val location: GeoPoint,
    val address: String,
    val schedule: String,
    val capacity: Int?,
    val phone: String,
    val email: String,
    val social: String?,
    /** Required photo or logo; a local photo is uploaded first. */
    val photoUrl: String,
)

/** The profile fields an affiliate may edit of their own organization. */
data class OrganizationProfile(
    val description: String,
    val schedule: String,
    val phone: String,
    val email: String,
    val social: String?,
    /** Required photo or logo; a local photo is uploaded first. */
    val photoUrl: String,
)

/** Validation shared by the fake and the Supabase organization repositories. */
object OrganizationRules {
    const val MISSING_PHOTO = "Agrega una foto del lugar o su logo"
    const val MISSING_NAME_OR_ADDRESS = "Completa el nombre y la dirección del lugar"
    const val NEGATIVE_CAPACITY = "La capacidad no puede ser negativa"
    const val MISSING_REJECTION_REASON = "Escribe el motivo del rechazo"
    const val REJECTION_REASON_TOO_LONG = "El motivo debe tener como máximo 500 caracteres"
    const val MAX_REJECTION_REASON_LENGTH = 500

    /** The readable reason [draft] cannot be saved, or null when it is valid. */
    fun problemOf(draft: OrganizationDraft): String? = when {
        draft.name.isBlank() || draft.address.isBlank() -> MISSING_NAME_OR_ADDRESS
        draft.photoUrl.isBlank() -> MISSING_PHOTO
        (draft.capacity ?: 0) < 0 -> NEGATIVE_CAPACITY
        else -> null
    }

    fun problemOf(profile: OrganizationProfile): String? =
        if (profile.photoUrl.isBlank()) MISSING_PHOTO else null

    /** The readable reason a review cannot be sent, or null; mirrors `review_affiliation`. */
    fun reviewProblemOf(approve: Boolean, reason: String?): String? {
        val trimmed = reason?.trim().orEmpty()
        return when {
            !approve && trimmed.isEmpty() -> MISSING_REJECTION_REASON
            trimmed.length > MAX_REJECTION_REASON_LENGTH -> REJECTION_REASON_TOO_LONG
            else -> null
        }
    }
}
