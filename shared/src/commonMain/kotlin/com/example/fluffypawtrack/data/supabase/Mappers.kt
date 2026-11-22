package com.example.fluffypawtrack.data.supabase

import com.example.fluffypawtrack.domain.model.AdoptionListing
import com.example.fluffypawtrack.domain.model.AdoptionRequest
import com.example.fluffypawtrack.domain.model.AppNotification
import com.example.fluffypawtrack.domain.model.ContactDetails
import com.example.fluffypawtrack.domain.model.ContactRequest
import com.example.fluffypawtrack.domain.model.GeoPoint
import com.example.fluffypawtrack.domain.model.NewAdoptionRequest
import com.example.fluffypawtrack.domain.model.NewListing
import com.example.fluffypawtrack.domain.model.NewReport
import com.example.fluffypawtrack.domain.model.Organization
import com.example.fluffypawtrack.domain.model.OrganizationDraft
import com.example.fluffypawtrack.domain.model.OrganizationProfile
import com.example.fluffypawtrack.domain.model.OrganizationStatus
import com.example.fluffypawtrack.domain.model.Report
import com.example.fluffypawtrack.domain.model.ReportStatus
import com.example.fluffypawtrack.domain.model.ReportStatusRules
import com.example.fluffypawtrack.domain.model.Role
import com.example.fluffypawtrack.domain.model.StatusHistoryEntry
import com.example.fluffypawtrack.domain.model.UserProfile
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// Explicit DTO <-> domain conversions; enums go through EnumMapping.kt.

@OptIn(ExperimentalTime::class)
private fun timestamp(value: String): Instant = Instant.parse(value)

internal fun ProfileDto.toDomain(email: String?, isAnonymous: Boolean): UserProfile = UserProfile(
    id = id,
    email = email,
    firstName = firstName,
    lastName = lastName,
    phone = phone,
    role = fromDb<Role>(role),
    organizationId = organizationId,
    isAnonymous = isAnonymous,
)

/** The signed-in user; a session whose profile row could not be read is a plain `user`. */
internal fun userProfileOf(id: String, email: String?, isAnonymous: Boolean, profile: ProfileDto?): UserProfile =
    (profile ?: ProfileDto(id = id)).toDomain(email = email, isAnonymous = isAnonymous)

internal fun OrganizationDto.toDomain(): Organization = Organization(
    id = id,
    type = fromDb(type),
    name = name,
    description = description,
    location = GeoPoint(lat, lng),
    address = address,
    schedule = schedule,
    phone = phone,
    email = email,
    social = social,
    photoUrl = photoUrl,
    status = fromDb(status),
    active = active,
    capacity = capacity,
)

/** [photoUrl] is the stored photo (already uploaded when the form held a local one). */
internal fun OrganizationDraft.toWriteDto(photoUrl: String): OrganizationWriteDto = OrganizationWriteDto(
    type = type.dbValue,
    name = name.trim(),
    description = description.trim(),
    photoUrl = photoUrl,
    lat = location.latitude,
    lng = location.longitude,
    address = address.trim(),
    schedule = schedule.trim(),
    capacity = capacity,
    phone = phone.trim(),
    email = email.trim(),
    social = social?.trim()?.takeIf { it.isNotEmpty() },
)

/** An admin-created place is approved and active, so it appears on the map right away. */
internal fun OrganizationWriteDto.asNewApproved(): NewOrganizationDto = NewOrganizationDto(
    type = type,
    name = name,
    description = description,
    photoUrl = photoUrl,
    lat = lat,
    lng = lng,
    address = address,
    schedule = schedule,
    capacity = capacity,
    phone = phone,
    email = email,
    social = social,
    status = OrganizationStatus.Approved.dbValue,
    active = true,
)

internal fun OrganizationProfile.toDto(photoUrl: String): OrganizationProfileDto = OrganizationProfileDto(
    description = description.trim(),
    schedule = schedule.trim(),
    phone = phone.trim(),
    email = email.trim(),
    social = social?.trim()?.takeIf { it.isNotEmpty() },
    photoUrl = photoUrl,
)

@OptIn(ExperimentalTime::class)
internal fun ReportDto.toDomain(): Report = Report(
    id = id,
    // README open question 1: anonymous sessions still store their uid, but the app treats
    // those reports as having no reporter (no contact recipient, RF-05).
    reporterId = reporterId.takeUnless { reporterIsAnonymous },
    category = fromDb(category),
    priority = fromDb(priority),
    description = description,
    photoUrl = photoUrl,
    location = GeoPoint(lat, lng),
    // The Geocoder may fail; the coordinates are the only address then.
    address = address?.takeIf { it.isNotBlank() } ?: "$lat, $lng",
    reference = reference,
    status = fromDb(status),
    assignedOrganizationId = assignedOrgId,
    createdAt = timestamp(createdAt),
    hidden = hidden,
)

