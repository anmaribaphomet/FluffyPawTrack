package com.example.fluffypawtrack.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.domain.model.AdoptionRequestStatus
import com.example.fluffypawtrack.ui.auth.AuthErrorMessage
import com.example.fluffypawtrack.ui.common.AppImage
import com.example.fluffypawtrack.ui.common.color
import com.example.fluffypawtrack.ui.common.formatDateTime
import com.example.fluffypawtrack.ui.common.label
import com.example.fluffypawtrack.ui.contact.CardActionButton
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import kotlin.time.ExperimentalTime

/** Cuenta → "Mis solicitudes de adopción" (RF-06). */
@Composable
fun MyAdoptionRequestsRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = viewModel {
        MyAdoptionRequestsViewModel(
            container.authRepository,
            container.adoptionRequestRepository,
            container.adoptionRepository,
            container.organizationRepository,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MyAdoptionRequestsScreen(
        state = state,
        onBack = onBack,
        onCancel = viewModel::cancel,
        onDismissMessage = viewModel::dismissMessage,
        modifier = modifier,
    )
}

@Composable
fun MyAdoptionRequestsScreen(
    state: MyAdoptionRequestsUiState,
    onBack: () -> Unit,
    onCancel: (String) -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountScaffold(title = "Mis solicitudes de adopción", onBack = onBack, modifier = modifier) {
        state.message?.let { message ->
            AuthErrorMessage(message)
            CardActionButton(text = "Entendido", onClick = onDismissMessage, color = FluffyColors.Peach300)
        }
        if (state.loaded && state.items.isEmpty()) {
            AccountCard {
                Text(
                    "Aún no has enviado solicitudes. Encuentra a tu nuevo compañero en el mapa de adopciones.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FluffyColors.Brown900,
                )
            }
        }
        state.items.forEach { item -> RequestCard(item, onCancel = { onCancel(item.request.id) }) }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun RequestCard(item: MyAdoptionRequestItem, onCancel: () -> Unit) {
    val request = item.request
    AccountCard {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AppImage(
                item.photoUrl,
                contentDescription = "Foto de ${item.animalName}",
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.animalName, style = MaterialTheme.typography.titleMedium, color = FluffyColors.Brown900)
                Text(item.centerName, style = MaterialTheme.typography.bodySmall, color = FluffyColors.Brown900)
                Text(formatDateTime(request.createdAt), style = MaterialTheme.typography.bodySmall, color = FluffyColors.Brown900)
            }
            AdoptionStatusChip(request.status)
        }
        request.rejectionReason?.takeIf { request.status == AdoptionRequestStatus.Rejected }?.let {
            LabeledValue("Motivo", it)
        }
        if (request.status == AdoptionRequestStatus.Pending) {
            CardActionButton(text = "Cancelar", onClick = onCancel, color = FluffyColors.Peach300)
        }
    }
}

/** Status label on its status color; the text is brown-900. */
@Composable
internal fun AdoptionStatusChip(status: AdoptionRequestStatus, modifier: Modifier = Modifier) {
    Text(
        text = status.label,
        modifier = modifier
            .background(status.color, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = FluffyColors.Brown900,
    )
}

@Preview
@Composable
private fun MyAdoptionRequestsScreenPreview() {
    FluffyPawTrackTheme {
        MyAdoptionRequestsScreen(
            state = MyAdoptionRequestsUiState(
                items = SampleData.adoptionRequests.map {
                    MyAdoptionRequestItem(it, "Michi", "res:sample_listing_michi_1", "Patitas Callejeras HMO")
                },
                loaded = true,
            ),
            onBack = {},
            onCancel = {},
            onDismissMessage = {},
        )
    }
}
