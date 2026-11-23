package com.example.fluffypawtrack.ui.affiliate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.fluffypawtrack.domain.model.Report
import com.example.fluffypawtrack.domain.model.Role as UserRole
import com.example.fluffypawtrack.domain.model.UserProfile
import com.example.fluffypawtrack.ui.account.AccountCard
import com.example.fluffypawtrack.ui.common.AppImage
import com.example.fluffypawtrack.ui.common.color
import com.example.fluffypawtrack.ui.common.formatDateTime
import com.example.fluffypawtrack.ui.common.label
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyShapes
import kotlin.time.ExperimentalTime

internal const val AFFILIATE_ONLY_MESSAGE = "Esta sección es solo para cuentas de organizaciones afiliadas."

/** The organization of an affiliate account, or null for any other account. */
internal val UserProfile?.affiliateOrganizationId: String?
    get() = this?.organizationId?.takeIf { role == UserRole.Affiliate }

private val ChipShape = RoundedCornerShape(50)

/** Small colored label; the text on it is brown-900. */
@Composable
internal fun LabelChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.background(color, ChipShape).padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = FluffyColors.Brown900,
        maxLines = 1,
    )
}

/** Inbox and "Mis casos" row: photo, category, priority, status, date and address. */
@OptIn(ExperimentalTime::class)
@Composable
internal fun ReportListCard(report: Report, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AccountCard(modifier.clip(FluffyShapes.Card).clickable(role = Role.Button, onClick = onClick)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            AppImage(
                report.photoUrl,
                contentDescription = "Foto del reporte",
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(report.category.label, style = MaterialTheme.typography.bodyLarge, color = FluffyColors.Brown900)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LabelChip(report.priority.label, report.priority.color)
                    LabelChip(report.status.label, report.status.color)
                }
                Text(formatDateTime(report.createdAt), style = MaterialTheme.typography.bodySmall, color = FluffyColors.Brown900)
                Text(
                    report.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = FluffyColors.Brown900,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Horizontally scrolling single- or multi-select chips; selected chips are orange. */
@Composable
internal fun <T> SelectableChips(
    options: List<T>,
    isSelected: (T) -> Boolean,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    role: Role = Role.RadioButton,
) {
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val selected = isSelected(option)
            Text(
                text = label(option),
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .clip(FluffyShapes.Pill)
                    .background(if (selected) FluffyColors.Orange500 else FluffyColors.Cream100)
                    .clickable(role = role) { onSelect(option) }
                    .semantics { this.selected = selected }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelLarge,
                color = FluffyColors.Brown900,
            )
        }
    }
}

/** Plain message card used for empty and blocked states. */
@Composable
internal fun InfoCard(text: String) {
    AccountCard { Text(text, style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900) }
}
