package com.example.fluffypawtrack.ui.report

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.fluffypawtrack.domain.model.Priority
import com.example.fluffypawtrack.domain.model.ReportCategory
import com.example.fluffypawtrack.ui.common.AppImage
import com.example.fluffypawtrack.ui.common.label
import com.example.fluffypawtrack.ui.components.PillActionButton
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffySemanticColors
import com.example.fluffypawtrack.ui.theme.FluffyShadows
import com.example.fluffypawtrack.ui.theme.FluffyShapes
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.ic_add
import fluffypawtrack.shared.generated.resources.ic_camera
import fluffypawtrack.shared.generated.resources.ic_map
import org.jetbrains.compose.resources.painterResource

// Step bodies of the new-report flow. Each one is stateless and only shows NewReportUiState.

private val CategoryPillHeight = 64.dp
private val CategoryKnobSize = 48.dp

/** Category pills (Figma component sets 158:39 / 158:59): the cream knob slides right when selected. */
@Composable
internal fun CategoryStep(selected: ReportCategory?, onSelect: (ReportCategory) -> Unit) {
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ReportCategory.entries.forEach { category ->
            CategoryPill(category = category, selected = category == selected, onClick = { onSelect(category) })
        }
    }
}

@Composable
private fun CategoryPill(category: ReportCategory, selected: Boolean, onClick: () -> Unit) {
    val gradient = if (selected) {
        Brush.horizontalGradient(listOf(FluffyColors.Orange500, FluffyColors.Peach300))
    } else {
        Brush.horizontalGradient(listOf(FluffyColors.Peach100, FluffyColors.Cream100))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = CategoryPillHeight)
            .dropShadow(FluffyShapes.NavBar, FluffyShadows.Drop)
            .clip(FluffyShapes.NavBar)
            .background(gradient)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!selected) Knob()
        Text(
            text = category.label,
            style = MaterialTheme.typography.bodyLarge,
            color = FluffyColors.Brown900,
            modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
        )
        if (selected) Knob()
    }
}

@Composable
private fun Knob() {
    Box(
        Modifier
            .size(CategoryKnobSize)
            .dropShadow(FluffyShapes.Ellipse, FluffyShadows.Drop)
            .background(FluffyColors.Cream50, FluffyShapes.Ellipse),
    )
}

/** Photo frame with camera and gallery buttons (Figma "Seccion Fotografia", 101:183). */
@Composable
internal fun PhotoStep(photoRef: String?, onTakePhoto: () -> Unit, onPickPhoto: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .dropShadow(FluffyShapes.Card, FluffyShadows.Drop)
                .clip(FluffyShapes.Card)
                .background(FluffySemanticColors.Disabled),
            contentAlignment = Alignment.Center,
        ) {
            if (photoRef != null) {
                AppImage(photoRef, contentDescription = "Foto del animal", modifier = Modifier.fillMaxSize())
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(Res.drawable.ic_camera), contentDescription = null, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    // The photo is required (RF-01): "Siguiente" stays disabled until there is one.
                    Text(
                        "Agrega una foto para continuar",
                        style = MaterialTheme.typography.bodySmall,
                        color = FluffyColors.Brown900,
                    )
                }
            }
        }
        StepPill {
            PillActionButton(
                text = if (photoRef == null) "Tomar foto" else "Tomar otra foto",
                icon = painterResource(Res.drawable.ic_camera),
                onClick = onTakePhoto,
            )
        }
        StepPill {
            PillActionButton(text = "Elegir de la galería", icon = painterResource(Res.drawable.ic_add), onClick = onPickPhoto)
        }
    }
}

/** Pill buttons are designed on a colored band; this gives them that band on the cream background. */
@Composable
private fun StepPill(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().background(FluffyColors.Orange500, FluffyShapes.Pill).padding(8.dp)) { content() }
}

