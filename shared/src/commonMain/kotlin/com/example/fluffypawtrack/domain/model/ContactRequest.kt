package com.example.fluffypawtrack.domain.model

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** pending → accepted | rejected | cancelled. Final states never change. */
enum class ContactRequestStatus { Pending, Accepted, Rejected, Cancelled }

/**
 * "Es mi mascota" request on a possibly-lost report. Contact details are not part of
 * this model: they are fetched separately once the request is accepted.
 */
@OptIn(ExperimentalTime::class)
data class ContactRequest(
    val id: String,
    val reportId: String,
    val requesterId: String,
    val requesterFirstName: String,
    val recipientUserId: String?,
    val recipientOrganizationId: String?,
    val message: String,
    val photoUrl: String?,
    val status: ContactRequestStatus,
    val statusNote: String?,
    val createdAt: Instant,
)

val ContactRequestStatus.isActive: Boolean
    get() = this == ContactRequestStatus.Pending || this == ContactRequestStatus.Accepted

/** Fixed rules and user-facing notices of RF-05. */
object ContactRequestRules {
    const val MIN_MESSAGE_LENGTH = 20
    const val MAX_MESSAGE_LENGTH = 500

    const val ANONYMOUS_REPORT_NOTICE =
        "Este reporte es anónimo. Podrás enviar una solicitud cuando un albergue lo tome."

    /** Note on the pending requests rejected when the pet is delivered. */
    const val CASE_CLOSED_NOTE = "El caso se cerró"

    fun isValidMessage(message: String): Boolean =
        message.trim().length in MIN_MESSAGE_LENGTH..MAX_MESSAGE_LENGTH
}

/** Who decides a contact request on a report (RF-05 "Receptor"). */
sealed interface ContactRecipient {
    data class User(val userId: String) : ContactRecipient
    data class Organization(val organizationId: String) : ContactRecipient
}

/**
 * The assigned organization if there is one, otherwise the reporter. Anonymous reports
 * without an organization have no recipient.
 */
val Report.contactRecipient: ContactRecipient?
    get() = when {
        assignedOrganizationId != null -> ContactRecipient.Organization(assignedOrganizationId)
        reporterId != null -> ContactRecipient.User(reporterId)
        else -> null
    }
