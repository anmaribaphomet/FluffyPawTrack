package com.example.fluffypawtrack.ui.adoptions

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.fluffypawtrack.domain.model.AdoptionListing
import com.example.fluffypawtrack.ui.common.AppImage
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyShadows
import com.example.fluffypawtrack.ui.theme.FluffyShapes
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.ic_chevron_right
import org.jetbrains.compose.resources.painterResource

/**
 * Every photo of a listing (at least one). A single photo fills the width; several scroll
 * horizontally so the next one peeks in.
 */
@Composable
internal fun ListingGallery(listing: AdoptionListing, height: Dp, modifier: Modifier = Modifier) {
    val photos = listing.photoUrls
    if (photos.size == 1) {
        AppImage(
            photos.first(),
            contentDescription = "Foto de ${listing.name}",
            modifier = modifier.fillMaxWidth().height(height).clip(FluffyShapes.Card),
        )
        return
    }
    LazyRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(photos) { index, ref ->
            AppImage(
                ref,
                contentDescription = "Foto ${index + 1} de ${photos.size} de ${listing.name}",
                modifier = Modifier.width(280.dp).height(height).clip(FluffyShapes.Card),
            )
        }
    }
}


/**
 * Collapsed card for one animal in a center's "Animales en adopción" section: cover photo,
 * name, type, breed and age, plus an arrow. The whole card (arrow included) opens the
 * animal's full detail.
 */
@Composable
internal fun AnimalCardRow(card: AnimalCard, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .dropShadow(FluffyShapes.OptionRow, FluffyShadows.Drop)
            .clip(FluffyShapes.OptionRow)
            .background(FluffyColors.Peach200)
            .clickable(role = Role.Button, onClickLabel = "Ver ficha de ${card.name}", onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppImage(
            card.coverPhotoUrl,
            contentDescription = "Foto de ${card.name}",
            modifier = Modifier.size(64.dp).clip(FluffyShapes.Card),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(card.name, style = MaterialTheme.typography.bodyLarge, color = FluffyColors.Brown900)
            Text(
                "${card.species} · ${card.breed}",
                style = MaterialTheme.typography.bodyMedium,
                color = FluffyColors.Brown900,
            )
            Text(card.age, style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900)
        }
        Image(
            painter = painterResource(Res.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
        )
    }
}
