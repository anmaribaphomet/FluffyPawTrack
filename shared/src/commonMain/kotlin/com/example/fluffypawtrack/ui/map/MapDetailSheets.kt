package com.example.fluffypawtrack.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fluffypawtrack.domain.model.AdoptionListing
import com.example.fluffypawtrack.domain.model.GeoPoint
import com.example.fluffypawtrack.domain.model.Priority
import com.example.fluffypawtrack.domain.model.ReportCategory
import com.example.fluffypawtrack.domain.model.ReportStatus
import com.example.fluffypawtrack.domain.model.Organization
import com.example.fluffypawtrack.domain.model.Report
import com.example.fluffypawtrack.ui.common.AppImage
import com.example.fluffypawtrack.ui.common.PinColors
import com.example.fluffypawtrack.ui.common.formatDateTime
import com.example.fluffypawtrack.ui.common.label
import com.example.fluffypawtrack.ui.common.pinColor
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import com.example.fluffypawtrack.ui.theme.FluffyShapes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Detail of a report (RF-02): photo, description, category, priority, status, date and address.
 * Reused outside the map; [actions] holds extra buttons such as "Es mi mascota".
 */
@OptIn(ExperimentalTime::class)
@Composable
fun ReportDetailSheetContent(
    report: Report,
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SheetPhoto(report.photoUrl, "Foto del reporte")
        SheetTitle(report.category.label, report.category.pinColor)
        Text(report.description, style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900)
        DetailRow("Prioridad", report.priority.label)
        DetailRow("Estado", report.status.label)
        DetailRow("Fecha", formatDateTime(report.createdAt))
        DetailRow("Dirección", report.address)
        report.reference?.let { DetailRow("Referencia", it) }
        actions()
    }
}

/** Detail of a support place: shelter, adoption center, vet, or safe point. */
@Composable
internal fun PlaceDetailSheetContent(
    organization: Organization,
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SheetPhoto(organization.photoUrl, "Foto de ${organization.name}")
        SheetTitle(organization.name, PinColors.SupportPlace)
        DetailRow("Tipo", organization.type.label)
        DetailRow("Dirección", organization.address)
        DetailRow("Horario", organization.schedule)
        DetailRow("Teléfono", organization.phone)
        actions()
    }
}

/** Detail of an animal available for adoption. */
@Composable
internal fun AdoptionDetailSheetContent(
    listing: AdoptionListing,
    organization: Organization,
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SheetPhoto(listing.photoUrls.first(), "Foto de ${listing.name}")
        SheetTitle(listing.name, PinColors.Adoption)
        DetailRow("Especie", listing.species)
        DetailRow("Raza", listing.breed)
        DetailRow("Edad", listing.age)
        DetailRow("Centro", organization.name)
        actions()
    }
}

@Composable
private fun SheetPhoto(ref: String, contentDescription: String) {
    AppImage(
        ref,
        contentDescription = contentDescription,
        modifier = Modifier.fillMaxWidth().height(160.dp).clip(FluffyShapes.Card),
    )
}

@Composable
private fun SheetTitle(text: String, pinColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 44.dp)) {
        Box(Modifier.size(14.dp).background(pinColor, FluffyShapes.Ellipse))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleLarge, color = FluffyColors.Brown900)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            text = label,
            modifier = Modifier.width(92.dp),
            style = MaterialTheme.typography.labelLarge,
            color = FluffyColors.Brown900,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = FluffyColors.Brown900,
        )
    }
}

@OptIn(ExperimentalTime::class)
@Preview
@Composable
private fun ReportDetailSheetContentPreview() {
    val report = Report(
        id = "preview",
        reporterId = null,
        category = ReportCategory.Injured,
        priority = Priority.High,
        description = "Perro con una pata trasera lastimada, cojea al caminar.",
        photoUrl = "res:sample_report_injured_dog",
        location = GeoPoint(29.1020, -110.9680),
        address = "Blvd. Luis Encinas y Reforma",
        reference = "Cerca de la parada del camión",
        status = ReportStatus.OnTheWay,
        assignedOrganizationId = null,
        createdAt = Instant.parse("2026-11-21T14:45:00Z"),
    )
    FluffyPawTrackTheme {
        Box(Modifier.background(FluffyColors.Cream50).padding(20.dp)) {
            ReportDetailSheetContent(report)
        }
    }
}
