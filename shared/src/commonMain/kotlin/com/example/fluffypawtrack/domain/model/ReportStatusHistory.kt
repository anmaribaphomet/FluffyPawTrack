package com.example.fluffypawtrack.domain.model

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** One row of the RF-04 status history: the status, when it was set and who is responsible. */
@OptIn(ExperimentalTime::class)
data class StatusHistoryEntry(
    val status: ReportStatus,
    val changedAt: Instant,
    val responsibleName: String,
)

/** RF-04 rules shared by the fake and the Supabase repositories (`take_report`, `set_report_status`). */
object ReportStatusRules {
    /** Shown for statuses set by the person who made the report, the initial one included. */
    const val REPORTER_RESPONSIBLE = "Quien hizo el reporte"

    /** Shown for changes on a report without an assigned organization (only the admin can make them). */
    const val ADMIN_RESPONSIBLE = "Administración"

    /** Title of the notification the reporter gets on each status change (trigger `reports_track_status`). */
    const val STATUS_CHANGED_TITLE = "Actualización de tu reporte"

    fun statusChangedMessage(status: ReportStatus): String {
        val label = when (status) {
            ReportStatus.Pending -> "Pendiente de atención"
            ReportStatus.OnTheWay -> "En camino"
            ReportStatus.Sheltered -> "Resguardado"
            ReportStatus.Closed -> "Caso cerrado"
        }
        return "Tu reporte ahora está en \"$label\"."
    }

    /** Note on the pending contact requests of a report that was hidden or deleted. */
    const val REPORT_UNAVAILABLE_NOTE = "El reporte ya no está disponible"

    /** An affiliate only moves a case forward: Pending → OnTheWay → Sheltered → Closed. */
    fun canAdvance(from: ReportStatus, to: ReportStatus): Boolean = to > from

    /**
     * The responsible shown in the history. Other people's profiles are private (RF-05), so a
     * change by anyone other than the reporter is attributed to the assigned organization.
     */
    fun responsibleName(
        status: ReportStatus,
        changedBy: String?,
        reporterId: String?,
        organizationName: String?,
    ): String = when {
        status == ReportStatus.Pending -> REPORTER_RESPONSIBLE
        changedBy != null && changedBy == reporterId -> REPORTER_RESPONSIBLE
        organizationName != null -> organizationName
        else -> ADMIN_RESPONSIBLE
    }
}
