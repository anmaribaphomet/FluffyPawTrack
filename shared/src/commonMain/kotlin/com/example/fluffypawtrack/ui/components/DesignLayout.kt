package com.example.fluffypawtrack.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Ancho del frame de Figma (iPhone 393 pt). */
internal val DesignWidth = 393.dp

/**
 * Lienzo de ancho fijo (393 dp) centrado horizontalmente, para las piezas decorativas del
 * diseño cuya geometría orgánica desborda la pantalla.
 */
@Composable
internal fun DesignFrame(
    height: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.fillMaxWidth().height(height), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.requiredWidth(DesignWidth).fillMaxHeight(), content = content)
    }
}

/** Coloca un elemento en (x, y) con tamaño fijo, sin recortarlo aunque desborde al padre. */
internal fun Modifier.placeAt(x: Dp, y: Dp, width: Dp, height: Dp): Modifier =
    layout { measurable, _ ->
        val placeable = measurable.measure(Constraints.fixed(width.roundToPx(), height.roundToPx()))
        layout(0, 0) { placeable.place(x.roundToPx(), y.roundToPx()) }
    }

/**
 * Sombra proyectada sólo por fuera de [shape] (útil bajo superficies translúcidas,
 * donde `dropShadow` normal se vería a través del relleno).
 */
@Composable
internal fun OuterShadow(shape: Shape, shadow: Shadow, modifier: Modifier = Modifier, bleed: Dp = 12.dp) {
    Box(
        modifier
            .layout { measurable, constraints ->
                val b = bleed.roundToPx()
                val placeable = measurable.measure(
                    Constraints.fixed(constraints.maxWidth + 2 * b, constraints.maxHeight + 2 * b),
                )
                layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(-b, -b) }
            }
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val b = bleed.toPx()
                val inner = shape.createOutline(Size(size.width - 2 * b, size.height - 2 * b), layoutDirection, this)
                translate(b, b) { drawOutline(inner, Color.Black, blendMode = BlendMode.Clear) }
            }
            .padding(bleed)
            .dropShadow(shape, shadow),
    )
}
