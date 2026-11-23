package com.example.fluffypawtrack.ui.affiliate

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.platform.rememberPhotoLaunchers
import com.example.fluffypawtrack.ui.account.AccountScaffold
import com.example.fluffypawtrack.ui.auth.AuthErrorMessage
import com.example.fluffypawtrack.ui.auth.AuthPrimaryButton
import com.example.fluffypawtrack.ui.auth.AuthTextField
import com.example.fluffypawtrack.ui.common.AppImage
import com.example.fluffypawtrack.ui.components.PillActionButton
import com.example.fluffypawtrack.ui.contact.MultilineField
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import com.example.fluffypawtrack.ui.theme.FluffyShapes
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.ic_add
import fluffypawtrack.shared.generated.resources.ic_camera
import org.jetbrains.compose.resources.painterResource

/** Creates ([listingId] null) or edits an adoption listing; [onSaved] runs once it is stored. */
@Composable
fun ListingEditorRoute(listingId: String?, onBack: () -> Unit, onSaved: () -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = viewModel(key = "listing-editor-${listingId ?: "new"}") {
        ListingEditorViewModel(listingId, container.authRepository, container.adoptionRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved != null) onSaved() }
    ListingEditorScreen(
        state = state,
        actions = ListingEditorActions(
            onNameChange = viewModel::onNameChange,
            onSpeciesChange = viewModel::onSpeciesChange,
            onBreedChange = viewModel::onBreedChange,
            onAgeChange = viewModel::onAgeChange,
            onVaccinationChange = viewModel::onVaccinationChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onPhotoAdded = viewModel::onPhotoAdded,
            onPhotoRemoved = viewModel::onPhotoRemoved,
            onSave = viewModel::onSave,
        ),
        onBack = onBack,
        modifier = modifier,
    )
}

data class ListingEditorActions(
    val onNameChange: (String) -> Unit = {},
    val onSpeciesChange: (String) -> Unit = {},
    val onBreedChange: (String) -> Unit = {},
    val onAgeChange: (String) -> Unit = {},
    val onVaccinationChange: (String) -> Unit = {},
    val onDescriptionChange: (String) -> Unit = {},
    val onPhotoAdded: (String) -> Unit = {},
    val onPhotoRemoved: (String) -> Unit = {},
    val onSave: () -> Unit = {},
)

@Composable
fun ListingEditorScreen(
    state: ListingEditorUiState,
    actions: ListingEditorActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val launchers = rememberPhotoLaunchers(actions.onPhotoAdded)
    val title = if (state.isEditing) "Editar ficha" else "Nueva ficha de adopción"
    AccountScaffold(title = title, onBack = onBack, modifier = modifier) {
        Text("Fotos", style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
        if (state.photos.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.photos.forEachIndexed { index, photo ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AppImage(
                            photo,
                            contentDescription = if (index == 0) "Foto de portada" else "Foto ${index + 1}",
                            modifier = Modifier.size(120.dp).clip(FluffyShapes.Card),
                        )
                        TextButton(onClick = { actions.onPhotoRemoved(photo) }) {
                            Text("Quitar", style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
                        }
                    }
                }
            }
            Text("La primera foto es la portada.", style = MaterialTheme.typography.bodySmall, color = FluffyColors.Brown900)
        }
        PillActionButton("Tomar foto", painterResource(Res.drawable.ic_camera), onClick = launchers.takePhoto)
        PillActionButton("Elegir de la galería", painterResource(Res.drawable.ic_add), onClick = launchers.pickFromGallery)
        state.errors.photos?.let { AuthErrorMessage(it) }

        AuthTextField(
            value = state.name,
            onValueChange = actions.onNameChange,
            label = "Nombre",
            error = state.errors.name,
            capitalization = KeyboardCapitalization.Words,
        )
        AuthTextField(
            value = state.species,
            onValueChange = actions.onSpeciesChange,
            label = "Tipo de animal",
            error = state.errors.species,
            helperText = "Por ejemplo: Perro, Gato",
            capitalization = KeyboardCapitalization.Sentences,
        )
        AuthTextField(
            value = state.breed,
            onValueChange = actions.onBreedChange,
            label = "Raza",
            error = state.errors.breed,
            capitalization = KeyboardCapitalization.Sentences,
        )
        AuthTextField(
            value = state.age,
            onValueChange = actions.onAgeChange,
            label = "Edad",
            error = state.errors.age,
            helperText = "Por ejemplo: 2 años, 8 meses",
        )
        AuthTextField(
            value = state.vaccination,
            onValueChange = actions.onVaccinationChange,
            label = "Vacunación",
            error = state.errors.vaccination,
            helperText = "Por ejemplo: Esquema completo, Primera dosis",
            capitalization = KeyboardCapitalization.Sentences,
        )
        MultilineField(
            value = state.description,
            onValueChange = actions.onDescriptionChange,
            label = "Descripción",
            error = state.errors.description,
            supportingText = "Carácter, cuidados y con quién convive bien",
        )

        state.message?.let { AuthErrorMessage(it) }
        AuthPrimaryButton(
            text = if (state.isEditing) "Guardar cambios" else "Publicar",
            onClick = actions.onSave,
            isLoading = state.isSaving,
        )
    }
}

@Preview
@Composable
private fun ListingEditorScreenPreview() {
    FluffyPawTrackTheme {
        ListingEditorScreen(
            state = ListingEditorUiState(name = "Rocky", photos = listOf("res:sample_listing_toby")),
            actions = ListingEditorActions(),
            onBack = {},
        )
    }
}
