package com.example.fluffypawtrack.ui.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.domain.model.AdoptionListing
import com.example.fluffypawtrack.ui.common.PhotoThumbnail
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import com.example.fluffypawtrack.ui.theme.FluffyShadows
import com.example.fluffypawtrack.ui.theme.FluffyShapes
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.ic_heart
import org.jetbrains.compose.resources.painterResource
import com.example.fluffypawtrack.domain.model.coverPhotoUrl

/** "Mis Favoritos": adoption listings the user saved. */
@Composable
fun FavoritesRoute(onBack: () -> Unit, onListingClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = viewModel {
        FavoritesViewModel(container.authRepository, container.favoritesRepository, container.adoptionRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FavoritesScreen(
        state = state,
        onBack = onBack,
        onListingClick = onListingClick,
        onRemove = viewModel::remove,
        modifier = modifier,
    )
}

@Composable
fun FavoritesScreen(
    state: FavoritesUiState,
    onBack: () -> Unit,
    onListingClick: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountScaffold(title = "Mis Favoritos", onBack = onBack, modifier = modifier) {
        if (state.loaded && state.listings.isEmpty()) {
            Text(
                "Aún no tienes favoritos. Guarda animales en adopción para encontrarlos aquí.",
                style = MaterialTheme.typography.bodyMedium,
                color = FluffyColors.Brown900,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        state.listings.forEach { listing ->
            FavoriteRow(listing, onClick = { onListingClick(listing.id) }, onRemove = { onRemove(listing.id) })
        }
    }
}

@Composable
private fun FavoriteRow(listing: AdoptionListing, onClick: () -> Unit, onRemove: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .dropShadow(FluffyShapes.OptionRow, FluffyShadows.Drop)
            .clip(FluffyShapes.OptionRow)
            .background(FluffyColors.Peach200)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PhotoThumbnail(listing.coverPhotoUrl, contentDescription = null)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(listing.name, style = MaterialTheme.typography.bodyLarge, color = FluffyColors.Brown900)
            Text(
                "${listing.species} · ${listing.breed} · ${listing.age}",
                style = MaterialTheme.typography.bodyMedium,
                color = FluffyColors.Brown900,
            )
        }
        IconButton(onClick = onRemove) {
            Image(
                painter = painterResource(Res.drawable.ic_heart),
                contentDescription = "Quitar a ${listing.name} de favoritos",
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Preview
@Composable
private fun FavoritesScreenPreview() {
    FluffyPawTrackTheme {
        FavoritesScreen(
            state = FavoritesUiState(listings = SampleData.adoptionListings.take(2), loaded = true),
            onBack = {},
            onListingClick = {},
            onRemove = {},
        )
    }
}
