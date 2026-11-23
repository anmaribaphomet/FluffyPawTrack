package com.example.fluffypawtrack.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.fluffypawtrack.domain.model.Organization
import com.example.fluffypawtrack.domain.model.OrganizationStatus
import com.example.fluffypawtrack.domain.model.OrganizationType
import com.example.fluffypawtrack.ui.account.AccountCard
import com.example.fluffypawtrack.ui.account.AccountScaffold
import com.example.fluffypawtrack.ui.common.AppImage
import com.example.fluffypawtrack.ui.common.label
import com.example.fluffypawtrack.ui.contact.CardActionButton
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme

/** Admin → "Directorio de lugares" (RF-07). The parent opens the editor through [onAddPlace] and [onEditPlace]. */
@Composable
fun PlacesDirectoryRoute(
    onBack: () -> Unit,
    onAddPlace: () -> Unit,
    onEditPlace: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { PlacesDirectoryViewModel(container.authRepository, container.organizationRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PlacesDirectoryScreen(
        state = state,
        actions = PlacesDirectoryActions(
            onTypeFilter = viewModel::onTypeFilter,
            onStatusFilter = viewModel::onStatusFilter,
            onAddPlace = onAddPlace,
            onEditPlace = onEditPlace,
            onToggleActive = viewModel::toggleActive,
            onDelete = viewModel::requestDelete,
            onConfirmDelete = viewModel::confirmDelete,
            onDismissDelete = viewModel::dismissDelete,
            onDismissMessage = viewModel::dismissMessage,
        ),
        onBack = onBack,
        modifier = modifier,
    )
}

data class PlacesDirectoryActions(
    val onTypeFilter: (OrganizationType?) -> Unit = {},
    val onStatusFilter: (PlaceStatus?) -> Unit = {},
    val onAddPlace: () -> Unit = {},
    val onEditPlace: (String) -> Unit = {},
    val onToggleActive: (String) -> Unit = {},
    val onDelete: (String) -> Unit = {},
    val onConfirmDelete: () -> Unit = {},
    val onDismissDelete: () -> Unit = {},
    val onDismissMessage: () -> Unit = {},
)

private val TypeOptions: List<OrganizationType?> = listOf(null) + OrganizationType.entries
private val StatusOptions: List<PlaceStatus?> = listOf(null) + PlaceStatus.entries

@Composable
fun PlacesDirectoryScreen(
    state: PlacesDirectoryUiState,
    actions: PlacesDirectoryActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountScaffold(title = "Directorio de lugares", onBack = onBack, modifier = modifier) {
        state.blockedMessage?.let { blocked ->
            AccountCard { Text(blocked, style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900) }
            return@AccountScaffold
        }
        CardActionButton(text = "Agregar lugar", onClick = actions.onAddPlace, modifier = Modifier.fillMaxWidth())
        Text("Tipo", style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
        FilterRow(TypeOptions, state.typeFilter, { it?.label ?: "Todos" }, actions.onTypeFilter)
        Text("Estado", style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
        FilterRow(StatusOptions, state.statusFilter, { it?.label ?: "Todos" }, actions.onStatusFilter)
        ResultBanner(state.message, state.notice, actions.onDismissMessage)
        if (state.loaded && state.places.isEmpty()) {
            AccountCard {
                Text("No hay lugares con estos filtros.", style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900)
            }
        }
        state.places.forEach { place -> PlaceCard(place, enabled = state.busyId == null, actions = actions) }
    }
    state.confirmingDelete?.let { place ->
        ConfirmDialog(
            title = "Eliminar lugar",
            text = "Se eliminará ${place.name} del directorio y del mapa. Sus afiliados volverán a ser " +
                "usuarios y ya no podrán atender reportes. Esta acción no se puede deshacer.",
            confirmText = "Eliminar",
            onConfirm = actions.onConfirmDelete,
            onDismiss = actions.onDismissDelete,
        )
    }
}

@Composable
private fun PlaceCard(place: Organization, enabled: Boolean, actions: PlacesDirectoryActions) {
    val status = place.placeStatus
    AccountCard {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AppImage(
                place.photoUrl,
                contentDescription = "Foto de ${place.name}",
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    place.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = FluffyColors.Brown900,
                    modifier = Modifier.semantics { heading() },
                )
                Text(place.type.label, style = MaterialTheme.typography.bodySmall, color = FluffyColors.Brown900)
                AdminChip(status.label, status.color)
            }
        }
        Text(place.address, style = MaterialTheme.typography.bodySmall, color = FluffyColors.Brown900)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CardActionButton(
                text = "Editar",
                onClick = { actions.onEditPlace(place.id) },
                modifier = Modifier.weight(1f),
                color = FluffyColors.Peach300,
            )
            CardActionButton(
                text = "Eliminar",
                onClick = { actions.onDelete(place.id) },
                modifier = Modifier.weight(1f),
                color = FluffyColors.Pink400,
                enabled = enabled,
            )
        }
        // Only approved places appear on the map; pending and rejected ones are reviewed elsewhere.
        if (place.status == OrganizationStatus.Approved) {
            CardActionButton(
                text = if (place.active) "Ocultar del mapa" else "Mostrar en el mapa",
                onClick = { actions.onToggleActive(place.id) },
                modifier = Modifier.fillMaxWidth(),
                color = FluffyColors.Sage400,
                enabled = enabled,
            )
        }
    }
}

@Preview
@Composable
private fun PlacesDirectoryScreenPreview() {
    FluffyPawTrackTheme {
        PlacesDirectoryScreen(
            state = PlacesDirectoryUiState(loaded = true, allPlaces = SampleData.organizations),
            actions = PlacesDirectoryActions(),
            onBack = {},
        )
    }
}
