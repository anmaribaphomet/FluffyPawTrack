package com.example.fluffypawtrack.ui.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.platform.rememberPhotoLaunchers
import com.example.fluffypawtrack.ui.auth.AuthErrorMessage
import com.example.fluffypawtrack.ui.auth.AuthPrimaryButton
import com.example.fluffypawtrack.ui.auth.AuthTextField
import com.example.fluffypawtrack.ui.common.AppImage
import com.example.fluffypawtrack.ui.components.PillActionButton
import com.example.fluffypawtrack.ui.contact.CardActionButton
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import com.example.fluffypawtrack.ui.theme.FluffyShapes
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.ic_add
import fluffypawtrack.shared.generated.resources.ic_camera
import fluffypawtrack.shared.generated.resources.ic_user
import org.jetbrains.compose.resources.painterResource

/** "Información Personal", reached from the account screen of every role. */
@Composable
fun PersonalInfoRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { PersonalInfoViewModel(container.authRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PersonalInfoScreen(
        state = state,
        onBack = onBack,
        modifier = modifier,
        actions = PersonalInfoActions(
            onEdit = viewModel::onEdit,
            onFirstNameChange = viewModel::onFirstNameChange,
            onLastNameChange = viewModel::onLastNameChange,
            onPhoneChange = viewModel::onPhoneChange,
            onPhotoSelected = viewModel::onPhotoSelected,
            onPhotoRemoved = viewModel::onPhotoRemoved,
            onSave = viewModel::onSave,
            onCancel = viewModel::onCancel,
        ),
    )
}

data class PersonalInfoActions(
    val onEdit: () -> Unit = {},
    val onFirstNameChange: (String) -> Unit = {},
    val onLastNameChange: (String) -> Unit = {},
    val onPhoneChange: (String) -> Unit = {},
    val onPhotoSelected: (String) -> Unit = {},
    val onPhotoRemoved: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onCancel: () -> Unit = {},
)

@Composable
fun PersonalInfoScreen(
    state: PersonalInfoUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: PersonalInfoActions = PersonalInfoActions(),
) {
    AccountScaffold(title = "Información Personal", onBack = onBack, modifier = modifier) {
        val draft = state.draft
        if (draft == null) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { ProfilePhoto(state.avatarUrl) }
            AccountCard(color = FluffyColors.Peach200) {
                LabeledValue("Nombre", state.firstName)
                LabeledValue("Apellidos", state.lastName)
            }
            AccountCard(color = FluffyColors.Peach200) {
                LabeledValue("Correo electrónico", state.email, emptyText = "Sin correo (sesión anónima)")
                LabeledValue("Teléfono", state.phone, emptyText = "Sin teléfono registrado")
            }
            state.confirmation?.let {
                AccountCard(color = FluffyColors.Sage200) {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900)
                }
            }
            if (state.canEdit) AuthPrimaryButton(text = "Editar", onClick = actions.onEdit)
        } else {
            PersonalInfoForm(state, draft, actions)
        }
    }
}

@Composable
private fun PersonalInfoForm(state: PersonalInfoUiState, draft: PersonalInfoDraft, actions: PersonalInfoActions) {
    val launchers = rememberPhotoLaunchers(actions.onPhotoSelected)
    Text("Foto de perfil (opcional)", style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
    val photo = draft.photoRef
    if (photo != null) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { ProfilePhoto(photo) }
        TextButton(onClick = actions.onPhotoRemoved) {
            Text("Cambiar foto", style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
        }
    } else {
        PillActionButton("Tomar foto", painterResource(Res.drawable.ic_camera), onClick = launchers.takePhoto)
        PillActionButton("Elegir de la galería", painterResource(Res.drawable.ic_add), onClick = launchers.pickFromGallery)
    }

    AuthTextField(
        value = draft.firstName,
        onValueChange = actions.onFirstNameChange,
        label = "Nombre",
        error = draft.firstNameError,
        capitalization = KeyboardCapitalization.Words,
    )
    AuthTextField(
        value = draft.lastName,
        onValueChange = actions.onLastNameChange,
        label = "Apellidos",
        capitalization = KeyboardCapitalization.Words,
    )
    AuthTextField(
        value = draft.phone,
        onValueChange = actions.onPhoneChange,
        label = "Teléfono",
        error = draft.phoneError,
        helperText = "10 dígitos",
        keyboardType = KeyboardType.Phone,
    )
    AccountCard(color = FluffyColors.Peach200) {
        LabeledValue("Correo electrónico", state.email, emptyText = "Sin correo")
        Text("El correo no se puede cambiar.", style = MaterialTheme.typography.bodySmall, color = FluffyColors.Brown900)
    }

    state.message?.let { AuthErrorMessage(it) }
    AuthPrimaryButton(text = "Guardar cambios", onClick = actions.onSave, isLoading = state.isSaving)
    CardActionButton(
        text = "Cancelar",
        onClick = actions.onCancel,
        color = FluffyColors.Peach300,
        enabled = !state.isSaving,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Round profile photo, or the default person icon when there is none. */
@Composable
internal fun ProfilePhoto(ref: String?, modifier: Modifier = Modifier) {
    val shape = FluffyShapes.Ellipse
    if (ref.isNullOrBlank()) {
        Box(modifier.size(96.dp).background(FluffyColors.Brown300, shape), contentAlignment = Alignment.Center) {
            Image(painterResource(Res.drawable.ic_user), contentDescription = "Foto de perfil", modifier = Modifier.size(64.dp))
        }
    } else {
        AppImage(ref, contentDescription = "Foto de perfil", modifier = modifier.size(96.dp).clip(shape))
    }
}

@Preview
@Composable
private fun PersonalInfoScreenPreview() {
    FluffyPawTrackTheme {
        PersonalInfoScreen(
            state = PersonalInfoUiState("María", "Valenzuela", "demo@correo.com", "6620000001", canEdit = true),
            onBack = {},
        )
    }
}
