package com.example.fluffypawtrack.ui.adoptions

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.fluffypawtrack.domain.model.AdoptionListing
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyShadows
import com.example.fluffypawtrack.ui.theme.FluffyShapes
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.ic_chevron_right
import fluffypawtrack.shared.generated.resources.img_dog_adopt
import org.jetbrains.compose.resources.painterResource

/** Stand-in for listing photos until Storage uploads exist (fake data has none). */
@Composable
internal fun ListingPhotoPlaceholder(height: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(FluffyShapes.Card)
            .background(FluffyColors.Peach100),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.img_dog_adopt),
            contentDescription = "Foto no disponible",
            contentScale = ContentScale.Fit,
            modifier = Modifier.padding(12.dp),
        )
    }
}

/** Peach row for one adoption listing, opening its detail. */
@Composable
internal fun ListingRow(listing: AdoptionListing, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .dropShadow(FluffyShapes.OptionRow, FluffyShadows.Drop)
            .clip(FluffyShapes.OptionRow)
            .background(FluffyColors.Peach200)
            .clickable(role = Role.Button, onClickLabel = "Ver a ${listing.name}", onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(listing.name, style = MaterialTheme.typography.bodyLarge, color = FluffyColors.Brown900)
            Text(
                "${listing.species} · ${listing.breed} · ${listing.age}",
                style = MaterialTheme.typography.bodyMedium,
                color = FluffyColors.Brown900,
            )
        }
        Image(
            painter = painterResource(Res.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
        )
    }
}
