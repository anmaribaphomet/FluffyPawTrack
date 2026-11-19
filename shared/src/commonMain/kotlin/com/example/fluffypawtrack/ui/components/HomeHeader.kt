package com.example.fluffypawtrack.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.example.fluffypawtrack.ui.theme.FluffyPalette
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import com.example.fluffypawtrack.ui.theme.FluffyShadows
import com.example.fluffypawtrack.ui.theme.FluffyShapes
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.ic_user
import fluffypawtrack.shared.generated.resources.img_logo_pin
import fluffypawtrack.shared.generated.resources.img_logo_wordmark
import org.jetbrains.compose.resources.painterResource

internal val HomeHeaderHeight = 155.dp

/** Cabecera crema ondulada con el logotipo y el botón de perfil. */
@Composable
fun HomeHeader(onProfileClick: () -> Unit, modifier: Modifier = Modifier) {
    DesignFrame(height = HomeHeaderHeight, modifier = modifier) {
        Box(
            Modifier
                .placeAt((-32).dp, 34.dp, 436.dp, 103.dp)
                .dropShadow(FluffyShapes.HeaderFront, FluffyShadows.Drop)
                .background(FluffyPalette.Cream, FluffyShapes.HeaderFront),
        )
        Box(
            Modifier
                .placeAt((-18.36).dp, 42.38.dp, 445.76.dp, 103.dp)
                .graphicsLayer { rotationZ = -5f }
                .dropShadow(FluffyShapes.HeaderBack, FluffyShadows.Drop)
                .background(FluffyPalette.Cream, FluffyShapes.HeaderBack),
        )
        // Logotipo: círculo naranja + pin + "Fluffy PawTrack"
        Box(
            Modifier
                .placeAt(22.dp, 54.dp, 60.dp, 57.dp)
                .dropShadow(FluffyShapes.Ellipse, FluffyShadows.Drop)
                .background(FluffyPalette.Orange, FluffyShapes.Ellipse),
        )
        Image(
            painter = painterResource(Res.drawable.img_logo_pin),
            contentDescription = null,
            modifier = Modifier.placeAt(12.dp, 50.dp, 83.dp, 65.dp),
        )
        Image(
            painter = painterResource(Res.drawable.img_logo_wordmark),
            contentDescription = "Fluffy PawTrack",
            modifier = Modifier.placeAt(69.dp, 73.dp, 185.dp, 42.dp),
        )
        CircleIconButton(
            icon = painterResource(Res.drawable.ic_user),
            contentDescription = "Perfil",
            containerColor = FluffyPalette.OrangeLight,
            onClick = onProfileClick,
            modifier = Modifier.placeAt(298.dp, 54.dp, 60.dp, 57.dp),
            size = DpSize(60.dp, 57.dp),
            iconSize = 33.dp,
        )
    }
}

@Preview
@Composable
private fun HomeHeaderPreview() {
    FluffyPawTrackTheme {
        HomeHeader(onProfileClick = {}, modifier = Modifier.background(FluffyPalette.Peach))
    }
}
