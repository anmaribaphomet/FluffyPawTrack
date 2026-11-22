package com.example.fluffypawtrack.ui.contact

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.fluffypawtrack.domain.model.ContactRequestStatus
import com.example.fluffypawtrack.domain.model.Report
import com.example.fluffypawtrack.ui.common.AppImage
import com.example.fluffypawtrack.ui.common.label
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyShapes

private val FieldShape = RoundedCornerShape(12.dp)
private val ChipShape = RoundedCornerShape(50)

/** Status colors of a contact request; the text on them is brown-900. */
internal val ContactRequestStatus.color: Color
    get() = when (this) {
        ContactRequestStatus.Pending -> FluffyColors.Peach300
        ContactRequestStatus.Accepted -> FluffyColors.Sage400
        ContactRequestStatus.Rejected -> FluffyColors.Pink400
        ContactRequestStatus.Cancelled -> FluffyColors.Neutral300
    }

@Composable
internal fun StatusChip(status: ContactRequestStatus, modifier: Modifier = Modifier) {
    Text(
        text = status.label,
        modifier = modifier
            .background(status.color, ChipShape)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = FluffyColors.Brown900,
    )
}

/** Report photo, or a neutral tile when the report no longer exists. */
@Composable
internal fun ReportThumbnail(report: Report?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    if (report != null) {
        AppImage(report.photoUrl, contentDescription = "Foto del reporte", modifier = modifier.size(56.dp).clip(shape))
    } else {
        Box(modifier.size(56.dp).background(FluffyColors.Neutral300, shape))
    }
}

/** Multi-line text field in the cream input style of the auth forms. */
@Composable
internal fun MultilineField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    error: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .semantics { if (error != null) error(error) },
        isError = error != null,
        minLines = 4,
        textStyle = MaterialTheme.typography.bodyMedium,
        supportingText = (error ?: supportingText)?.let { { Text(it, style = MaterialTheme.typography.labelSmall) } },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        shape = FieldShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = FluffyColors.Brown900,
            unfocusedTextColor = FluffyColors.Brown900,
            errorTextColor = FluffyColors.Brown900,
            focusedContainerColor = FluffyColors.Cream100,
            unfocusedContainerColor = FluffyColors.Cream100,
            errorContainerColor = FluffyColors.Cream100,
            cursorColor = FluffyColors.Brown900,
            focusedBorderColor = FluffyColors.Orange700,
            unfocusedBorderColor = FluffyColors.Sage700,
            errorBorderColor = FluffyColors.Orange700,
            focusedLabelColor = FluffyColors.Brown900,
            unfocusedLabelColor = FluffyColors.Brown900,
            errorLabelColor = FluffyColors.Brown900,
            focusedSupportingTextColor = FluffyColors.Brown900,
            unfocusedSupportingTextColor = FluffyColors.Brown900,
            errorSupportingTextColor = FluffyColors.Brown900,
        ),
    )
}

/** Compact pill button for card actions. */
@Composable
internal fun CardActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = FluffyColors.Orange500,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = FluffyShapes.Pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = FluffyColors.Brown900,
            disabledContainerColor = FluffyColors.Neutral300,
            disabledContentColor = FluffyColors.Brown900,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
