package com.example.fluffypawtrack.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.fluffypawtrack.ui.components.ProfileHeader
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyShadows
import com.example.fluffypawtrack.ui.theme.FluffyShapes

/** Top padding that clears [ProfileHeader]. */
private val HeaderClearance = 158.dp

/**
 * Layout shared by the account and settings sub-screens: cream background, the profile
 * header with "Regresar", a centered title and a scrollable column of content.
 */
@Composable
internal fun AccountScaffold(
    title: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.background,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxSize().background(background)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = HeaderClearance, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = FluffyColors.Brown900,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().semantics { heading() },
                )
            }
            content()
        }
        ProfileHeader(onBack = onBack, modifier = Modifier.align(Alignment.TopCenter))
    }
}

/** Rounded surface with the standard drop shadow; text on it is brown-900. */
@Composable
internal fun AccountCard(
    modifier: Modifier = Modifier,
    color: Color = FluffyColors.White,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .dropShadow(FluffyShapes.Card, FluffyShadows.Drop)
            .background(color, FluffyShapes.Card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

/** Small label + value pair used inside cards. */
@Composable
internal fun LabeledValue(label: String, value: String?, emptyText: String = "Sin datos") {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
        if (value.isNullOrBlank()) {
            Text(emptyText, style = MaterialTheme.typography.bodySmall, color = FluffyColors.Brown900)
        } else {
            Text(value, style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900)
        }
    }
}
