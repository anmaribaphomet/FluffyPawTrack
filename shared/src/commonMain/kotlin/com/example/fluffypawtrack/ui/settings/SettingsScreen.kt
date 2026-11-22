package com.example.fluffypawtrack.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.ui.account.AccountScaffold
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import com.example.fluffypawtrack.ui.theme.FluffySemanticColors
import com.example.fluffypawtrack.ui.theme.FluffyShadows
import com.example.fluffypawtrack.ui.theme.FluffyShapes
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.ic_arrow_down
import org.jetbrains.compose.resources.painterResource

/** Informational sub-screens opened from Ajustes. */
enum class SettingsTopic(val title: String) {
    Accessibility("Accesibilidad"),
    Permissions("Permisos"),
    Faq("Preguntas frecuentes"),
    Contact("Contáctanos"),
}

/** Ajustes (Figma 265:73). */
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onSignedOut: () -> Unit,
    onTopicClick: (SettingsTopic) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { SettingsViewModel(container.authRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.signedOut) {
        if (state.signedOut) onSignedOut()
    }

    SettingsScreen(
        state = state,
        onBack = onBack,
        onToggleDarkMode = viewModel::toggleDarkMode,
        onToggleNotifications = viewModel::toggleNotifications,
        onTopicClick = onTopicClick,
        onSignOut = viewModel::signOut,
        modifier = modifier,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onToggleNotifications: () -> Unit,
    onTopicClick: (SettingsTopic) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountScaffold(title = "Ajustes", onBack = onBack, modifier = modifier) {
        // Only Spanish is available for now, so the row is informational.
        SettingsRow("Idioma", Modifier) {
            Text(state.language, style = MaterialTheme.typography.bodyLarge, color = FluffyColors.Brown900)
        }
        ToggleRow("Modo oscuro", state.darkMode, onToggleDarkMode)
        ToggleRow("Notificaciones", state.notifications, onToggleNotifications)
        SettingsTopic.entries.forEach { topic ->
            SettingsRow(
                label = if (topic == SettingsTopic.Faq) "FAQ" else topic.title,
                modifier = Modifier.clickable(role = Role.Button) { onTopicClick(topic) },
            ) { Chevron() }
        }
        Spacer(Modifier.height(8.dp))
        SettingsRow(
            label = "Cerrar sesión",
            modifier = Modifier.clickable(role = Role.Button, onClick = onSignOut),
            color = FluffyColors.White,
        ) { Chevron() }
    }
}

@Composable
private fun SettingsRow(
    label: String,
    modifier: Modifier,
    color: Color = FluffySemanticColors.SurfaceSettings,
    trailing: @Composable RowScope.() -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .dropShadow(FluffyShapes.OptionRow, FluffyShadows.Drop)
            .clip(FluffyShapes.OptionRow)
            .background(color)
            .then(modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = FluffyColors.Brown900, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    SettingsRow(
        label = label,
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = { onToggle() }),
    ) {
        // The whole row is the toggle; the switch only shows its state.
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = FluffyColors.White,
                checkedTrackColor = FluffyColors.Sage700,
                checkedBorderColor = FluffyColors.Sage700,
                uncheckedThumbColor = FluffyColors.Sage700,
                uncheckedTrackColor = FluffyColors.Cream50,
                uncheckedBorderColor = FluffyColors.Sage700,
            ),
        )
    }
}

@Composable
private fun Chevron() {
    Image(
        painter = painterResource(Res.drawable.ic_arrow_down),
        contentDescription = null,
        modifier = Modifier.size(16.dp).rotate(-90f),
    )
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    FluffyPawTrackTheme {
        SettingsScreen(
            state = SettingsUiState(),
            onBack = {},
            onToggleDarkMode = {},
            onToggleNotifications = {},
            onTopicClick = {},
            onSignOut = {},
        )
    }
}
