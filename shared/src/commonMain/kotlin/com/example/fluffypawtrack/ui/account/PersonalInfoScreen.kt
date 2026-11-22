package com.example.fluffypawtrack.ui.account

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme

/** "Información Personal", reached from the account screen. */
@Composable
fun PersonalInfoRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { PersonalInfoViewModel(container.authRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PersonalInfoScreen(state = state, onBack = onBack, modifier = modifier)
}

@Composable
fun PersonalInfoScreen(state: PersonalInfoUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    AccountScaffold(title = "Información Personal", onBack = onBack, modifier = modifier) {
        AccountCard(color = FluffyColors.Peach200) {
            LabeledValue("Nombre", state.firstName)
            LabeledValue("Apellidos", state.lastName)
        }
        AccountCard(color = FluffyColors.Peach200) {
            LabeledValue("Correo electrónico", state.email, emptyText = "Sin correo (sesión anónima)")
            LabeledValue("Teléfono", state.phone, emptyText = "Sin teléfono registrado")
        }
        Text(
            "Pronto podrás editar estos datos desde aquí.",
            style = MaterialTheme.typography.bodySmall,
            color = FluffyColors.Brown900,
        )
    }
}

@Preview
@Composable
private fun PersonalInfoScreenPreview() {
    FluffyPawTrackTheme {
        PersonalInfoScreen(
            state = PersonalInfoUiState("María", "Valenzuela", "demo@correo.com", "6620000001"),
            onBack = {},
        )
    }
}
