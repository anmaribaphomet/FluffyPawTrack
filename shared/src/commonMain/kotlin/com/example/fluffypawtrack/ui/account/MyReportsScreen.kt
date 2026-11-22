package com.example.fluffypawtrack.ui.account

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.domain.model.Report
import com.example.fluffypawtrack.domain.model.ReportStatus
import com.example.fluffypawtrack.ui.common.formatDateTime
import com.example.fluffypawtrack.ui.common.label
import com.example.fluffypawtrack.ui.common.pinColor
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import com.example.fluffypawtrack.ui.theme.FluffySemanticColors
import com.example.fluffypawtrack.ui.theme.FluffyShadows
import com.example.fluffypawtrack.ui.theme.FluffyShapes
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.ic_arrow_down
import org.jetbrains.compose.resources.painterResource
import kotlin.time.ExperimentalTime

/** "Mis reportes": history list (Figma 204:524) and report detail (Figma 279:556). */
@Composable
fun MyReportsRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { MyReportsViewModel(container.authRepository, container.reportRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    MyReportsScreen(
        state = state,
        onBack = onBack,
        onSelect = viewModel::select,
        onDelete = viewModel::delete,
        onUpdate = viewModel::update,
        onDismissMessage = viewModel::dismissMessage,
        modifier = modifier,
    )
}

@Composable
fun MyReportsScreen(
    state: MyReportsUiState,
    onBack: () -> Unit,
    onSelect: (String?) -> Unit,
    onDelete: (String) -> Unit,
    onUpdate: (id: String, description: String, reference: String?) -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = state.selected
    val position = state.reports.indexOfFirst { it.id == selected?.id } + 1
    AccountScaffold(
        title = if (selected == null) "Mis reportes" else null,
        onBack = { if (selected != null) onSelect(null) else onBack() },
        modifier = modifier,
    ) {
        state.message?.let { MessageCard(it, onDismissMessage) }
        when {
            selected != null -> ReportDetail(selected, position, onDelete, onUpdate)
            state.loaded && state.reports.isEmpty() -> Text(
                "Aún no has publicado reportes. Cuando reportes un animal, podrás seguir su estado aquí.",
                style = MaterialTheme.typography.bodyMedium,
                color = FluffyColors.Brown900,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            else -> state.reports.forEachIndexed { index, report ->
                ReportRow(index + 1, report, onOpen = { onSelect(report.id) })
            }
        }
    }
}

@Composable
private fun MessageCard(message: String, onDismiss: () -> Unit) {
    AccountCard(color = FluffyColors.Peach100) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900)
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
            Text("Entendido", style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
        }
    }
}

/** Collapsed: number, category and status. Expanded: address, date and "Ver detalle". */
@OptIn(ExperimentalTime::class)
@Composable
private fun ReportRow(position: Int, report: Report, onOpen: () -> Unit) {
    var expanded by rememberSaveable(report.id) { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .dropShadow(FluffyShapes.OptionRow, FluffyShadows.Drop)
            .clip(FluffyShapes.OptionRow)
            .background(FluffyColors.Peach200)
            .clickable(role = Role.Button, onClickLabel = if (expanded) "Contraer" else "Expandir") {
                expanded = !expanded
            }
            .semantics { stateDescription = if (expanded) "Expandido" else "Contraído" }
            .animateContentSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(position.twoDigits(), style = MaterialTheme.typography.titleLarge, color = FluffyColors.Brown900)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                CategoryLine(report)
                StatusLine(report.status)
            }
            Image(
                painter = painterResource(Res.drawable.ic_arrow_down),
                contentDescription = null,
                modifier = Modifier.size(14.dp).rotate(if (expanded) 180f else 0f),
            )
        }
        if (expanded) {
            LabeledValue("Dirección", report.address)
            LabeledValue("Fecha", formatDateTime(report.createdAt))
            TextButton(onClick = onOpen, modifier = Modifier.align(Alignment.End)) {
                Text("Ver detalle", style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
            }
        }
    }
}

@Composable
private fun CategoryLine(report: Report) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(report.category.pinColor, FluffyShapes.Ellipse))
        Spacer(Modifier.width(6.dp))
        Text(report.category.label, style = MaterialTheme.typography.bodyLarge, color = FluffyColors.Brown900)
    }
}

