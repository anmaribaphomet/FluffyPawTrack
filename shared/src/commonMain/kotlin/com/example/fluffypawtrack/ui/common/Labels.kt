package com.example.fluffypawtrack.ui.common

import androidx.compose.ui.graphics.Color
import com.example.fluffypawtrack.domain.model.ContactRequestStatus
import com.example.fluffypawtrack.domain.model.OrganizationType
import com.example.fluffypawtrack.domain.model.Priority
import com.example.fluffypawtrack.domain.model.ReportCategory
import com.example.fluffypawtrack.domain.model.ReportStatus
import com.example.fluffypawtrack.ui.theme.FluffyColors
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// Single source for user-facing names and pin colors of domain values.
// Every screen uses these instead of writing its own strings.

val ReportCategory.label: String
    get() = when (this) {
        ReportCategory.Street -> "Situación de calle"
        ReportCategory.PossiblyLost -> "Posible mascota extraviada"
        ReportCategory.Mistreated -> "Maltratado"
        ReportCategory.Injured -> "Herido o lesionado"
        ReportCategory.Critical -> "Estado crítico"
        ReportCategory.Abandoned -> "Abandono"
    }

val Priority.label: String
    get() = when (this) {
        Priority.Low -> "Baja"
        Priority.Medium -> "Media"
        Priority.High -> "Alta"
        Priority.Urgent -> "Urgente"
    }

val ReportStatus.label: String
    get() = when (this) {
        ReportStatus.Pending -> "Pendiente de atención"
        ReportStatus.OnTheWay -> "En camino"
        ReportStatus.Sheltered -> "Resguardado"
        ReportStatus.Closed -> "Caso cerrado"
    }

val OrganizationType.label: String
    get() = when (this) {
        OrganizationType.Shelter -> "Albergue"
        OrganizationType.AdoptionCenter -> "Centro de adopción"
        OrganizationType.Vet -> "Veterinaria aliada"
        OrganizationType.SafePoint -> "Punto seguro"
    }

val ContactRequestStatus.label: String
    get() = when (this) {
        ContactRequestStatus.Pending -> "Pendiente"
        ContactRequestStatus.Accepted -> "Aceptada"
        ContactRequestStatus.Rejected -> "Rechazada"
        ContactRequestStatus.Cancelled -> "Cancelada"
    }

/** Map pin colors, docs/design-system.md §1.4. */
object PinColors {
    val Adoption = FluffyColors.Pink400
    val SupportPlace = FluffyColors.Brown900
}

val ReportCategory.pinColor: Color
    get() = when (this) {
        ReportCategory.Street -> FluffyColors.Orange500
        ReportCategory.PossiblyLost -> Color(0xFF3E7CB1)
        ReportCategory.Mistreated -> Color(0xFF8E6CB0)
        ReportCategory.Injured -> Color(0xFFE53935)
        ReportCategory.Critical -> Color(0xFF7F0000)
        ReportCategory.Abandoned -> FluffyColors.Sage700
    }

private val MonthNames = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/** Hermosillo is UTC−7 all year (no daylight saving time). */
private const val HermosilloOffsetSeconds = -7 * 3600L

/** "21 nov 2026, 11:20" in Hermosillo time. */
@OptIn(ExperimentalTime::class)
fun formatDateTime(instant: Instant): String {
    val local = instant.epochSeconds + HermosilloOffsetSeconds
    val days = local.floorDiv(86_400L)
    val secondsOfDay = local.mod(86_400L)
    val (year, month, day) = civilFromDays(days)
    val hour = secondsOfDay / 3600
    val minute = (secondsOfDay % 3600) / 60
    return "$day ${MonthNames[month - 1]} $year, $hour:${minute.toString().padStart(2, '0')}"
}

/** Days since 1970-01-01 → (year, month 1..12, day). Howard Hinnant's civil_from_days. */
private fun civilFromDays(days: Long): Triple<Int, Int, Int> {
    val z = days + 719_468
    val era = z.floorDiv(146_097L)
    val doe = z - era * 146_097
    val yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val day = (doy - (153 * mp + 2) / 5 + 1).toInt()
    val month = (if (mp < 10) mp + 3 else mp - 9).toInt()
    val year = (yoe + era * 400 + if (month <= 2) 1 else 0).toInt()
    return Triple(year, month, day)
}
