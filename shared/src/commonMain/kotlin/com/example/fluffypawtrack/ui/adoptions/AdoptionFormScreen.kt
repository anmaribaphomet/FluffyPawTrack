package com.example.fluffypawtrack.ui.adoptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.domain.model.AdoptionRequestTexts
import com.example.fluffypawtrack.domain.model.HousingTenure
import com.example.fluffypawtrack.domain.model.HousingType
import com.example.fluffypawtrack.ui.account.AccountCard
import com.example.fluffypawtrack.ui.account.AccountScaffold
import com.example.fluffypawtrack.ui.auth.AuthErrorMessage
import com.example.fluffypawtrack.ui.auth.AuthPrimaryButton
import com.example.fluffypawtrack.ui.auth.AuthTextField
import com.example.fluffypawtrack.ui.common.label
import com.example.fluffypawtrack.ui.contact.MultilineField
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme

internal const val ADOPTION_SENT_MESSAGE =
    "¡Solicitud enviada! Puedes ver su estado en Cuenta → \"Mis solicitudes de adopción\"."

/** "Formulario de adopción" for one animal (Figma screen 6, RF-06). */
@Composable
fun AdoptionFormRoute(
    listingId: String,
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel(key = "adoption-form-$listingId") {
        AdoptionFormViewModel(
            listingId,
            container.authRepository,
            container.adoptionRepository,
            container.adoptionRequestRepository,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AdoptionFormScreen(
        state = state,
        actions = AdoptionFormActions(
            onFullNameChange = viewModel::onFullNameChange,
            onPhoneChange = viewModel::onPhoneChange,
            onEmailChange = viewModel::onEmailChange,
            onHousingTypeChange = viewModel::onHousingTypeChange,
            onHousingTenureChange = viewModel::onHousingTenureChange,
            onHasOtherPetsChange = viewModel::onHasOtherPetsChange,
            onOtherPetsChange = viewModel::onOtherPetsChange,
            onReasonChange = viewModel::onReasonChange,
            onSubmit = viewModel::submit,
        ),
        onBack = onBack,
        onDone = onDone,
        modifier = modifier,
    )
}

/** Form callbacks, grouped so the screen signature stays short. */
data class AdoptionFormActions(
    val onFullNameChange: (String) -> Unit = {},
    val onPhoneChange: (String) -> Unit = {},
    val onEmailChange: (String) -> Unit = {},
    val onHousingTypeChange: (HousingType) -> Unit = {},
    val onHousingTenureChange: (HousingTenure) -> Unit = {},
    val onHasOtherPetsChange: (Boolean) -> Unit = {},
    val onOtherPetsChange: (String) -> Unit = {},
    val onReasonChange: (String) -> Unit = {},
    val onSubmit: () -> Unit = {},
)

@Composable
fun AdoptionFormScreen(
    state: AdoptionFormUiState,
    actions: AdoptionFormActions,
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountScaffold(title = "Formulario de adopción", onBack = onBack, modifier = modifier) {
        val blocked = state.blockedMessage
        if (blocked != null) {
            AccountCard { Text(blocked, style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900) }
            return@AccountScaffold
        }
        AccountCard {
            LockedField(label = "Interesado en:", value = state.interestedIn)
            AuthTextField(
                value = state.fullName,
                onValueChange = actions.onFullNameChange,
                label = "Nombre completo",
                error = state.errors.fullName,
                capitalization = KeyboardCapitalization.Words,
            )
            AuthTextField(
                value = state.phone,
                onValueChange = actions.onPhoneChange,
                label = "Teléfono",
                error = state.errors.phone,
                keyboardType = KeyboardType.Phone,
            )
            AuthTextField(
                value = state.email,
                onValueChange = actions.onEmailChange,
                label = "Correo",
                error = state.errors.email,
                keyboardType = KeyboardType.Email,
            )
            ChoiceGroup(
                title = "Tipo de vivienda",
                options = HousingType.entries,
                selected = state.housingType,
                label = { it.label },
                onSelect = actions.onHousingTypeChange,
            )
            ChoiceGroup(
                title = "La vivienda es",
                options = HousingTenure.entries,
                selected = state.housingTenure,
                label = { it.label },
                onSelect = actions.onHousingTenureChange,
            )
            ChoiceGroup(
                title = "¿Tienes otras mascotas?",
                options = listOf(true, false),
                selected = state.hasOtherPets,
                label = { if (it) "Sí" else "No" },
                onSelect = actions.onHasOtherPetsChange,
            )
            if (state.hasOtherPets) {
                AuthTextField(
                    value = state.otherPets,
                    onValueChange = actions.onOtherPetsChange,
                    label = "¿Cuáles?",
                    error = state.errors.otherPets,
                    capitalization = KeyboardCapitalization.Sentences,
                )
            }
            MultilineField(
                value = state.reason,
                onValueChange = actions.onReasonChange,
                label = "¿Por qué quieres adoptarlo?",
                error = state.errors.reason,
                supportingText = "Obligatorio",
            )
        }
        state.submitError?.let { AuthErrorMessage(it) }
        Text(
            AdoptionRequestTexts.CONTACT_NOTICE,
            style = MaterialTheme.typography.bodySmall,
            color = FluffyColors.Brown900,
        )
        AuthPrimaryButton(text = "Enviar solicitud", onClick = actions.onSubmit, isLoading = state.isSubmitting)
    }
    if (state.submitted) {
        AlertDialog(
            onDismissRequest = onDone,
            confirmButton = {
                TextButton(onClick = onDone) { Text("Aceptar", color = FluffyColors.Brown900) }
            },
            text = { Text(ADOPTION_SENT_MESSAGE, color = FluffyColors.Brown900) },
        )
    }
}

/** Read-only field in the cream input style; screen readers announce it as not editable. */
@Composable
private fun LockedField(label: String, value: String) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        singleLine = true,
        label = { Text(label) },
        textStyle = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth().semantics { stateDescription = "No editable" },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = FluffyColors.Brown900,
            unfocusedTextColor = FluffyColors.Brown900,
            focusedContainerColor = FluffyColors.Neutral300,
            unfocusedContainerColor = FluffyColors.Neutral300,
            focusedBorderColor = FluffyColors.Sage700,
            unfocusedBorderColor = FluffyColors.Sage700,
            focusedLabelColor = FluffyColors.Brown900,
            unfocusedLabelColor = FluffyColors.Brown900,
        ),
    )
}

/** Single-choice radio row with a title. */
@Composable
private fun <T> ChoiceGroup(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
    Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        options.forEach { option ->
            Row(
                Modifier
                    .heightIn(min = 48.dp)
                    .selectable(selected = selected == option, onClick = { onSelect(option) }, role = Role.RadioButton),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selected == option,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = FluffyColors.Orange700,
                        unselectedColor = FluffyColors.Brown900,
                    ),
                )
                Text(label(option), style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900)
            }
        }
    }
}

@Preview
@Composable
private fun AdoptionFormScreenPreview() {
    FluffyPawTrackTheme {
        AdoptionFormScreen(
            state = AdoptionFormUiState(
                interestedIn = "Canela · Perro · Mestiza",
                fullName = "María Valenzuela",
                hasOtherPets = true,
            ),
            actions = AdoptionFormActions(),
            onBack = {},
            onDone = {},
        )
    }
}
