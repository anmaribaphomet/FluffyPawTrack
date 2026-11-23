package com.example.fluffypawtrack.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.fluffypawtrack.ui.auth.AuthErrorMessage
import com.example.fluffypawtrack.ui.contact.CardActionButton
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyShapes

private val ChipShape = RoundedCornerShape(50)

/** Small colored label such as a place status or the "Oculto" badge. */
@Composable
internal fun AdminChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.background(color, ChipShape).padding(horizontal = 12.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = FluffyColors.Brown900,
    )
}

/** Horizontally scrolling single-choice filter row. */
@Composable
internal fun <T> FilterRow(
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Text(
                text = labelOf(option),
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .clip(FluffyShapes.Pill)
                    .background(if (isSelected) FluffyColors.Orange500 else FluffyColors.Cream100)
                    .clickable(role = Role.RadioButton) { onSelect(option) }
                    .semantics { this.selected = isSelected }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelLarge,
                color = FluffyColors.Brown900,
            )
        }
    }
}

/** The error ([message]) or the result ([notice]) of the last action, with a button to dismiss it. */
@Composable
internal fun ResultBanner(message: String?, notice: String?, onDismiss: () -> Unit) {
    if (message == null && notice == null) return
    if (message != null) {
        AuthErrorMessage(message)
    } else if (notice != null) {
        Text(
            text = notice,
            modifier = Modifier
                .fillMaxWidth()
                .background(FluffyColors.Sage200, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodyMedium,
            color = FluffyColors.Brown900,
        )
    }
    CardActionButton(text = "Entendido", onClick = onDismiss, color = FluffyColors.Peach300)
}

/** Confirmation dialog for an admin action that changes what other people see. */
@Composable
internal fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = FluffyColors.Brown900) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText, color = FluffyColors.Brown900) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = FluffyColors.Brown900) } },
    )
}