@Composable
private fun StatusLine(status: ReportStatus) {
    Text(
        "Estado: ${status.label}",
        style = MaterialTheme.typography.bodyMedium,
        color = FluffyColors.Brown900,
    )
}

@OptIn(ExperimentalTime::class)
@Composable
private fun ReportDetail(
    report: Report,
    position: Int,
    onDelete: (String) -> Unit,
    onUpdate: (id: String, description: String, reference: String?) -> Unit,
) {
    var confirmDelete by remember(report.id) { mutableStateOf(false) }
    var editing by remember(report.id) { mutableStateOf(false) }
    val editable = report.status == ReportStatus.Pending

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(72.dp).background(FluffySemanticColors.SurfaceCard, FluffyShapes.Ellipse),
            contentAlignment = Alignment.Center,
        ) {
            Text(position.twoDigits(), style = MaterialTheme.typography.headlineSmall, color = FluffyColors.Brown900)
        }
        Spacer(Modifier.width(16.dp))
        Box(
            Modifier
                .weight(1f)
                .dropShadow(FluffyShapes.Pill, FluffyShadows.Drop)
                .background(FluffyColors.Peach200, FluffyShapes.Pill)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            CategoryLine(report)
        }
    }
    AccountCard {
        LabeledValue("Ubicación", report.address)
        LabeledValue("Referencia", report.reference, emptyText = "Sin referencia")
        LabeledValue("Fecha", formatDateTime(report.createdAt))
    }
    AccountCard(color = FluffyColors.Peach200) {
        LabeledValue("Estado", report.status.label)
    }
    AccountCard {
        LabeledValue("Comentarios / Descripción adicional", report.description, emptyText = "Sin comentarios")
    }
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(
            onClick = { editing = true },
            enabled = editable,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = FluffyColors.Peach200,
                contentColor = FluffyColors.Brown900,
                disabledContainerColor = FluffySemanticColors.Disabled,
                disabledContentColor = FluffyColors.Brown900,
            ),
        ) { Text("Editar", style = MaterialTheme.typography.bodyLarge) }
        Button(
            onClick = { confirmDelete = true },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = FluffySemanticColors.Accent,
                contentColor = FluffyColors.Brown900,
            ),
        ) { Text("Eliminar", style = MaterialTheme.typography.bodyLarge) }
    }
    if (!editable) {
        Text(
            "Solo puedes editar reportes pendientes de atención.",
            style = MaterialTheme.typography.bodySmall,
            color = FluffyColors.Brown900,
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Eliminar este reporte?") },
            text = { Text("El reporte desaparecerá del mapa y de tu historial. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete(report.id)
                }) { Text("Eliminar", color = FluffyColors.Brown900) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancelar", color = FluffyColors.Brown900) }
            },
        )
    }
    if (editing) {
        EditReportDialog(
            report = report,
            onDismiss = { editing = false },
            onSave = { description, reference ->
                editing = false
                onUpdate(report.id, description, reference)
            },
        )
    }
}

@Composable
private fun EditReportDialog(report: Report, onDismiss: () -> Unit, onSave: (String, String?) -> Unit) {
    var description by rememberSaveable { mutableStateOf(report.description) }
    var reference by rememberSaveable { mutableStateOf(report.reference.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar reporte") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    isError = description.isBlank(),
                    supportingText = if (description.isBlank()) {
                        { Text("Escribe una descripción") }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
                )
                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("Referencia (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(description, reference) }, enabled = description.isNotBlank()) {
                Text("Guardar", color = FluffyColors.Brown900)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = FluffyColors.Brown900) }
        },
    )
}

private fun Int.twoDigits(): String = toString().padStart(2, '0')

@Preview
@Composable
private fun MyReportsScreenPreview() {
    FluffyPawTrackTheme {
        MyReportsScreen(
            state = MyReportsUiState(reports = SampleData.reports.take(3), loaded = true),
            onBack = {},
            onSelect = {},
            onDelete = {},
            onUpdate = { _, _, _ -> },
            onDismissMessage = {},
        )
    }
}

@Preview
@Composable
private fun ReportDetailPreview() {
    val report = SampleData.reports.first()
    FluffyPawTrackTheme {
        MyReportsScreen(
            state = MyReportsUiState(reports = listOf(report), selectedId = report.id, loaded = true),
            onBack = {},
            onSelect = {},
            onDelete = {},
            onUpdate = { _, _, _ -> },
            onDismissMessage = {},
        )
    }
}