/**
 * History rows of [report] (its raw row, so anonymous reporters still match their own changes),
 * oldest first. [organizationName] is the assigned organization's name, if any.
 */
@OptIn(ExperimentalTime::class)
internal fun statusHistoryOf(
    rows: List<StatusHistoryDto>,
    report: ReportDto,
    organizationName: String?,
): List<StatusHistoryEntry> = rows.map { row ->
    val status = fromDb<ReportStatus>(row.status)
    StatusHistoryEntry(
        status = status,
        changedAt = timestamp(row.createdAt),
        responsibleName = ReportStatusRules.responsibleName(status, row.changedBy, report.reporterId, organizationName),
    )
}.sortedBy { it.changedAt }

/** [photoUrl] is the stored photo (already uploaded when the form held a local one). */
internal fun NewReport.toInsertDto(reporterId: String, photoUrl: String): NewReportDto = NewReportDto(
    reporterId = reporterId,
    category = category.dbValue,
    priority = priority.dbValue,
    description = description,
    photoUrl = photoUrl,
    lat = location.latitude,
    lng = location.longitude,
    address = address.takeIf { it.isNotBlank() },
    reference = reference,
)

internal fun ListingDto.toDomain(): AdoptionListing = AdoptionListing(
    id = id,
    organizationId = organizationId,
    name = name,
    species = species,
    breed = breed,
    age = age,
    vaccination = vaccination,
    description = description,
    photoUrls = photoUrls,
    status = fromDb(status),
)

/** [photoUrls] are the stored photos (already uploaded when the form held local ones). */
internal fun NewListing.toInsertDto(organizationId: String, photoUrls: List<String>): NewListingDto = NewListingDto(
    organizationId = organizationId,
    name = name.trim(),
    species = species.trim(),
    breed = breed.trim(),
    age = age.trim(),
    vaccination = vaccination.trim(),
    description = description.trim(),
    photoUrls = photoUrls,
)

internal fun NewListing.toUpdateDto(photoUrls: List<String>): ListingUpdateDto = ListingUpdateDto(
    name = name.trim(),
    species = species.trim(),
    breed = breed.trim(),
    age = age.trim(),
    vaccination = vaccination.trim(),
    description = description.trim(),
    photoUrls = photoUrls,
)

@OptIn(ExperimentalTime::class)
internal fun AdoptionRequestDto.toDomain(): AdoptionRequest = AdoptionRequest(
    id = id,
    listingId = listingId,
    organizationId = organizationId,
    requesterId = requesterId,
    fullName = fullName,
    phone = phone,
    email = email,
    housingType = fromDb(housingType),
    housingTenure = fromDb(housingTenure),
    hasOtherPets = hasOtherPets,
    otherPets = otherPets,
    reason = reason,
    status = fromDb(status),
    rejectionReason = rejectionReason,
    createdAt = timestamp(createdAt),
    respondedAt = respondedAt?.let(::timestamp),
)

/** Parameters of the `submit_adoption_request` RPC (supabase/README.md). */
internal fun NewAdoptionRequest.toSubmitParams(): JsonObject = buildJsonObject {
    put("p_listing_id", listingId)
    put("p_full_name", fullName)
    put("p_phone", phone)
    put("p_email", email)
    put("p_housing_type", housingType.dbValue)
    put("p_housing_tenure", housingTenure.dbValue)
    put("p_has_other_pets", hasOtherPets)
    put("p_other_pets", otherPets)
    put("p_reason", reason)
}

@OptIn(ExperimentalTime::class)
internal fun ContactRequestDto.toDomain(): ContactRequest = ContactRequest(
    id = id,
    reportId = reportId.orEmpty(),
    requesterId = requesterId,
    requesterFirstName = requesterFirstName,
    recipientUserId = recipientUserId,
    recipientOrganizationId = recipientOrgId,
    message = message,
    photoUrl = photoUrl,
    status = fromDb(status),
    statusNote = statusNote,
    createdAt = timestamp(createdAt),
)

@OptIn(ExperimentalTime::class)
internal fun NotificationDto.toDomain(): AppNotification = AppNotification(
    id = id,
    title = title,
    message = body,
    receivedAt = timestamp(createdAt),
    type = fromDb(type),
    isRead = read,
)

internal fun ContactDetailsDto.toDomain(): ContactDetails = ContactDetails(fullName, phone, email)
