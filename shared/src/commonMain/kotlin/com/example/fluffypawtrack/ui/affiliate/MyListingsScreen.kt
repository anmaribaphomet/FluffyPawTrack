package com.example.fluffypawtrack.ui.affiliate

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.domain.model.AdoptionListing
import com.example.fluffypawtrack.domain.model.ListingStatus
import com.example.fluffypawtrack.domain.model.coverPhotoUrl
import com.example.fluffypawtrack.ui.account.AccountCard
import com.example.fluffypawtrack.ui.account.AccountScaffold
import com.example.fluffypawtrack.ui.auth.AuthErrorMessage
import com.example.fluffypawtrack.ui.auth.AuthPrimaryButton
import com.example.fluffypawtrack.ui.common.AppImage
import com.example.fluffypawtrack.ui.contact.CardActionButton
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme

/** "Mis fichas de adopción"; [onEditListing] opens the editor of one animal. */
@Composable
fun MyListingsRoute(
    onBack: () -> Unit,
    onCreateListing: () -> Unit,
    onEditListing: (listingId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { MyListingsViewModel(container.authRepository, container.adoptionRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MyListingsScreen(
        state = state,
        onBack = onBack,
        onCreateListing = onCreateListing,
        onEditListing = onEditListing,
        onMarkAdopted = viewModel::markAdopted,
        onDismissMessage = viewModel::dismissMessage,
        modifier = modifier,
    )
}

@Composable
fun MyListingsScreen(
    state: MyListingsUiState,
    onBack: () -> Unit,
    onCreateListing: () -> Unit,
    onEditListing: (String) -> Unit,
    onMarkAdopted: (String) -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountScaffold(title = "Mis fichas de adopción", onBack = onBack, modifier = modifier) {
        state.blockedMessage?.let {
            InfoCard(it)
            return@AccountScaffold
        }
        AuthPrimaryButton(text = "Publicar un animal", onClick = onCreateListing)
        state.message?.let { message ->
            AuthErrorMessage(message)
            CardActionButton("Entendido", onClick = onDismissMessage, color = FluffyColors.Peach300)
        }
        if (state.loaded && state.listings.isEmpty()) {
            InfoCard("Aún no publicas animales en adopción.")
        }
        state.listings.forEach { listing ->
            ListingCard(listing, onEdit = { onEditListing(listing.id) }, onMarkAdopted = { onMarkAdopted(listing.id) })
        }
    }
}

@Composable
private fun ListingCard(listing: AdoptionListing, onEdit: () -> Unit, onMarkAdopted: () -> Unit) {
    AccountCard {
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Editar ficha", onClick = onEdit),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppImage(
                listing.coverPhotoUrl,
                contentDescription = "Foto de ${listing.name}",
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    listing.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = FluffyColors.Brown900,
                    modifier = Modifier.semantics { heading() },
                )
                Text("${listing.species} · ${listing.breed}", style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900)
                val available = listing.status == ListingStatus.Available
                LabelChip(
                    if (available) "Disponible" else "Adoptado",
                    if (available) FluffyColors.Sage200 else FluffyColors.Neutral300,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CardActionButton("Editar", onClick = onEdit, color = FluffyColors.Cream100, modifier = Modifier.weight(1f))
            if (listing.status == ListingStatus.Available) {
                CardActionButton(
                    "Marcar como adoptado",
                    onClick = onMarkAdopted,
                    color = FluffyColors.Sage400,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Preview
@Composable
private fun MyListingsScreenPreview() {
    FluffyPawTrackTheme {
        MyListingsScreen(
            state = MyListingsUiState(listings = SampleData.adoptionListings, loaded = true),
            onBack = {},
            onCreateListing = {},
            onEditListing = {},
            onMarkAdopted = {},
            onDismissMessage = {},
        )
    }
}
