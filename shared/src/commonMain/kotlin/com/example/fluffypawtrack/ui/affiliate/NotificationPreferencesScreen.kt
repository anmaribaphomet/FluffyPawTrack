package com.example.fluffypawtrack.ui.affiliate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.domain.model.Priority
import com.example.fluffypawtrack.ui.account.AccountCard
import com.example.fluffypawtrack.ui.account.AccountScaffold
import com.example.fluffypawtrack.ui.auth.AuthErrorMessage
import com.example.fluffypawtrack.ui.auth.AuthPrimaryButton
import com.example.fluffypawtrack.ui.common.label
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme

/** "Preferencias de notificación" of the affiliate (RF-03). */
@Composable
fun NotificationPreferencesRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = viewModel {
        NotificationPreferencesViewModel(container.authRepository, container.notificationPrefsRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    NotificationPreferencesScreen(
        state = state,
        onRadiusChange = viewModel::onRadiusChange,
        onTogglePriority = viewModel::togglePriority,
        onPushChange = viewModel::onPushChange,
        onSave = viewModel::onSave,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun NotificationPreferencesScreen(
    state: NotificationPreferencesUiState,
    onRadiusChange: (Float) -> Unit,
    onTogglePriority: (Priority) -> Unit,
    onPushChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountScaffold(title = "Preferencias de notificación", onBack = onBack, modifier = modifier) {
        AccountCard {
            Text("Radio: ${state.radiusKm} km", style = MaterialTheme.typography.bodyLarge, color = FluffyColors.Brown900)
            Slider(
                value = state.radiusKm.toFloat(),
                onValueChange = onRadiusChange,
                valueRange = MIN_RADIUS_KM.toFloat()..MAX_RADIUS_KM.toFloat(),
                steps = MAX_RADIUS_KM - MIN_RADIUS_KM - 1,
                modifier = Modifier.semantics { contentDescription = "Radio de avisos en kilómetros" },
                colors = SliderDefaults.colors(
                    thumbColor = FluffyColors.Orange700,
                    activeTrackColor = FluffyColors.Orange500,
                    inactiveTrackColor = FluffyColors.Cream100,
                    activeTickColor = FluffyColors.Brown900,
                    inactiveTickColor = FluffyColors.Brown600,
                ),
            )
            Text(
                "Recibirás avisos de reportes dentro de este radio.",
                style = MaterialTheme.typography.bodySmall,
                color = FluffyColors.Brown900,
            )
        }
        AccountCard {
            Text("Prioridades", style = MaterialTheme.typography.bodyLarge, color = FluffyColors.Brown900)
            SelectableChips(
                options = Priority.entries,
                isSelected = { it in state.priorities },
                label = { it.label },
                onSelect = onTogglePriority,
                role = Role.Checkbox,
            )
        }
        AccountCard {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Notificaciones push", style = MaterialTheme.typography.bodyLarge, color = FluffyColors.Brown900)
                Switch(
                    checked = state.pushEnabled,
                    onCheckedChange = onPushChange,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = FluffyColors.Sage700,
                        checkedThumbColor = FluffyColors.White,
                        uncheckedTrackColor = FluffyColors.Cream100,
                        uncheckedThumbColor = FluffyColors.Brown600,
                        uncheckedBorderColor = FluffyColors.Sage700,
                    ),
                )
            }
        }
        state.message?.let { AuthErrorMessage(it) }
        if (state.saved) {
            AccountCard(color = FluffyColors.Sage200) {
                Text("Preferencias guardadas.", style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900)
            }
        }
        AuthPrimaryButton(text = "Guardar", onClick = onSave, isLoading = state.isSaving)
    }
}

@Preview
@Composable
private fun NotificationPreferencesScreenPreview() {
    FluffyPawTrackTheme {
        NotificationPreferencesScreen(
            state = NotificationPreferencesUiState(loaded = true),
            onRadiusChange = {},
            onTogglePriority = {},
            onPushChange = {},
            onSave = {},
            onBack = {},
        )
    }
}
