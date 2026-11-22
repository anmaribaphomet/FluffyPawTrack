package com.example.fluffypawtrack.ui.affiliation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.domain.model.OrganizationType
import com.example.fluffypawtrack.ui.account.AccountCard
import com.example.fluffypawtrack.ui.account.AccountScaffold
import com.example.fluffypawtrack.ui.auth.AuthErrorMessage
import com.example.fluffypawtrack.ui.auth.AuthPrimaryButton
import com.example.fluffypawtrack.ui.auth.AuthTextField
import com.example.fluffypawtrack.ui.common.label
import com.example.fluffypawtrack.ui.contact.MultilineField
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyShapes

/** Affiliation request (REQUIREMENTS §6 "Común"). [onDone] closes the confirmation. */
@Composable
fun AffiliationRequestRoute(onBack: () -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { AffiliationRequestViewModel(container.organizationRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AffiliationRequestScreen(
        state = state,
        onBack = onBack,
        onDone = onDone,
        onNameChange = viewModel::onNameChange,
        onTypeChange = viewModel::onTypeChange,
        onAddressChange = viewModel::onAddressChange,
        onPhoneChange = viewModel::onPhoneChange,
        onEmailChange = viewModel::onEmailChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onSubmit = viewModel::onSubmit,
        modifier = modifier,
    )
}

@Composable
fun AffiliationRequestScreen(
    state: AffiliationRequestUiState,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onNameChange: (String) -> Unit,
    onTypeChange: (OrganizationType) -> Unit,
    onAddressChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountScaffold(title = "Solicitud de afiliación", onBack = onBack, modifier = modifier) {
        val submitted = state.submitted
        if (submitted != null) {
            AccountCard(color = FluffyColors.Sage200) {
                Text("¡Solicitud enviada!", style = MaterialTheme.typography.titleMedium, color = FluffyColors.Brown900)
                Text(
                    "El administrador revisará la solicitud de ${submitted.name}. " +
                        "Cuando la apruebe, tu organización aparecerá en el mapa y podrás atender reportes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FluffyColors.Brown900,
                )
            }
            AuthPrimaryButton(text = "Listo", onClick = onDone)
            return@AccountScaffold
        }

        Text(
            "¿Eres un albergue, rescatista o centro de adopción? Envía tus datos y el administrador " +
                "revisará tu solicitud.",
            style = MaterialTheme.typography.bodyMedium,
            color = FluffyColors.Brown900,
        )

        AuthTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = "Nombre de la organización",
            error = state.errors.name,
            capitalization = KeyboardCapitalization.Words,
        )

        Text("Tipo de organización", style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
        TypeSelector(options = state.typeOptions, selected = state.type, onSelect = onTypeChange)
        Text(
            "Si eres rescatista independiente, elige ${OrganizationType.Shelter.label}.",
            style = MaterialTheme.typography.bodySmall,
            color = FluffyColors.Brown900,
        )

        AuthTextField(
            value = state.address,
            onValueChange = onAddressChange,
            label = "Dirección",
            error = state.errors.address,
            capitalization = KeyboardCapitalization.Sentences,
        )
        AuthTextField(
            value = state.phone,
            onValueChange = onPhoneChange,
            label = "Teléfono",
            error = state.errors.phone,
            keyboardType = KeyboardType.Phone,
        )
        AuthTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = "Correo",
            error = state.errors.email,
            keyboardType = KeyboardType.Email,
        )
        MultilineField(
            value = state.description,
            onValueChange = onDescriptionChange,
            label = "Descripción",
            error = state.errors.description,
            supportingText = "A qué se dedican, qué animales reciben y su horario",
        )

        state.message?.let { AuthErrorMessage(it) }

        AuthPrimaryButton(text = "Enviar solicitud", onClick = onSubmit, isLoading = state.isSubmitting)
    }
}

@Composable
private fun TypeSelector(
    options: List<OrganizationType>,
    selected: OrganizationType,
    onSelect: (OrganizationType) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { type ->
            val isSelected = type == selected
            Text(
                text = type.label,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(FluffyShapes.Pill)
                    .background(if (isSelected) FluffyColors.Orange500 else FluffyColors.Cream100)
                    .clickable(role = Role.RadioButton) { onSelect(type) }
                    .semantics { this.selected = isSelected }
                    .padding(vertical = 14.dp, horizontal = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = FluffyColors.Brown900,
                textAlign = TextAlign.Center,
            )
        }
    }
}