/** Current location card plus optional reference (Figma "Ubicacion" 164:87 and "ubic final" 174:279). */
@Composable
internal fun LocationStep(state: NewReportUiState, actions: NewReportActions) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .dropShadow(FluffyShapes.Card, FluffyShadows.Drop)
                .background(FluffyColors.Peach200, FluffyShapes.Card)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(Res.drawable.ic_map), contentDescription = null, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                when (state.locationStatus) {
                    LocationStatus.Idle, LocationStatus.Locating -> {
                        Text("Buscando tu ubicación…", style = MaterialTheme.typography.bodyLarge, color = FluffyColors.Brown900)
                        Spacer(Modifier.height(8.dp))
                        CircularProgressIndicator(color = FluffyColors.Brown900, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    }
                    LocationStatus.Found -> {
                        Text("Tu ubicación actual", style = MaterialTheme.typography.bodyLarge, color = FluffyColors.Brown900)
                        Text(state.address, style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900)
                    }
                    LocationStatus.Manual -> {
                        Text("No pudimos obtener tu ubicación", style = MaterialTheme.typography.bodyLarge, color = FluffyColors.Brown900)
                        Text(
                            "Escribe la dirección donde está el animal o vuelve a intentarlo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FluffyColors.Brown900,
                        )
                    }
                }
            }
        }
        if (state.locationStatus == LocationStatus.Manual) {
            ReportTextField(
                label = "Dirección",
                value = state.address,
                onValueChange = actions.onAddressChange,
                placeholder = "Calle, número y colonia",
            )
            StepPill {
                PillActionButton(text = "Usar mi ubicación", icon = painterResource(Res.drawable.ic_map), onClick = actions.onRequestLocation)
            }
        }
        ReportTextField(
            label = "Referencia adicional (opcional)",
            value = state.reference,
            onValueChange = actions.onReferenceChange,
            placeholder = "Ej. frente a la tienda, junto al parque",
            minLines = 4,
        )
    }
}

/** Description and priority (RF-01 points 4 and 5). */
@Composable
internal fun DetailsStep(state: NewReportUiState, actions: NewReportActions) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ReportTextField(
            label = "Descripción",
            value = state.description,
            onValueChange = actions.onDescriptionChange,
            placeholder = "Ej. Perrito café con collar azul, cojea de una pata",
            minLines = 4,
            supportingText = "${state.description.length}/${NewReportViewModel.MaxDescriptionLength}",
        )
        Text("Prioridad", style = MaterialTheme.typography.bodyLarge, color = FluffyColors.Brown900)
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Priority.entries.forEach { priority ->
                PriorityChip(
                    priority = priority,
                    selected = priority == state.priority,
                    enabled = !state.isPriorityLocked,
                    onClick = { actions.onSelectPriority(priority) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text(
            text = if (state.isPriorityLocked) {
                "Los animales en estado crítico siempre tienen prioridad urgente."
            } else {
                "Sugerimos una prioridad según la categoría. Puedes cambiarla."
            },
            style = MaterialTheme.typography.bodySmall,
            color = FluffyColors.Brown900,
        )
    }
}

@Composable
private fun PriorityChip(priority: Priority, selected: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val background = when {
        selected -> FluffyColors.Orange500
        enabled -> FluffyColors.White
        else -> FluffySemanticColors.Disabled
    }
    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(FluffyShapes.Pill)
            .background(background)
            .border(1.dp, if (selected) FluffyColors.Brown900 else FluffyColors.Sage700, FluffyShapes.Pill)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(priority.label, style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900, maxLines = 1)
    }
}

/** Everything the user entered before "Subir Reporte" (Figma "prefinal", 164:208). */
@Composable
internal fun SummaryStep(state: NewReportUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .dropShadow(FluffyShapes.Card, FluffyShadows.Drop)
            .background(FluffyColors.White, FluffyShapes.Card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.photoRef?.let { ref ->
            AppImage(
                ref,
                contentDescription = "Foto del animal",
                modifier = Modifier.fillMaxWidth().height(180.dp).clip(FluffyShapes.Card),
            )
        }
        SummaryRow("Categoría", state.category?.label.orEmpty())
        SummaryRow("Prioridad", state.priority?.label.orEmpty())
        SummaryRow("Descripción", state.description.trim())
        SummaryRow("Ubicación", state.address.trim())
        if (state.reference.isNotBlank()) SummaryRow("Referencia", state.reference.trim())
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900)
    }
}

@Composable
private fun ReportTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minLines: Int = 1,
    supportingText: String? = null,
) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = FluffyColors.Brown900)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium,
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
            supportingText = supportingText?.let { { Text(it, style = MaterialTheme.typography.labelSmall) } },
            minLines = minLines,
            shape = FluffyShapes.Card,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = FluffyColors.Cream100,
                unfocusedContainerColor = FluffyColors.Cream100,
                focusedBorderColor = FluffyColors.Sage700,
                unfocusedBorderColor = FluffyColors.Cream100,
                focusedTextColor = FluffyColors.Brown900,
                unfocusedTextColor = FluffyColors.Brown900,
                cursorColor = FluffyColors.Brown900,
                focusedPlaceholderColor = FluffyColors.Brown900,
                unfocusedPlaceholderColor = FluffyColors.Brown900,
                focusedSupportingTextColor = FluffyColors.Brown900,
                unfocusedSupportingTextColor = FluffyColors.Brown900,
            ),
        )
    }
}
