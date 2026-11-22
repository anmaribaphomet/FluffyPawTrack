package com.example.fluffypawtrack.domain.model

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

enum class HousingType { House, Apartment }

enum class HousingTenure { Owned, Rented }

/** RF-06 states: `pending` → `approved`, `rejected` or `cancelled` (only while pending). */
enum class AdoptionRequestStatus { Pending, Approved, Rejected, Cancelled }

/** Pending and approved requests count toward the one-active-request-per-animal rule. */
val AdoptionRequestStatus.isActive: Boolean
    get() = this == AdoptionRequestStatus.Pending || this == AdoptionRequestStatus.Approved

/** A row of the `adoption_requests` table (REQUIREMENTS §7). */
@OptIn(ExperimentalTime::class)
data class AdoptionRequest(
    val id: String,
    val listingId: String,
    val organizationId: String,
    val requesterId: String,
    val fullName: String,
    val phone: String,
    val email: String,
    val housingType: HousingType,
    val housingTenure: HousingTenure,
    val hasOtherPets: Boolean,
    val otherPets: String?,
    val reason: String,
    val status: AdoptionRequestStatus,
    val rejectionReason: String?,
    val createdAt: Instant,
    val respondedAt: Instant?,
)

/** What the adoption form sends; the organization comes from the listing. */
data class NewAdoptionRequest(
    val listingId: String,
    val fullName: String,
    val phone: String,
    val email: String,
    val housingType: HousingType,
    val housingTenure: HousingTenure,
    val hasOtherPets: Boolean,
    val otherPets: String?,
    val reason: String,
)

/** User-facing texts of RF-06, shared by the repositories and the screens. */
object AdoptionRequestTexts {
    const val CONTACT_NOTICE =
        "Una vez aprobada tu solicitud, el centro se pondrá en contacto contigo mediante los datos de contacto que proporcionaste."

    const val APPROVED_TITLE = "Solicitud de adopción aprobada"
    const val REJECTED_TITLE = "Solicitud de adopción no aprobada"

    /** Rejection reason used when the center marks the animal as adopted. */
    const val ADOPTED_REASON = "El animal ya fue adoptado"

    fun approvedMessage(animalName: String): String =
        "¡Buenas noticias! El centro aprobó tu solicitud para adoptar a $animalName. " +
            "Pronto se pondrán en contacto contigo."

    fun rejectedMessage(animalName: String, reason: String): String =
        "Tu solicitud para adoptar a $animalName no fue aprobada esta vez. ¡Hay muchos más esperando un hogar!\n" +
            "Motivo: $reason"
}
