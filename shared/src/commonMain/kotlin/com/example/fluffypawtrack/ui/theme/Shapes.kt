package com.example.fluffypawtrack.ui.theme

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Shape construido a partir del `d` de un path SVG exportado de Figma.
 *
 * Las formas orgánicas del diseño (cabecera, orejas, pasto) llevan sombras que un vector
 * drawable no soporta, así que se dibujan como Shape para usar `dropShadow`/`innerShadow`.
 *
 * @param width/height tamaño del nodo de Figma en unidades del SVG.
 * @param originX/originY posición del nodo dentro del lienzo del SVG (el SVG incluye margen de sombra).
 * @param mirrorX/mirrorY espejos aplicados en Figma al nodo.
 */
class SvgPathShape(
    pathData: String,
    private val width: Float,
    private val height: Float,
    private val originX: Float = 0f,
    private val originY: Float = 0f,
    private val mirrorX: Boolean = false,
    private val mirrorY: Boolean = false,
) : Shape {
    private val source: Path = PathParser().parsePathString(pathData).toPath()

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val sx = size.width / width
        val sy = size.height / height
        val matrix = Matrix().apply {
            this[0, 0] = if (mirrorX) -sx else sx
            this[1, 1] = if (mirrorY) -sy else sy
            this[3, 0] = if (mirrorX) size.width + originX * sx else -originX * sx
            this[3, 1] = if (mirrorY) size.height + originY * sy else -originY * sy
        }
        val path = Path().apply {
            addPath(source)
            transform(matrix)
        }
        return Outline.Generic(path)
    }
}

private const val TangerineDripPath =
    "M100.069 225.167L24.3599 184.499C11.7072 177.702 3.66966 164.644 3.30158 150.286L0.513423 41.5251C-0.064417 18.9846 18.0927 0.421262 40.6404 0.500251L399.598 1.75775C421.635 1.83494 439.458 19.7209 439.458 41.7576V148.881C439.458 172.894 418.449 191.495 394.613 188.586L382.595 187.12C373.102 185.961 363.507 188.242 355.551 193.55L330.701 210.128C321.501 216.266 310.169 218.321 299.399 215.803L169.63 185.474C154.997 182.053 140.518 191.705 138.045 206.528C135.112 224.108 115.771 233.602 100.069 225.167Z"

object FluffyShapes {
    val Ellipse: Shape = GenericShape { size, _ -> addOval(Rect(0f, 0f, size.width, size.height)) }
    val Card = RoundedCornerShape(10.dp)
    val Pill = RoundedCornerShape(20.dp)
    val NavBar = RoundedCornerShape(100.dp)

    // Cabecera (Vector 8 al frente y Vector 9 detrás, espejado)
    val HeaderFront = SvgPathShape(
        "M124.787 101.746C87.826 101.964 26.8878 102.207 16.896 101.791C16.0235 101.755 15.1625 101.78 14.3035 101.937C9.82224 102.756 5.67321 99.3673 5.58183 94.8126L4.50822 41.3024C4.05878 18.9015 22.0947 0.5 44.5002 0.5H397.424C420.38 0.5 438.627 19.78 437.363 42.7019L437.008 49.1354C435.839 70.3391 418.305 86.9336 397.069 86.9336H143.276C138.834 86.9336 134.983 90.0085 134.001 94.3406C133.023 98.6505 129.207 101.719 124.787 101.746Z",
        width = 436f, height = 103f, originX = 3.706f, originY = 0.505f,
    )
    val HeaderBack = SvgPathShape(
        "M127.641 101.745C89.8383 101.963 27.3026 102.208 17.1486 101.79C16.2706 101.754 15.4082 101.778 14.5431 101.933C9.94381 102.756 5.6974 99.2751 5.60158 94.6038L4.5086 41.3203C4.04895 18.9126 22.0877 0.5 44.5002 0.5H407.114C430.09 0.5 448.343 19.8112 447.05 42.751L446.688 49.1845C445.494 70.3678 427.968 86.9336 406.751 86.9336H146.22C141.769 86.9336 137.905 90.0036 136.9 94.34C135.899 98.6548 132.071 101.719 127.641 101.745Z",
        width = 445.761f, height = 103f, originX = 3.655f, originY = 0.505f, mirrorX = true,
    )

    // Cabecera trasera del perfil: igual idea que HeaderBack pero más alta (Vector 9, nodo 11:230)
    val HeaderBackTall = SvgPathShape(
        "M127.013 111.834C89.2764 112.072 27.7532 112.334 17.3336 111.884C16.3946 111.844 15.4525 111.873 14.5304 112.055C10.0086 112.944 5.78096 109.532 5.695 104.925L4.50711 41.246C4.08963 18.8666 22.1168 0.500002 44.5001 0.5H407.405C430.3 0.5 448.524 19.6819 447.352 42.5476L446.581 57.591C445.491 78.8586 427.929 95.5434 406.634 95.5434H147.019C142.166 95.5434 137.976 98.9417 136.975 103.69C135.979 108.413 131.839 111.804 127.013 111.834Z",
        width = 445.761f, height = 113.26f, originX = 3.744f, originY = 0.498f, mirrorX = true,
    )

