package com.example.fluffypawtrack.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.fredoka_semibold
import fluffypawtrack.shared.generated.resources.inter_semibold
import fluffypawtrack.shared.generated.resources.itim_regular
import org.jetbrains.compose.resources.Font

/** Títulos redondeados (en Figma venían como imágenes de texto). */
@Composable
fun fredokaFamily() = FontFamily(Font(Res.font.fredoka_semibold, FontWeight.SemiBold))

/** Etiquetas de navegación y botones. */
@Composable
fun interFamily() = FontFamily(Font(Res.font.inter_semibold, FontWeight.SemiBold))

/** Texto descriptivo de las tarjetas. */
@Composable
fun itimFamily() = FontFamily(Font(Res.font.itim_regular, FontWeight.Normal))

@Composable
internal fun fluffyTypography(): Typography {
    val fredoka = fredokaFamily()
    val inter = interFamily()
    val itim = itimFamily()
    return Typography(
        // "¡Hola, amante de los animales!"
        headlineSmall = TextStyle(fontFamily = fredoka, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 29.sp),
        // "¡Dales una oportunidad!"
        titleLarge = TextStyle(fontFamily = fredoka, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 22.sp),
        // "¿Encontraste un animalito?"
        titleSmall = TextStyle(fontFamily = fredoka, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 17.sp),
        // Descripciones
        bodySmall = TextStyle(fontFamily = itim, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 13.sp),
        // Números de estadísticas del perfil
        titleMedium = TextStyle(fontFamily = inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 19.sp),
        // Títulos de opciones del perfil
        labelLarge = TextStyle(fontFamily = inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 17.sp),
        // Subtítulos, etiquetas de estadísticas y correo
        labelMedium = TextStyle(fontFamily = inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 15.sp),
        // Botones píldora y barra de navegación
        labelSmall = TextStyle(fontFamily = inter, fontWeight = FontWeight.SemiBold, fontSize = 8.sp, lineHeight = 10.sp),
    )
}
