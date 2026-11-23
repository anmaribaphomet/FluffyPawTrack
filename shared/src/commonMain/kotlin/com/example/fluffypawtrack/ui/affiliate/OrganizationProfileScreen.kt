package com.example.fluffypawtrack.ui.affiliate

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.platform.rememberPhotoLaunchers
import com.example.fluffypawtrack.ui.account.AccountCard
import com.example.fluffypawtrack.ui.account.AccountScaffold
import com.example.fluffypawtrack.ui.account.LabeledValue
import com.example.fluffypawtrack.ui.auth.AuthErrorMessage
import com.example.fluffypawtrack.ui.auth.AuthPrimaryButton
import com.example.fluffypawtrack.ui.auth.AuthTextField
import com.example.fluffypawtrack.ui.common.AppImage
import com.example.fluffypawtrack.ui.common.DirectionsButton
import com.example.fluffypawtrack.ui.common.label
import com.example.fluffypawtrack.ui.components.PillActionButton
import com.example.fluffypawtrack.ui.contact.MultilineField
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import com.example.fluffypawtrack.ui.theme.FluffyShapes
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.ic_add
import fluffypawtrack.shared.generated.resources.ic_camera
import org.jetbrains.compose.resources.painterResource

/** "Perfil de la organización" of the signed-in affiliate. */
@Composable
fun OrganizationProfileRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { OrganizationProfileViewModel(container.authRepository, container.organizationRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OrganizationProfileScreen(
        state = state,
        actions = OrganizationProfileActions(
            onDescriptionChange = viewModel::onDescriptionChange,
            onScheduleChange = viewModel::onScheduleChange,
            onPhoneChange = viewModel::onPhoneChange,
            onEmailChange = viewModel::onEmailChange,
            onSocialChange = viewModel::onSocialChange,
            onPhotoSelected = viewModel::onPhotoSelected,
            onPhotoRemoved = viewModel::onPhotoRemoved,
            onSave = viewModel::onSave,
        ),
        onBack = onBack,
        modifier = modifier,
    )
}

data class OrganizationProfileActions(
    val onDescriptionChange: (String) -> Unit = {},
    val onScheduleChange: (String) -> Unit = {},
    val onPhoneChange: (String) -> Unit = {},
    val onEmailChange: (String) -> Unit = {},
    val onSocialChange: (String) -> Unit = {},
    val onPhotoSelected: (String) -> Unit = {},
    val onPhotoRemoved: () -> Unit = {},
    val onSave: () -> Unit = {},
)

@Composable
fun OrganizationProfileScreen(
    state: OrganizationProfileUiState,
    actions: OrganizationProfileActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val launchers = rememberPhotoLaunchers(actions.onPhotoSelected)
    AccountScaffold(title = "Perfil de la organización", onBack = onBack, modifier = modifier) {
        state.blockedMessage?.let {
            InfoCard(it)
            return@AccountScaffold
        }
        val organization = state.organization ?: return@AccountScaffold
        AccountCard {
            Text(
                organization.name,
                style = MaterialTheme.typography.titleLarge,
                color = FluffyColors.Brown900,
                modifier = Modifier.semantics { heading() },
            )
            LabeledValue("Tipo", organization.type.label)
            LabeledValue("Dirección", organization.address)
            organization.capacity?.let { LabeledValue("Capacidad", "$it animales") }
            DirectionsButton(organization.location)
            Text(
                "El nombre, el tipo y la dirección los cambia la administración.",
                style = MaterialTheme.typography.bodySmall,
                color = FluffyColors.Brown900,
            )
        }

        Text("Foto del lugar o logo", style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
        val photo = state.photoRef
        if (photo != null) {
            AppImage(
                photo,
                contentDescription = "Foto de ${organization.name}",
                modifier = Modifier.fillMaxWidth().height(180.dp).clip(FluffyShapes.Card),
            )
            TextButton(onClick = actions.onPhotoRemoved) {
                Text("Cambiar foto", style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
            }
        } else {
            PillActionButton("Tomar foto", painterResource(Res.drawable.ic_camera), onClick = launchers.takePhoto)
            PillActionButton("Elegir de la galería", painterResource(Res.drawable.ic_add), onClick = launchers.pickFromGallery)
        }
        state.photoError?.let { AuthErrorMessage(it) }

        MultilineField(value = state.description, onValueChange = actions.onDescriptionChange, label = "Descripción")
        AuthTextField(
            value = state.schedule,
            onValueChange = actions.onScheduleChange,
            label = "Horario",
            capitalization = KeyboardCapitalization.Sentences,
        )
        AuthTextField(
            value = state.phone,
            onValueChange = actions.onPhoneChange,
            label = "Teléfono",
            keyboardType = KeyboardType.Phone,
        )
        AuthTextField(
            value = state.email,
            onValueChange = actions.onEmailChange,
            label = "Correo",
            keyboardType = KeyboardType.Email,
        )
        AuthTextField(
            value = state.social,
            onValueChange = actions.onSocialChange,
            label = "Redes sociales",
            helperText = "Usuario de Instagram o enlace (opcional)",
        )

        state.message?.let { AuthErrorMessage(it) }
        if (state.saved) {
            AccountCard(color = FluffyColors.Sage200) {
                Text("Cambios guardados.", style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900)
            }
        }
        AuthPrimaryButton(text = "Guardar cambios", onClick = actions.onSave, isLoading = state.isSaving)
    }
}

@Preview
@Composable
private fun OrganizationProfileScreenPreview() {
    val organization = SampleData.organizations.first { it.id == "org-patitas" }
    FluffyPawTrackTheme {
        OrganizationProfileScreen(
            state = OrganizationProfileUiState(
                organization = organization,
                description = organization.description,
                schedule = organization.schedule,
                phone = organization.phone,
                email = organization.email,
                social = organization.social.orEmpty(),
                photoRef = organization.photoUrl,
            ),
            actions = OrganizationProfileActions(),
            onBack = {},
        )
    }
}
