package com.example.fluffypawtrack.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.example.fluffypawtrack.ui.theme.FluffyPalette
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import com.example.fluffypawtrack.ui.theme.FluffyShadows
import com.example.fluffypawtrack.ui.theme.FluffyShapes
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.ic_home
import fluffypawtrack.shared.generated.resources.ic_user
import org.jetbrains.compose.resources.painterResource

/** Botón circular con sombra e ícono centrado (perfil y barra de navegación). */
@Composable
fun CircleIconButton(
    icon: Painter,
    contentDescription: String?,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: DpSize = DpSize(49.dp, 49.dp),
    iconSize: Dp = 26.dp,
    /** Ajuste óptico: desplaza el ícono cuando su dibujo no está equilibrado dentro del PNG. */
    iconOffset: DpOffset = DpOffset.Zero,
) {
    Box(
        modifier = modifier
            .size(size)
            .dropShadow(FluffyShapes.Ellipse, FluffyShadows.Drop)
            .clip(FluffyShapes.Ellipse)
            .background(containerColor)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(painter = icon, contentDescription = contentDescription, modifier = Modifier.offset(iconOffset.x, iconOffset.y).size(iconSize))
    }
}

@Preview
@Composable
private fun CircleIconButtonPreview() {
    FluffyPawTrackTheme {
        Row(Modifier.background(FluffyPalette.Peach).padding(16.dp)) {
            CircleIconButton(
                icon = painterResource(Res.drawable.ic_user),
                contentDescription = "Perfil",
                containerColor = FluffyPalette.OrangeLight,
                onClick = {},
                size = DpSize(60.dp, 57.dp),
                iconSize = 33.dp,
            )
            CircleIconButton(
                icon = painterResource(Res.drawable.ic_home),
                contentDescription = "Inicio",
                containerColor = FluffyPalette.Orange,
                onClick = {},
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}
