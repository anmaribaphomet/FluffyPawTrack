package com.example.fluffypawtrack.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.domain.model.GeoPoint
import com.example.fluffypawtrack.domain.model.OrganizationType
import com.example.fluffypawtrack.platform.rememberLocationRequester
import com.example.fluffypawtrack.platform.rememberPhotoLaunchers
import com.example.fluffypawtrack.platform.rememberReverseGeocoder
import com.example.fluffypawtrack.ui.account.AccountCard
import com.example.fluffypawtrack.ui.account.AccountScaffold
import com.example.fluffypawtrack.ui.auth.AuthErrorMessage
import com.example.fluffypawtrack.ui.auth.AuthPrimaryButton
import com.example.fluffypawtrack.ui.auth.AuthTextField
import com.example.fluffypawtrack.ui.common.AppImage
import com.example.fluffypawtrack.ui.common.label
import com.example.fluffypawtrack.ui.components.PillActionButton
import com.example.fluffypawtrack.ui.contact.MultilineField
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import com.example.fluffypawtrack.ui.theme.FluffyShapes
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.ic_add
import fluffypawtrack.shared.generated.resources.ic_camera
import fluffypawtrack.shared.generated.resources.ic_map
import org.jetbrains.compose.resources.painterResource

/**
 * Admin → create ([organizationId] null) or edit a place of the directory (RF-07).
 * [onSaved] runs once the place was stored, so the parent can go back to the directory.
 */
@Composable
fun PlaceEditorRoute(
    organizationId: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val reverseGeocoder = rememberReverseGeocoder()
    val viewModel = viewModel(key = "place-editor-${organizationId ?: "new"}") {
        PlaceEditorViewModel(container.authRepository, container.organizationRepository, organizationId, reverseGeocoder)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnSaved by rememberUpdatedState(onSaved)
    LaunchedEffect(state.saved) { if (state.saved != null) currentOnSaved() }

    PlaceEditorScreen(
        state = state,
        actions = PlaceEditorActions(
            onNameChange = viewModel::onNameChange,
            onTypeChange = viewModel::onTypeChange,
            onLatitudeChange = viewModel::onLatitudeChange,
            onLongitudeChange = viewModel::onLongitudeChange,
            onAddressChange = viewModel::onAddressChange,
            onScheduleChange = viewModel::onScheduleChange,
            onCapacityChange = viewModel::onCapacityChange,
            onPhoneChange = viewModel::onPhoneChange,
            onEmailChange = viewModel::onEmailChange,
            onSocialChange = viewModel::onSocialChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onPhotoSelected = viewModel::onPhotoSelected,
            onRemovePhoto = viewModel::onPhotoRemoved,
            onLocationResult = viewModel::onLocationResult,
            onSave = viewModel::onSave,
        ),
        onBack = onBack,
        modifier = modifier,
    )
}

data class PlaceEditorActions(
    val onNameChange: (String) -> Unit = {},
    val onTypeChange: (OrganizationType) -> Unit = {},
    val onLatitudeChange: (String) -> Unit = {},
    val onLongitudeChange: (String) -> Unit = {},
    val onAddressChange: (String) -> Unit = {},
    val onScheduleChange: (String) -> Unit = {},
    val onCapacityChange: (String) -> Unit = {},
    val onPhoneChange: (String) -> Unit = {},
    val onEmailChange: (String) -> Unit = {},
    val onSocialChange: (String) -> Unit = {},
    val onDescriptionChange: (String) -> Unit = {},
    val onPhotoSelected: (String) -> Unit = {},
    val onRemovePhoto: () -> Unit = {},
    val onLocationResult: (GeoPoint?) -> Unit = {},
    val onSave: () -> Unit = {},
)

@Composable
fun PlaceEditorScreen(
    state: PlaceEditorUiState,
    actions: PlaceEditorActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val launchers = rememberPhotoLaunchers(actions.onPhotoSelected)
    val requestLocation = rememberLocationRequester(actions.onLocationResult)
    AccountScaffold(title = if (state.isEditing) "Editar lugar" else "Nuevo lugar", onBack = onBack, modifier = modifier) {
        state.blockedMessage?.let { blocked ->
            AccountCard { Text(blocked, style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900) }
            return@AccountScaffold
        }
        if (!state.loaded) return@AccountScaffold

        AuthTextField(
            value = state.name,
            onValueChange = actions.onNameChange,
            label = "Nombre",
            error = state.errors.name,
            capitalization = KeyboardCapitalization.Words,
        )

        Text("Tipo de lugar", style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
        FilterRow(state.typeOptions, state.type, { it.label }, actions.onTypeChange)

        Text("Ubicación", style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
        PillActionButton("Usar mi ubicación", painterResource(Res.drawable.ic_map), onClick = requestLocation)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AuthTextField(
                value = state.latitude,
                onValueChange = actions.onLatitudeChange,
                label = "Latitud",
                modifier = Modifier.weight(1f),
                error = state.errors.latitude,
                keyboardType = KeyboardType.Decimal,
            )
            AuthTextField(
                value = state.longitude,
                onValueChange = actions.onLongitudeChange,
                label = "Longitud",
                modifier = Modifier.weight(1f),
                error = state.errors.longitude,
                keyboardType = KeyboardType.Decimal,
            )
        }
        AuthTextField(
            value = state.address,
            onValueChange = actions.onAddressChange,
            label = "Dirección",
            error = state.errors.address,
            capitalization = KeyboardCapitalization.Sentences,
        )
        AuthTextField(
            value = state.schedule,
            onValueChange = actions.onScheduleChange,
            label = "Horario",
            error = state.errors.schedule,
            helperText = "Por ejemplo: Lunes a viernes, 9:00 a 18:00",
            capitalization = KeyboardCapitalization.Sentences,
        )
        AuthTextField(
            value = state.capacity,
            onValueChange = actions.onCapacityChange,
            label = "Capacidad (opcional)",
            error = state.errors.capacity,
            helperText = "Cuántos animales puede recibir",
            keyboardType = KeyboardType.Number,
        )

        Text("Contacto", style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
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
        AuthTextField(
            value = state.social,
            onValueChange = actions.onSocialChange,
            label = "Redes sociales (opcional)",
            helperText = "Usuario de Instagram o enlace",
        )
        MultilineField(
            value = state.description,
            onValueChange = actions.onDescriptionChange,
            label = "Descripción",
            error = state.errors.description,
        )

        Text("Foto del lugar o logo", style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
        val photo = state.photoRef
        if (photo != null) {
            AppImage(
                photo,
                contentDescription = "Foto del lugar",
                modifier = Modifier.fillMaxWidth().height(180.dp).clip(FluffyShapes.Card),
            )
            TextButton(onClick = actions.onRemovePhoto) {
                Text("Cambiar foto", style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
            }
        } else {
            PillActionButton("Tomar foto", painterResource(Res.drawable.ic_camera), onClick = launchers.takePhoto)
            PillActionButton("Elegir de la galería", painterResource(Res.drawable.ic_add), onClick = launchers.pickFromGallery)
        }
        state.errors.photo?.let { AuthErrorMessage(it) }

        state.message?.let { AuthErrorMessage(it) }
        AuthPrimaryButton(
            text = if (state.isEditing) "Guardar cambios" else "Agregar lugar",
            onClick = actions.onSave,
            isLoading = state.isSaving,
        )
    }
}

@Preview
@Composable
private fun PlaceEditorScreenPreview() {
    FluffyPawTrackTheme {
        PlaceEditorScreen(state = PlaceEditorUiState(loaded = true), actions = PlaceEditorActions(), onBack = {})
    }
}
