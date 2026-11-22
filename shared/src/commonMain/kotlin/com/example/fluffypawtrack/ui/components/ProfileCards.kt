package com.example.fluffypawtrack.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import com.example.fluffypawtrack.ui.theme.FluffyShadows
import com.example.fluffypawtrack.ui.theme.FluffyShapes
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.ic_arrow_down
import fluffypawtrack.shared.generated.resources.ic_person_outline
import fluffypawtrack.shared.generated.resources.ic_report
import org.jetbrains.compose.resources.painterResource

/** Tarjeta pequeña con un número y su etiqueta ("01 / Reportes"). */
@Composable
fun ProfileStatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .size(91.dp, 63.dp)
            .dropShadow(FluffyShapes.Card, FluffyShadows.Drop)
            .background(FluffyColors.Peach200, FluffyShapes.Card)
            .padding(top = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.bodyLarge, color = FluffyColors.Brown900)
        Spacer(Modifier.height(5.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900, maxLines = 1)
    }
}

/** Fila de opción del perfil: ícono, título, subtítulo y flecha. */
@Composable
fun ProfileOptionRow(
    icon: Painter,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
) {
    Row(
        modifier = modifier
            .size(309.dp, 63.dp)
            .dropShadow(FluffyShapes.OptionRow, FluffyShadows.Drop)
            .clip(FluffyShapes.OptionRow)
            .background(FluffyColors.Peach200)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 18.dp, end = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
            Image(icon, contentDescription = null, modifier = Modifier.size(iconSize))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = FluffyColors.Brown900, maxLines = 1)
            Spacer(Modifier.height(6.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = FluffyColors.Brown900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Image(
            painter = painterResource(Res.drawable.ic_arrow_down),
            contentDescription = null,
            modifier = Modifier.size(16.dp).rotate(-90f),
        )
    }
}

@Preview
@Composable
private fun ProfileCardsPreview() {
    FluffyPawTrackTheme {
        Column(Modifier.background(FluffyColors.White).padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                ProfileStatCard("01", "Reportes")
                ProfileStatCard("00", "Atendidos")
                ProfileStatCard("01", "Favoritos")
            }
            Spacer(Modifier.height(18.dp))
            ProfileOptionRow(
                icon = painterResource(Res.drawable.ic_person_outline),
                title = "Información Personal",
                subtitle = "Nombre, Correo y Teléfono",
                onClick = {},
                iconSize = 22.dp,
            )
            Spacer(Modifier.height(18.dp))
            ProfileOptionRow(
                icon = painterResource(Res.drawable.ic_report),
                title = "Mis reportes",
                subtitle = "Consulta el estado de tus casos",
                onClick = {},
            )
        }
    }
}
