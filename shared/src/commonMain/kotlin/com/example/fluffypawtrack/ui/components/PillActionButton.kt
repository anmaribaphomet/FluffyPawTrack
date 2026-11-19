package com.example.fluffypawtrack.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.fluffypawtrack.ui.theme.FluffyPalette
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import com.example.fluffypawtrack.ui.theme.FluffyShapes
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.ic_arrow_down
import fluffypawtrack.shared.generated.resources.ic_camera
import fluffypawtrack.shared.generated.resources.ic_heart
import org.jetbrains.compose.resources.painterResource

private val PillWidth = 135.dp
private val PillHeight = 32.dp
private val GlowOffset = 4.dp

/** Botón píldora crema con ícono, texto, flecha y un resplandor lima por debajo. */
@Composable
fun PillActionButton(
    text: String,
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 15.dp,
) {
    Box(modifier.size(PillWidth, PillHeight + GlowOffset)) {
        Box(
            Modifier
                .padding(top = GlowOffset)
                .size(PillWidth, PillHeight)
                .background(FluffyPalette.LimeGlow, FluffyShapes.Pill),
        )
        Row(
            modifier = Modifier
                .size(PillWidth, PillHeight)
                .clip(FluffyShapes.Pill)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(start = 16.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                Image(icon, contentDescription = null, modifier = Modifier.size(iconSize))
            }
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = FluffyPalette.TextBrown,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Image(
                painter = painterResource(Res.drawable.ic_arrow_down),
                contentDescription = null,
                modifier = Modifier.size(11.dp).rotate(-90f),
            )
        }
    }
}

@Preview
@Composable
private fun PillActionButtonPreview() {
    FluffyPawTrackTheme {
        Column(Modifier.background(FluffyPalette.CardBrown).padding(16.dp)) {
            PillActionButton("Reportar un animal", painterResource(Res.drawable.ic_camera), onClick = {})
            Spacer(Modifier.height(12.dp))
            PillActionButton("Ver adopciones", painterResource(Res.drawable.ic_heart), onClick = {}, iconSize = 17.dp)
        }
    }
}
