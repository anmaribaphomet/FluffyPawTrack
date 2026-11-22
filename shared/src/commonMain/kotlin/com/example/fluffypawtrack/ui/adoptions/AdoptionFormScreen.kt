package com.example.fluffypawtrack.ui.adoptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.ui.account.AccountCard
import com.example.fluffypawtrack.ui.account.AccountScaffold
import com.example.fluffypawtrack.ui.auth.AuthPrimaryButton
import com.example.fluffypawtrack.ui.auth.AuthTextField
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme

internal const val ADOPTION_SENT_MESSAGE = "¡Solicitud enviada! El centro se pondrá en contacto contigo."

/** "Formulario de adopción" for a center, optionally about one animal (Figma screen 6). */
@Composable
fun AdoptionFormRoute(
    organizationId: String,
    listingId: String?,
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel(key = "adoption-form-$organizationId-$listingId") {
        AdoptionFormViewModel(
            organizationId,
            listingId,
            container.authRepository,
            container.organizationRepository,
            container.adoptionRepository,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AdoptionFormScreen(
        state = state,
        onBack = onBack,
        onNameChange = viewModel::onNameChange,
        onPhoneChange = viewModel::onPhoneChange,
        onEmailChange = viewModel::onEmailChange,
        onHousingChange = viewModel::onHousingChange,
        onHasOtherPetsChange = viewModel::onHasOtherPetsChange,
        onMessageChange = viewModel::onMessageChange,
        onSubmit = viewModel::submit,
        onDone = onDone,
        modifier = modifier,
    )
}

@Composable
fun AdoptionFormScreen(
    state: AdoptionFormUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onHousingChange: (Housing) -> Unit,
    onHasOtherPetsChange: (Boolean) -> Unit,
    onMessageChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountScaffold(title = "Formulario de adopción", onBack = onBack, modifier = modifier) {
        val subject = listOfNotNull(state.listingName, state.centerName).joinToString(" · ")
        if (subject.isNotEmpty()) {
            Text(
                subject,
                style = MaterialTheme.typography.labelLarge,
                color = FluffyColors.Brown900,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        AccountCard {
            AuthTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = "Nombre completo",
                error = state.errors.name,
                capitalization = KeyboardCapitalization.Words,
            )
            AuthTextField(
                value = state.phone,
                onValueChange = onPhoneChange,
                label = "Teléfono",
                error = state.errors.contact,
                keyboardType = KeyboardType.Phone,
            )
            AuthTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = "Correo",
                helperText = "Indica al menos un teléfono o un correo",
                keyboardType = KeyboardType.Email,
            )
            Text("Tipo de vivienda", style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
            Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Housing.entries.forEach { option ->
                    Row(
                        Modifier
                            .heightIn(min = 48.dp)
                            .selectable(
                                selected = state.housing == option,
                                onClick = { onHousingChange(option) },
                                role = Role.RadioButton,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = state.housing == option,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = FluffyColors.Orange700,
                                unselectedColor = FluffyColors.Brown900,
                            ),
                        )
                        Text(option.label, style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900)
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .toggleable(value = state.hasOtherPets, onValueChange = onHasOtherPetsChange, role = Role.Checkbox),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = state.hasOtherPets,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(
                        checkedColor = FluffyColors.Orange700,
                        uncheckedColor = FluffyColors.Brown900,
                        checkmarkColor = FluffyColors.Brown900,
                    ),
                )
                Text("Tengo otras mascotas", style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900)
            }
            MessageField(value = state.message, onValueChange = onMessageChange, error = state.errors.message)
        }
        AuthPrimaryButton(text = "Enviar solicitud", onClick = onSubmit)
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

/** Multi-line version of the cream input style used by AuthTextField. */
@Composable
private fun MessageField(value: String, onValueChange: (String) -> Unit, error: String?) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("¿Por qué quieres adoptar?") },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .semantics { if (error != null) error(error) },
        isError = error != null,
        textStyle = MaterialTheme.typography.bodyMedium,
        supportingText = {
            Text(
                error ?: "Mínimo $MIN_ADOPTION_MESSAGE_LENGTH caracteres",
                style = MaterialTheme.typography.labelSmall,
            )
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Default,
        ),
        shape = RoundedCornerShape(12.dp),
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

@Preview
@Composable
private fun AdoptionFormScreenPreview() {
    FluffyPawTrackTheme {
        AdoptionFormScreen(
            state = AdoptionFormUiState(centerName = "Patitas Callejeras HMO", listingName = "Canela", name = "Carlos"),
            onBack = {},
            onNameChange = {},
            onPhoneChange = {},
            onEmailChange = {},
            onHousingChange = {},
            onHasOtherPetsChange = {},
            onMessageChange = {},
            onSubmit = {},
            onDone = {},
        )
    }
}
