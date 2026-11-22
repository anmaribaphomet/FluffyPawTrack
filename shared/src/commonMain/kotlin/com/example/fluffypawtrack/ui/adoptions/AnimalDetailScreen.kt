package com.example.fluffypawtrack.ui.adoptions

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.ui.account.AccountCard
import com.example.fluffypawtrack.ui.account.AccountScaffold
import com.example.fluffypawtrack.ui.account.LabeledValue
import com.example.fluffypawtrack.ui.auth.AuthPrimaryButton
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.ic_heart
import org.jetbrains.compose.resources.painterResource

/** Adoption listing detail with favorite toggle and "Quiero adoptarlo". */
@Composable
fun AnimalDetailRoute(
    listingId: String,
    onBack: () -> Unit,
    onCenterClick: (String) -> Unit,
    onAdopt: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel(key = "listing-$listingId") {
        AnimalDetailViewModel(
            listingId,
            container.adoptionRepository,
            container.organizationRepository,
            container.authRepository,
            container.favoritesRepository,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AnimalDetailScreen(
        state = state,
        onBack = onBack,
        onCenterClick = onCenterClick,
        onAdopt = { onAdopt(listingId) },
        onToggleFavorite = viewModel::toggleFavorite,
        onDismissAccountMessage = viewModel::dismissAccountMessage,
        modifier = modifier,
    )
}

@Composable
fun AnimalDetailScreen(
    state: AnimalDetailUiState,
    onBack: () -> Unit,
    onCenterClick: (String) -> Unit,
    onAdopt: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDismissAccountMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listing = state.listing
    AccountScaffold(title = listing?.name ?: "Animal en adopción", onBack = onBack, modifier = modifier) {
        if (listing == null) {
            if (state.loaded) {
                Text(
                    "Este animal ya no está disponible.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FluffyColors.Brown900,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            return@AccountScaffold
        }
        ListingPhotoPlaceholder(height = 200.dp)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${listing.species} · ${listing.breed}",
                style = MaterialTheme.typography.titleMedium,
                color = FluffyColors.Brown900,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.semantics {
                    stateDescription = if (state.isFavorite) "En favoritos" else "No está en favoritos"
                },
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_heart),
                    contentDescription = if (state.isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                    modifier = Modifier.size(26.dp).alpha(if (state.isFavorite) 1f else 0.35f),
                )
            }
        }
        AccountCard {
            LabeledValue("Edad", listing.age)
            LabeledValue("Vacunación", listing.vaccination)
            LabeledValue("Descripción", listing.description)
        }
        val organizationId = state.organizationId
        if (organizationId != null) {
            TextButton(onClick = { onCenterClick(organizationId) }) {
                Text(
                    "Centro: ${state.centerName ?: "Ver centro"}",
                    style = MaterialTheme.typography.labelLarge,
                    color = FluffyColors.Brown900,
                    textDecoration = TextDecoration.Underline,
                )
            }
        }
        AuthPrimaryButton(text = "Quiero adoptarlo", onClick = onAdopt)
    }
    state.accountMessage?.let { message ->
        AlertDialog(
            onDismissRequest = onDismissAccountMessage,
            confirmButton = {
                TextButton(onClick = onDismissAccountMessage) {
                    Text("Entendido", color = FluffyColors.Brown900)
                }
            },
            title = { Text("Necesitas una cuenta", color = FluffyColors.Brown900) },
            text = { Text(message, color = FluffyColors.Brown900) },
        )
    }
}

@Preview
@Composable
private fun AnimalDetailScreenPreview() {
    FluffyPawTrackTheme {
        AnimalDetailScreen(
            state = AnimalDetailUiState(
                listing = SampleData.adoptionListings.first(),
                organizationId = "org-patitas",
                centerName = "Patitas Callejeras HMO",
                isFavorite = true,
                loaded = true,
            ),
            onBack = {},
            onCenterClick = {},
            onAdopt = {},
            onToggleFavorite = {},
            onDismissAccountMessage = {},
        )
    }
}
