package com.example.fluffypawtrack.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fluffypawtrack.ui.theme.FluffyPalette
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import com.example.fluffypawtrack.ui.theme.FluffyShadows
import com.example.fluffypawtrack.ui.theme.FluffyShapes
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.ic_chevron_right
import fluffypawtrack.shared.generated.resources.img_logo_pin
import fluffypawtrack.shared.generated.resources.img_logo_wordmark
import org.jetbrains.compose.resources.painterResource

internal val ProfileHeaderHeight = 150.dp

/**
 * Cabecera crema de pantallas secundarias: logotipo a la izquierda y botón "Regresar" a la derecha.
 *
 * @param showLogoBadge círculo naranja detrás del pin (Perfil lo muestra, Notificaciones no).
 */
@Composable
fun ProfileHeader(onBack: () -> Unit, modifier: Modifier = Modifier, showLogoBadge: Boolean = true) {
    DesignFrame(height = ProfileHeaderHeight, modifier = modifier) {
        Box(
            Modifier
                .placeAt((-32).dp, 34.dp, 436.dp, 103.dp)
                .dropShadow(FluffyShapes.HeaderFront, FluffyShadows.Drop)
                .background(FluffyPalette.Cream, FluffyShapes.HeaderFront),
        )
        Box(
            Modifier
                .placeAt((-21.91).dp, 34.21.dp, 445.76.dp, 113.26.dp)
                .graphicsLayer { rotationZ = -5f }
                .dropShadow(FluffyShapes.HeaderBackTall, FluffyShadows.Drop)
                .background(FluffyPalette.Cream, FluffyShapes.HeaderBackTall),
        )
        if (showLogoBadge) {
            Box(
                Modifier
                    .placeAt(22.dp, 59.dp, 60.dp, 57.dp)
                    .dropShadow(FluffyShapes.Ellipse, FluffyShadows.Drop)
                    .background(FluffyPalette.TitleOrange, FluffyShapes.Ellipse),
            )
        }
        Image(
            painter = painterResource(Res.drawable.img_logo_pin),
            contentDescription = null,
            modifier = Modifier.placeAt(12.dp, 51.dp, 83.dp, 65.dp),
        )
        Image(
            painter = painterResource(Res.drawable.img_logo_wordmark),
            contentDescription = "Fluffy PawTrack",
            modifier = Modifier.placeAt(70.dp, 72.dp, 185.dp, 42.dp),
        )
        HeaderBackButton(onClick = onBack, modifier = Modifier.placeAt(341.dp, 52.dp, 48.dp, 56.dp))
    }
}

/** Flecha + texto "Regresar" de la cabecera. */
@Composable
private fun HeaderBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClickLabel = "Regresar", onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(4.dp))
        Image(
            painter = painterResource(Res.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(31.dp),
        )
        Spacer(Modifier.height(5.dp))
        Text("Regresar", style = MaterialTheme.typography.labelSmall, color = FluffyPalette.TextBrown, maxLines = 1)
    }
}

@Preview
@Composable
private fun ProfileHeaderPreview() {
    FluffyPawTrackTheme {
        ProfileHeader(onBack = {}, modifier = Modifier.background(FluffyPalette.Tangerine))
    }
}
