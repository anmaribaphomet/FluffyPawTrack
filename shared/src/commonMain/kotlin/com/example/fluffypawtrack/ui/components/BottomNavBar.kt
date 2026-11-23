package com.example.fluffypawtrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fluffypawtrack.ui.theme.FluffyColors
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

/** Pasto visible debajo de la barra de vidrio (la barra ocupa de 77 a 163 dp del bloque). */
internal val BottomNavBarSpaceBelowBar = BottomNavBarHeight - 163.dp

/** Pasto ondulado con una barra de navegación tipo "vidrio" encima. */
@Composable
fun BottomNavBar(
    selected: HomeDestination,
    onSelect: (HomeDestination) -> Unit,
    modifier: Modifier = Modifier,
    /** Shown as a badge on the "Notificaciones" tab when above 0. */
    unreadNotifications: Int = 0,
) {
    DesignFrame(height = BottomNavBarHeight, modifier = modifier) {
        Box(
            Modifier
                .placeAt((-62).dp, 0.dp, 484.54.dp, 307.73.dp)
                .background(FluffyColors.Sage400, FluffyShapes.Grass)
                .innerShadow(FluffyShapes.Grass, FluffyShadows.InnerDeep)
                .border(1.dp, FluffyColors.Peach200, FluffyShapes.Grass),
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
                    badgeCount = if (destination == HomeDestination.Notificaciones) unreadNotifications else 0,
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
    badgeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            CircleIconButton(
                icon = painterResource(destination.icon),
                contentDescription = if (destination == HomeDestination.Notificaciones) {
                    notificationsTabDescription(badgeCount)
                } else {
                    destination.label
                },
                containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                onClick = onClick,
            )
            if (badgeCount > 0) {
                CountBadge(badgeCount, Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-4).dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        NavBarLabel(destination.label)
    }
}

/**
 * Unread count on a tab. Brown-900 text on cream-50 (8.59:1) inside an orange-700 ring; the
 * button's description already says the count, so the badge is hidden from TalkBack.
 */
@Composable
private fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
            .background(FluffyColors.Cream50, CircleShape)
            .border(2.dp, FluffyColors.Orange700, CircleShape)
            .padding(horizontal = 5.dp)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = badgeText(count),
            style = MaterialTheme.typography.labelSmall.copy(color = FluffyColors.Brown900, textAlign = TextAlign.Center),
            maxLines = 1,
        )
    }
}

/** What TalkBack reads for the Notifications tab, e.g. "Notificaciones, 3 sin leer". */
internal fun notificationsTabDescription(unread: Int): String =
    if (unread > 0) "${HomeDestination.Notificaciones.label}, $unread sin leer" else HomeDestination.Notificaciones.label

/** The badge keeps its size with large counts. */
internal fun badgeText(count: Int): String = if (count > 99) "99+" else count.toString()

/**
 * Tab label. It may spill a little into the neighbor tabs (whose labels are short), and with very
 * large font scales it shrinks down to the caption's design size (its size at font scale 1.0).
 * The circle button already announces the label, so the text itself is hidden from TalkBack.
 */
@Composable
private fun NavBarLabel(text: String) {
    val caption = MaterialTheme.typography.labelSmall
    val fontScale = LocalDensity.current.fontScale
    val autoSize = if (fontScale > 1f) {
        TextAutoSize.StepBased(minFontSize = caption.fontSize / fontScale, maxFontSize = caption.fontSize)
    } else {
        null
    }
    BasicText(
        text = text,
        modifier = Modifier
            .wrapContentWidth(unbounded = true)
            .widthIn(max = NavBarLabelMaxWidth)
            .clearAndSetSemantics {},
        style = caption.copy(color = FluffyColors.Brown900, textAlign = TextAlign.Center),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        autoSize = autoSize,
    )
}

/** Each tab is ~73 dp wide; "Notificaciones" needs ~91 dp at font scale 1.3. */
private val NavBarLabelMaxWidth = 96.dp

@Preview
@Composable
private fun BottomNavBarPreview() {
    FluffyPawTrackTheme {
        var selected by remember { mutableStateOf(HomeDestination.Inicio) }
        BottomNavBar(selected, { selected = it }, Modifier.background(FluffyColors.Peach200), unreadNotifications = 3)
    }
}
