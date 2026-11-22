package com.example.fluffypawtrack.ui.contact

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.platform.PhotoPreview
import com.example.fluffypawtrack.platform.rememberPhotoLaunchers
import com.example.fluffypawtrack.ui.account.AccountCard
import com.example.fluffypawtrack.ui.account.AccountScaffold
import com.example.fluffypawtrack.ui.auth.AuthErrorMessage
import com.example.fluffypawtrack.ui.auth.AuthPrimaryButton
import com.example.fluffypawtrack.ui.common.label
import com.example.fluffypawtrack.ui.components.PillActionButton
import com.example.fluffypawtrack.ui.theme.FluffyColors
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.ic_camera
import fluffypawtrack.shared.generated.resources.ic_add
import org.jetbrains.compose.resources.painterResource

/** "Es mi mascota" form (RF-05). [onSent] runs once the request was created. */
@Composable
fun ContactRequestFormRoute(
    reportId: String,
    onBack: () -> Unit,
    onSent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel(key = "contact-form-$reportId") {
        ContactRequestFormViewModel(
            reportId,
            container.authRepository,
            container.contactRequestRepository,
            container.reportRepository,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.sent) { if (state.sent) onSent() }

    ContactRequestFormScreen(
        state = state,
        onBack = onBack,
        onMessageChange = viewModel::onMessageChange,
        onPhotoSelected = viewModel::onPhotoSelected,
        onRemovePhoto = viewModel::onRemovePhoto,
        onSubmit = viewModel::onSubmit,
        modifier = modifier,
    )
}

@Composable
fun ContactRequestFormScreen(
    state: ContactRequestFormUiState,
    onBack: () -> Unit,
    onMessageChange: (String) -> Unit,
    onPhotoSelected: (String) -> Unit,
    onRemovePhoto: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val launchers = rememberPhotoLaunchers(onPhotoSelected)

    AccountScaffold(title = "Es mi mascota", onBack = onBack, modifier = modifier) {
        state.report?.let { report ->
            AccountCard {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReportThumbnail(report)
                    androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(report.category.label, style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
                        Text(report.description, style = MaterialTheme.typography.bodySmall, color = FluffyColors.Brown900)
                        Text(report.address, style = MaterialTheme.typography.bodySmall, color = FluffyColors.Brown900)
                    }
                }
            }
        }

        Text(
            "Cuéntale a quien encontró al animal por qué crees que es tu mascota: nombre, marcas, collar… " +
                "Tu teléfono y tu correo solo se comparten si acepta tu solicitud.",
            style = MaterialTheme.typography.bodyMedium,
            color = FluffyColors.Brown900,
        )

        MultilineField(
            value = state.message,
            onValueChange = onMessageChange,
            label = "Mensaje",
            supportingText = listOfNotNull(state.messageHint, state.counter).joinToString(" · "),
        )

        Text("Foto de tu mascota (opcional)", style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
        val photo = state.photoRef
        if (photo != null) {
            PhotoPreview(
                photo,
                contentDescription = "Foto de tu mascota",
                modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)),
            )
            TextButton(onClick = onRemovePhoto) {
                Text("Quitar foto", style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
            }
        } else {
            PillActionButton("Tomar foto", painterResource(Res.drawable.ic_camera), onClick = launchers.takePhoto)
            PillActionButton("Elegir de la galería", painterResource(Res.drawable.ic_add), onClick = launchers.pickFromGallery)
        }

        state.error?.let { AuthErrorMessage(it) }

        AuthPrimaryButton(text = "Enviar solicitud", onClick = onSubmit, isLoading = state.isSubmitting)
    }
}
