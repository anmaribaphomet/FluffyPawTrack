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