    // Onda naranja del perfil (Vector 12 arriba / Vector 13 abajo: mismo trazo, el de abajo girado 180°)
    val TangerineDripTop = SvgPathShape(TangerineDripPath, width = 440f, height = 242f, originX = -0.528f, originY = 0.363f)
    val TangerineDripBottom = SvgPathShape(
        TangerineDripPath, width = 440f, height = 242f, originX = -0.528f, originY = 0.363f, mirrorX = true, mirrorY = true,
    )

    val ProfileSheet = RoundedCornerShape(40.dp)
    val OptionRow = RoundedCornerShape(5.dp)

    // Pasto inferior (Vector 1)
    val Grass = SvgPathShape(
        "M81.3425 0.818535C41.8267 -2.62167 17.738 22.6981 16.4601 33.7284L0.595646 265.127C-0.998223 288.375 17.5388 308.059 40.8407 307.862L439.44 304.485C461.217 304.301 478.844 286.73 479.099 264.954L481.681 43.9304C481.951 20.8118 461.427 3.59088 444.538 19.3805C415.273 46.7412 397.61 90.5774 343.034 44.6564C340.814 42.7892 338.292 40.9248 335.672 39.6823C275.109 10.9649 277.858 129.439 229.894 39.9371C222.478 26.0996 208.139 16.0866 192.97 20.1306C146.127 32.6183 127.458 72.8163 109.219 25.9841C104.292 13.3325 94.8684 1.99609 81.3425 0.818535Z",
        width = 484.539f, height = 307.727f, originX = -2.374f, originY = 0.492f,
    )

    // Orejas redondeadas de la primera tarjeta (Vector 7 izquierda, Vector 3 derecha)
    val EarSoftLeft = SvgPathShape(
        "M23.3726 4.383L75.1905 0.133877C84.9637 -0.667532 94.6908 2.14896 102.524 8.04827L116.709 18.732C117.206 19.1057 117.67 19.5198 118.098 19.97C124.237 26.4268 120.82 37.1174 112.075 38.817L30.3044 54.7078C12.6184 58.1448 -2.93694 42.6133 0.472806 24.9221C2.63317 13.7132 11.9956 5.31592 23.3726 4.383Z",
        width = 138.936f, height = 63f, originX = -6.655f, originY = -1.109f, mirrorX = true, mirrorY = true,
    )
    val EarSoftRight = SvgPathShape(
        "M22.872 4.2867L74.5108 0.129091C84.3543 -0.663447 94.1431 2.21336 101.993 8.20569L115.358 18.4089C122.133 24.9799 118.698 36.458 109.427 38.227L29.5248 53.4731C12.234 56.7724 -2.91216 41.5459 0.478528 24.2727C2.6233 13.3466 11.7733 5.1803 22.872 4.2867Z",
        width = 138.064f, height = 61.469f, originX = -6.613f, originY = -1.1f, mirrorY = true,
    )

    // Orejas puntiagudas de la segunda tarjeta (Vector 5 izquierda, Vector 6 derecha)
    val EarPointyLeft = SvgPathShape(
        "M69.8336 49.4378L15.8766 68.5913C10.4081 70.5325 4.36955 67.9137 2.04957 62.5949C1.5409 61.4287 1.23743 60.1834 1.15252 58.9139L0.0891618 43.0145C-0.20055 38.6827 0.216877 34.3326 1.32506 30.135L4.53911 17.9606L8.88348 6.75549C10.9089 1.04484 17.457 -1.62724 22.8995 1.036L45.0713 11.8856C47.3662 13.0085 49.519 14.401 51.4843 16.0335L60.1566 23.2373C62.1727 24.912 64.0194 26.7809 65.6699 28.8169L71.5045 36.0141C71.7524 36.3199 72.024 36.6055 72.317 36.8683C76.5253 40.6436 75.1615 47.5466 69.8336 49.4378Z",
        width = 82f, height = 76f, originX = -0.344f, originY = -3.96f,
    )
    val EarPointyRight = SvgPathShape(
        "M71.5026 51.3692L16.4247 71.2084C10.7579 73.2497 4.48149 70.5251 2.10283 64.9914C1.59167 63.8023 1.28697 62.5346 1.20185 61.2431L0.0866012 44.3223C-0.194909 40.0512 0.210957 35.7626 1.28871 31.6201L4.66822 18.6305L9.11855 6.98307C11.1861 1.06783 17.9708 -1.69599 23.5831 1.09079L46.2035 12.3228C48.5446 13.4853 50.7379 14.9242 52.7365 16.6088L61.7352 24.1938C63.7387 25.8826 65.5714 27.7641 67.2069 29.8113L73.2632 37.392C73.5196 37.7129 73.8009 38.013 74.1047 38.2895C78.4416 42.2375 77.0203 49.3817 71.5026 51.3692Z",
        width = 84f, height = 79f, originX = -0.336f, originY = -4.155f, mirrorX = true,
    )
}

/** Sombras del diseño (Figma: blur 4, offset Y 4, negro 25 %). */
object FluffyShadows {
    private val ShadowColor = Color.Black.copy(alpha = 0.25f)
    val Drop = Shadow(radius = 4.dp, color = ShadowColor, offset = DpOffset(0.dp, 4.dp))
    val Inner = Shadow(radius = 4.dp, color = ShadowColor, offset = DpOffset(0.dp, 4.dp))
    val InnerDeep = Shadow(radius = 4.dp, color = ShadowColor, offset = DpOffset(0.dp, 6.dp))
    val NavBar = Shadow(radius = 2.dp, color = ShadowColor, offset = DpOffset(0.dp, 4.dp))
}
