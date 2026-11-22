package com.example.fluffypawtrack.domain.model

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Latitude/longitude pair in decimal degrees. */
data class GeoPoint(val latitude: Double, val longitude: Double)

enum class ReportCategory {
    Street,
    PossiblyLost,
    Mistreated,
    Injured,
    Critical,
    Abandoned,
}

enum class Priority { Low, Medium, High, Urgent }

/** Rescue lifecycle: Pending → OnTheWay → Sheltered → Closed. */
enum class ReportStatus { Pending, OnTheWay, Sheltered, Closed }

@OptIn(ExperimentalTime::class)
data class Report(
    val id: String,
    /** Null when the report was created anonymously. */
    val reporterId: String?,
    val category: ReportCategory,
    val priority: Priority,
    val description: String,
    /** Required photo of the animal (RF-01); see `ui/common/AppImage` for the reference formats. */
    val photoUrl: String,
    val location: GeoPoint,
    val address: String,
    val reference: String?,
    val status: ReportStatus,
    val assignedOrganizationId: String?,
    val createdAt: Instant,
    /** Hidden by the admin (RF-07): off the map and the inboxes, still visible to the admin. */
    val hidden: Boolean = false,
)

/** Data the user fills in the new-report flow. */
data class NewReport(
    val category: ReportCategory,
    val priority: Priority,
    val description: String,
    /** Required: the flow cannot leave the photo step without it. */
    val photoUrl: String,
    val location: GeoPoint,
    val address: String,
    val reference: String?,
)
