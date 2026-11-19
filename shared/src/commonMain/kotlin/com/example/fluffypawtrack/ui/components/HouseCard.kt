package com.example.fluffypawtrack.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.fluffypawtrack.ui.theme.FluffyPalette
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import com.example.fluffypawtrack.ui.theme.FluffyShadows
import com.example.fluffypawtrack.ui.theme.FluffyShapes
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.img_cat_phone
import fluffypawtrack.shared.generated.resources.ic_camera
import org.jetbrains.compose.resources.painterResource

/** Estilo de las "orejas" que forman el techo de la tarjeta. */
enum class HouseCardEars { Soft, Pointy }

private class Ear(val shape: Shape, val x: Dp, val y: Dp, val width: Dp, val height: Dp, val rotation: Float = 0f)

private class EarSet(val extent: Dp, val ears: List<Ear>)

// Posiciones relativas al cuerpo de la tarjeta, medidas en Figma.
private fun earSet(style: HouseCardEars) = when (style) {
    HouseCardEars.Soft -> EarSet(
        extent = 31.5.dp,
        ears = listOf(
            Ear(FluffyShapes.EarSoftLeft, (-37.59).dp, (-31.42).dp, 138.94.dp, 63.dp, rotation = -10f),
            Ear(FluffyShapes.EarSoftRight, 225.29.dp, (-31.48).dp, 138.06.dp, 61.47.dp, rotation = 10f),
        ),
    )
    HouseCardEars.Pointy -> EarSet(
        extent = 47.dp,
        ears = listOf(
            Ear(FluffyShapes.EarPointyLeft, (-5).dp, (-47).dp, 82.dp, 76.dp),
            Ear(FluffyShapes.EarPointyRight, 248.dp, (-47).dp, 84.dp, 79.dp),
        ),
    )
}

private val CardX = 35.dp
private val CardWidth = 328.dp

/**
 * Tarjeta café en forma de casita (cuerpo redondeado + dos orejas) con sombra interior.
 * El [content] se dibuja dentro del cuerpo (328 dp × [bodyHeight]).
 */
@Composable
fun HouseCard(
    ears: HouseCardEars,
    bodyHeight: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val set = earSet(ears)
    val color = MaterialTheme.colorScheme.secondary
    DesignFrame(height = set.extent + bodyHeight, modifier = modifier) {
        Box(
            Modifier
                .placeAt(CardX, set.extent, CardWidth, bodyHeight)
                .background(color, FluffyShapes.Card)
                .innerShadow(FluffyShapes.Card, FluffyShadows.Inner),
        )
        set.ears.forEach { ear ->
            Box(
                Modifier
                    .placeAt(CardX + ear.x, set.extent + ear.y, ear.width, ear.height)
                    .graphicsLayer { rotationZ = ear.rotation }
                    .background(color, ear.shape)
                    .innerShadow(ear.shape, FluffyShadows.Inner),
            )
        }
        Box(Modifier.placeAt(CardX, set.extent, CardWidth, bodyHeight), content = content)
    }
}

/** Columna derecha de la tarjeta: título, descripción y botón de acción. */
@Composable
fun CardCallToAction(
    title: String,
    description: String,
    actionText: String,
    actionIcon: Painter,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.titleSmall,
    actionIconSize: Dp = 15.dp,
    descriptionWidth: Dp = 172.dp,
) {
    Column(
        modifier = modifier.width(200.dp).fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = titleStyle, color = FluffyPalette.TitleBrown, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            text = description,
            modifier = Modifier.width(descriptionWidth),
            style = MaterialTheme.typography.bodySmall,
            color = FluffyPalette.TextDark,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        PillActionButton(actionText, actionIcon, onAction, iconSize = actionIconSize)
    }
}

@Preview
@Composable
private fun HouseCardPreview() {
    FluffyPawTrackTheme {
        Box(Modifier.background(FluffyPalette.Peach).padding(vertical = 16.dp)) {
            HouseCard(ears = HouseCardEars.Soft, bodyHeight = 157.dp) {
                Image(
                    painterResource(Res.drawable.img_cat_phone),
                    contentDescription = null,
                    modifier = Modifier.placeAt(8.dp, 10.dp, 125.dp, 116.dp),
                )
                CardCallToAction(
                    title = "¿Encontraste un animalito?",
                    description = "Ayúdanos a encontrarle un hogar o un refugio donde pueda estar a salvo",
                    actionText = "Reportar un animal",
                    actionIcon = painterResource(Res.drawable.ic_camera),
                    onAction = {},
                    modifier = Modifier.padding(start = 115.dp, top = 35.dp, bottom = 14.dp),
                )
            }
        }
    }
}
