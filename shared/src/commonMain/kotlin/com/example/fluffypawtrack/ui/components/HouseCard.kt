package com.example.fluffypawtrack.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import com.example.fluffypawtrack.ui.theme.FluffySemanticColors
import com.example.fluffypawtrack.ui.theme.FluffyShadows
import com.example.fluffypawtrack.ui.theme.FluffyShapes
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.ic_camera
import fluffypawtrack.shared.generated.resources.img_cat_phone
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
private val TextStart = 120.dp
private val TextEnd = 16.dp
private val VerticalPadding = 20.dp

/** Ilustración del lado izquierdo de la tarjeta; [x] es relativo al cuerpo de la tarjeta. */
class CardIllustration(val painter: Painter, val x: Dp, val width: Dp, val height: Dp)

/**
 * Tarjeta café en forma de casita (cuerpo redondeado + dos orejas) con sombra interior.
 * El cuerpo mide 328 dp de ancho y crece con el texto de [content] (nunca menos que
 * [minBodyHeight]), así el texto no se corta aunque el usuario agrande la letra.
 * La ilustración y el texto quedan centrados verticalmente.
 */
@Composable
fun HouseCard(
    ears: HouseCardEars,
    illustration: CardIllustration,
    minBodyHeight: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val set = earSet(ears)
    val color = FluffySemanticColors.SurfaceCard
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = {
            Box(Modifier.background(color, FluffyShapes.Card).innerShadow(FluffyShapes.Card, FluffyShadows.Inner))
            set.ears.forEach { ear ->
                Box(
                    Modifier
                        .graphicsLayer { rotationZ = ear.rotation }
                        .background(color, ear.shape)
                        .innerShadow(ear.shape, FluffyShadows.Inner),
                )
            }
            Image(illustration.painter, contentDescription = null)
            Box { content() }
        },
    ) { measurables, constraints ->
        val textWidth = (CardWidth - TextStart - TextEnd).roundToPx()
        val text = measurables.last().measure(Constraints(minWidth = textWidth, maxWidth = textWidth))
        val bodyHeight = maxOf(minBodyHeight.roundToPx(), text.height + 2 * VerticalPadding.roundToPx())
        val extent = set.extent.roundToPx()

        val body = measurables[0].measure(Constraints.fixed(CardWidth.roundToPx(), bodyHeight))
        val earPlaceables = set.ears.mapIndexed { index, ear ->
            measurables[index + 1].measure(Constraints.fixed(ear.width.roundToPx(), ear.height.roundToPx()))
        }
        val art = measurables[set.ears.size + 1].measure(
            Constraints.fixed(illustration.width.roundToPx(), illustration.height.roundToPx()),
        )

        val width = constraints.maxWidth
        // Same horizontal origin as the 393 dp Figma frame, centered on screen.
        val originX = (width - DesignWidth.roundToPx()) / 2 + CardX.roundToPx()
        layout(width, extent + bodyHeight) {
            body.place(originX, extent)
            earPlaceables.forEachIndexed { index, placeable ->
                val ear = set.ears[index]
                placeable.place(originX + ear.x.roundToPx(), extent + ear.y.roundToPx())
            }
            art.place(originX + illustration.x.roundToPx(), extent + (bodyHeight - art.height) / 2)
            text.place(originX + TextStart.roundToPx(), extent + (bodyHeight - text.height) / 2)
        }
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
    actionIconSize: Dp = 15.dp,
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = FluffyColors.Brown900,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = FluffyColors.Brown900,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        PillActionButton(actionText, actionIcon, onAction, iconSize = actionIconSize)
    }
}

@Preview
@Composable
private fun HouseCardPreview() {
    FluffyPawTrackTheme {
        Box(Modifier.background(FluffyColors.Peach200).padding(vertical = 48.dp)) {
            HouseCard(
                ears = HouseCardEars.Soft,
                illustration = CardIllustration(painterResource(Res.drawable.img_cat_phone), 6.dp, 125.dp, 116.dp),
                minBodyHeight = 157.dp,
            ) {
                CardCallToAction(
                    title = "¿Encontraste un animalito?",
                    description = "Ayúdanos a encontrarle un hogar o un refugio donde pueda estar a salvo",
                    actionText = "Reportar un animal",
                    actionIcon = painterResource(Res.drawable.ic_camera),
                    onAction = {},
                )
            }
        }
    }
}
