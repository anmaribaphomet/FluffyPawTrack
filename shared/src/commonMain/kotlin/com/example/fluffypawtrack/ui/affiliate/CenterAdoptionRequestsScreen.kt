package com.example.fluffypawtrack.ui.affiliate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.domain.model.AdoptionRequest
import com.example.fluffypawtrack.domain.model.AdoptionRequestStatus
import com.example.fluffypawtrack.domain.model.ListingStatus
import com.example.fluffypawtrack.domain.model.coverPhotoUrl
import com.example.fluffypawtrack.ui.account.AccountCard
import com.example.fluffypawtrack.ui.account.AccountScaffold
import com.example.fluffypawtrack.ui.account.AdoptionStatusChip
import com.example.fluffypawtrack.ui.account.LabeledValue
import com.example.fluffypawtrack.ui.auth.AuthErrorMessage
import com.example.fluffypawtrack.ui.common.AppImage
import com.example.fluffypawtrack.ui.common.formatDateTime
import com.example.fluffypawtrack.ui.common.label
import com.example.fluffypawtrack.ui.contact.CardActionButton
import com.example.fluffypawtrack.ui.contact.MultilineField
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import kotlin.time.ExperimentalTime

/** Cuenta → "Solicitudes de adopción" for an adoption-center affiliate (RF-06). */
@Composable
fun CenterAdoptionRequestsRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = viewModel {
        CenterAdoptionRequestsViewModel(
            container.authRepository,
            container.adoptionRequestRepository,
            container.adoptionRepository,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CenterAdoptionRequestsScreen(
        state = state,
        actions = CenterAdoptionRequestsActions(
            onApprove = viewModel::approve,
            onReject = viewModel::startReject,
            onRejectReasonChange = viewModel::onRejectReasonChange,
            onConfirmReject = viewModel::confirmReject,
            onDismissReject = viewModel::dismissReject,
            onMarkAdopted = viewModel::markAdopted,
            onDismissMessage = viewModel::dismissMessage,
        ),
        onBack = onBack,
        modifier = modifier,
    )
}

data class CenterAdoptionRequestsActions(
    val onApprove: (String) -> Unit = {},
    val onReject: (String) -> Unit = {},
    val onRejectReasonChange: (String) -> Unit = {},
    val onConfirmReject: () -> Unit = {},
    val onDismissReject: () -> Unit = {},
    val onMarkAdopted: (String) -> Unit = {},
    val onDismissMessage: () -> Unit = {},
)

@Composable
fun CenterAdoptionRequestsScreen(
    state: CenterAdoptionRequestsUiState,
    actions: CenterAdoptionRequestsActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountScaffold(title = "Solicitudes de adopción", onBack = onBack, modifier = modifier) {
        state.blockedMessage?.let { blocked ->
            AccountCard { Text(blocked, style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900) }
            return@AccountScaffold
        }
        state.message?.let { message ->
            AuthErrorMessage(message)
            CardActionButton(text = "Entendido", onClick = actions.onDismissMessage, color = FluffyColors.Peach300)
        }
        if (state.loaded && state.groups.isEmpty()) {
            AccountCard {
                Text("Tu centro no tiene animales en adopción.", style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900)
            }
        }
        state.groups.forEach { group -> AnimalGroup(group, actions) }
    }
    state.rejecting?.let { dialog ->
        AlertDialog(
            onDismissRequest = actions.onDismissReject,
            title = { Text("Rechazar solicitud", color = FluffyColors.Brown900) },
            text = {
                MultilineField(
                    value = dialog.reason,
                    onValueChange = actions.onRejectReasonChange,
                    label = "Motivo del rechazo",
                    supportingText = "Obligatorio. El usuario verá este motivo.",
                    error = dialog.error,
                )
            },
            confirmButton = {
                TextButton(onClick = actions.onConfirmReject) { Text("Rechazar", color = FluffyColors.Brown900) }
            },
            dismissButton = {
                TextButton(onClick = actions.onDismissReject) { Text("Cancelar", color = FluffyColors.Brown900) }
            },
        )
    }
}

@Composable
private fun AnimalGroup(group: AnimalRequestsGroup, actions: CenterAdoptionRequestsActions) {
    val listing = group.listing
    AccountCard {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AppImage(
                listing.coverPhotoUrl,
                contentDescription = "Foto de ${listing.name}",
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    listing.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = FluffyColors.Brown900,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    "${listing.species} · ${listing.breed}",
                    style = MaterialTheme.typography.bodySmall,
                    color = FluffyColors.Brown900,
                )
                if (listing.status == ListingStatus.Adopted) {
                    Text("Adoptado", style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
                }
            }
        }
        if (group.requests.isEmpty()) {
            Text("Sin solicitudes por ahora.", style = MaterialTheme.typography.bodySmall, color = FluffyColors.Brown900)
        }
        group.requests.forEach { request ->
            HorizontalDivider(color = FluffyColors.Neutral300)
            RequestDetails(request, actions)
        }
        if (listing.status == ListingStatus.Available) {
            HorizontalDivider(color = FluffyColors.Neutral300)
            CardActionButton(
                text = "Marcar como adoptado",
                onClick = { actions.onMarkAdopted(listing.id) },
                color = FluffyColors.Sage400,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun RequestDetails(request: AdoptionRequest, actions: CenterAdoptionRequestsActions) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                request.fullName,
                style = MaterialTheme.typography.labelLarge,
                color = FluffyColors.Brown900,
                modifier = Modifier.weight(1f),
            )
            AdoptionStatusChip(request.status)
        }
        Text(formatDateTime(request.createdAt), style = MaterialTheme.typography.bodySmall, color = FluffyColors.Brown900)
        LabeledValue("Teléfono", request.phone)
        LabeledValue("Correo", request.email)
        LabeledValue("Vivienda", "${request.housingType.label} · ${request.housingTenure.label}")
        LabeledValue(
            "¿Tiene otras mascotas?",
            if (request.hasOtherPets) "Sí: ${request.otherPets.orEmpty()}" else "No",
        )
        LabeledValue("¿Por qué quiere adoptarlo?", request.reason)
        request.rejectionReason?.let { LabeledValue("Motivo del rechazo", it) }
        if (request.status == AdoptionRequestStatus.Pending) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CardActionButton(
                    text = "Aprobar",
                    onClick = { actions.onApprove(request.id) },
                    color = FluffyColors.Sage400,
                    modifier = Modifier.weight(1f),
                )
                CardActionButton(
                    text = "Rechazar",
                    onClick = { actions.onReject(request.id) },
                    color = FluffyColors.Pink400,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Preview
@Composable
private fun CenterAdoptionRequestsScreenPreview() {
    FluffyPawTrackTheme {
        CenterAdoptionRequestsScreen(
            state = CenterAdoptionRequestsUiState(
                groups = SampleData.adoptionListings.take(2).map { listing ->
                    AnimalRequestsGroup(listing, SampleData.adoptionRequests.filter { it.listingId == listing.id })
                },
                loaded = true,
            ),
            actions = CenterAdoptionRequestsActions(),
            onBack = {},
        )
    }
}
