package com.example.fluffypawtrack.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.fluffypawtrack.domain.model.Organization
import com.example.fluffypawtrack.domain.model.OrganizationStatus
import com.example.fluffypawtrack.ui.account.AccountCard
import com.example.fluffypawtrack.ui.account.AccountScaffold
import com.example.fluffypawtrack.ui.account.LabeledValue
import com.example.fluffypawtrack.ui.common.AppImage
import com.example.fluffypawtrack.ui.common.DirectionsButton
import com.example.fluffypawtrack.ui.common.LabeledLink
import com.example.fluffypawtrack.ui.common.emailUri
import com.example.fluffypawtrack.ui.common.label
import com.example.fluffypawtrack.ui.common.phoneUri
import com.example.fluffypawtrack.ui.contact.CardActionButton
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import com.example.fluffypawtrack.ui.theme.FluffyShapes

/** Admin → "Solicitudes de afiliación" (REQUIREMENTS §6 "Administrador", RF-07). */
@Composable
fun AffiliationRequestsRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = viewModel {
        AffiliationRequestsViewModel(container.authRepository, container.organizationRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AffiliationRequestsScreen(
        state = state,
        onBack = onBack,
        onApprove = { viewModel.requestReview(it, approve = true) },
        onReject = { viewModel.requestReview(it, approve = false) },
        onConfirm = viewModel::confirmReview,
        onDismissConfirm = viewModel::dismissReview,
        onDismissMessage = viewModel::dismissMessage,
        modifier = modifier,
    )
}

@Composable
fun AffiliationRequestsScreen(
    state: AffiliationRequestsUiState,
    onBack: () -> Unit,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismissConfirm: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountScaffold(title = "Solicitudes de afiliación", onBack = onBack, modifier = modifier) {
        state.blockedMessage?.let { blocked ->
            AccountCard { Text(blocked, style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900) }
            return@AccountScaffold
        }
        ResultBanner(state.message, state.notice, onDismissMessage)
        if (state.loaded && state.pending.isEmpty()) {
            AccountCard {
                Text(
                    "No hay solicitudes de afiliación pendientes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FluffyColors.Brown900,
                )
            }
        }
        state.pending.forEach { organization ->
            AffiliationCard(
                organization = organization,
                enabled = !state.isWorking,
                onApprove = { onApprove(organization.id) },
                onReject = { onReject(organization.id) },
            )
        }
    }
    state.confirming?.let { review ->
        val name = review.organization.name
        if (review.approve) {
            ConfirmDialog(
                title = "Aprobar afiliación",
                text = "$name aparecerá en el mapa y quien la solicitó podrá atender reportes como afiliado.",
                confirmText = "Aprobar",
                onConfirm = onConfirm,
                onDismiss = onDismissConfirm,
            )
        } else {
            ConfirmDialog(
                title = "Rechazar afiliación",
                text = "$name no aparecerá en el mapa. Se avisará a quien la solicitó.",
                confirmText = "Rechazar",
                onConfirm = onConfirm,
                onDismiss = onDismissConfirm,
            )
        }
    }
}

@Composable
private fun AffiliationCard(
    organization: Organization,
    enabled: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    AccountCard {
        AppImage(
            organization.photoUrl,
            contentDescription = "Foto de ${organization.name}",
            modifier = Modifier.fillMaxWidth().height(160.dp).clip(FluffyShapes.Card),
        )
        Text(
            organization.name,
            style = MaterialTheme.typography.titleMedium,
            color = FluffyColors.Brown900,
            modifier = Modifier.semantics { heading() },
        )
        Text(organization.type.label, style = MaterialTheme.typography.bodySmall, color = FluffyColors.Brown900)
        LabeledValue("Dirección", organization.address)
        DirectionsButton(organization.location)
        LabeledLink("Teléfono", organization.phone, ::phoneUri)
        LabeledLink("Correo", organization.email, ::emailUri)
        LabeledValue("Descripción", organization.description)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CardActionButton(text = "Aprobar", onClick = onApprove, modifier = Modifier.weight(1f), enabled = enabled)
            CardActionButton(
                text = "Rechazar",
                onClick = onReject,
                modifier = Modifier.weight(1f),
                color = FluffyColors.Peach300,
                enabled = enabled,
            )
        }
    }
}

@Preview
@Composable
private fun AffiliationRequestsScreenPreview() {
    FluffyPawTrackTheme {
        AffiliationRequestsScreen(
            state = AffiliationRequestsUiState(
                loaded = true,
                pending = SampleData.organizations.filter { it.status == OrganizationStatus.Pending },
            ),
            onBack = {},
            onApprove = {},
            onReject = {},
            onConfirm = {},
            onDismissConfirm = {},
            onDismissMessage = {},
        )
    }
}
