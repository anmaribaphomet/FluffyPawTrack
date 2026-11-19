package com.example.fluffypawtrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fluffypawtrack.ui.theme.FluffyPalette
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import com.example.fluffypawtrack.ui.theme.FluffyShadows
import com.example.fluffypawtrack.ui.theme.FluffyShapes
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.ic_add
import fluffypawtrack.shared.generated.resources.ic_home
import fluffypawtrack.shared.generated.resources.ic_map
import fluffypawtrack.shared.generated.resources.ic_notification
import fluffypawtrack.shared.generated.resources.ic_settings
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

enum class HomeDestination(val label: String, val icon: DrawableResource) {
    Inicio("Inicio", Res.drawable.ic_home),
    Mapa("Mapa", Res.drawable.ic_map),
    Nuevo("Nuevo", Res.drawable.ic_add),
    Notificaciones("Notificaciones", Res.drawable.ic_notification),
    Ajustes("Ajustes", Res.drawable.ic_settings),
}

/** Alto del bloque inferior (desde el borde superior del pasto hasta el fondo de pantalla). */
internal val BottomNavBarHeight = 219.dp

/** Pasto ondulado con una barra de navegación tipo "vidrio" encima. */
@Composable
fun BottomNavBar(
    selected: HomeDestination,
    onSelect: (HomeDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    DesignFrame(height = BottomNavBarHeight, modifier = modifier) {
        Box(
            Modifier
                .placeAt((-62).dp, 0.dp, 484.54.dp, 307.73.dp)
                .background(MaterialTheme.colorScheme.tertiary, FluffyShapes.Grass)
                .innerShadow(FluffyShapes.Grass, FluffyShadows.InnerDeep)
                .border(1.dp, FluffyPalette.PeachBorder, FluffyShapes.Grass),
        )
        Box(Modifier.placeAt(0.dp, 77.dp, DesignWidth, 86.dp)) {
            OuterShadow(FluffyShapes.NavBar, FluffyShadows.NavBar, Modifier.fillMaxSize())
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.03f), FluffyShapes.NavBar)
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(Color.White.copy(alpha = 0.75f), Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.45f)),
                        ),
                        shape = FluffyShapes.NavBar,
                    ),
            )
        }
        Row(Modifier.placeAt(0.dp, 90.dp, DesignWidth, 70.dp).padding(horizontal = 13.25.dp)) {
            HomeDestination.entries.forEach { destination ->
                NavBarItem(
                    destination = destination,
                    selected = destination == selected,
                    onClick = { onSelect(destination) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NavBarItem(
    destination: HomeDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        CircleIconButton(
            icon = painterResource(destination.icon),
            contentDescription = destination.label,
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
            onClick = onClick,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall,
            color = FluffyPalette.TextBrown,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Preview
@Composable
private fun BottomNavBarPreview() {
    FluffyPawTrackTheme {
        var selected by remember { mutableStateOf(HomeDestination.Inicio) }
        BottomNavBar(selected, { selected = it }, Modifier.background(FluffyPalette.Peach))
    }
}
