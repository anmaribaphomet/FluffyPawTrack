package com.example.fluffypawtrack.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Paleta extraída del diseño de Figma (Fluffy PawTrack / INICIO). */
object FluffyPalette {
    val Peach = Color(0xFFFCD4A1)          // Fondo de pantalla
    val PeachBorder = Color(0xFFFAD3A2)    // Borde del pasto
    val Cream = Color(0xFFF8F3E7)          // Cabecera y botones píldora
    val Sand = Color(0xFFF0E2D2)           // Botones circulares de navegación
    val CardBrown = Color(0xFFC5A67E)      // Tarjetas en forma de casita
    val Orange = Color(0xFFFF9526)         // Acento principal / ítem seleccionado
    val OrangeLight = Color(0xFFF8AF49)    // Botón de perfil
    val Grass = Color(0xFFBFC98D)          // Pasto inferior
    val Mint = Color(0xFFA2CEAA)           // "Track" del logotipo
    val LimeGlow = Color(0x33D4FF00)       // Resplandor bajo los botones píldora
    val TitleOrange = Color(0xFFDEA467)    // Saludo principal
    val TitleBrown = Color(0xFF825D35)     // Títulos de tarjetas
    val TextBrown = Color(0xFF633D14)      // Etiquetas
    val TextDark = Color(0xFF2D2D2D)       // Texto descriptivo

    // Perfil (GESTION DE CUENTA)
    val Sheet = Color(0xFFFFFFFF)          // Hoja blanca central
    val Apricot = Color(0xFFF4BD82)        // Tarjetas de estadísticas y opciones
    val Tangerine = Color(0xFFFFA03C)      // Ondas naranjas superior e inferior
    val TangerineBorder = Color(0xFFFEBE99)
    val EmailTan = Color(0xFFDDAB75)       // Correo bajo el nombre

    // Notificaciones
    val MessageBrown = Color(0xFF8D673E)   // Cuerpo del mensaje
    val CardGlass = Color(0x99FFFFFF)      // Fondo de tarjeta (blanco 60 %)
}

internal val FluffyLightColorScheme = lightColorScheme(
    primary = FluffyPalette.Orange,
    onPrimary = Color.White,
    primaryContainer = FluffyPalette.OrangeLight,
    onPrimaryContainer = FluffyPalette.TextBrown,
    secondary = FluffyPalette.CardBrown,
    onSecondary = FluffyPalette.TextDark,
    secondaryContainer = FluffyPalette.Sand,
    onSecondaryContainer = FluffyPalette.TextBrown,
    tertiary = FluffyPalette.Grass,
    onTertiary = FluffyPalette.TextBrown,
    background = FluffyPalette.Peach,
    onBackground = FluffyPalette.TextDark,
    surface = FluffyPalette.Cream,
    onSurface = FluffyPalette.TextBrown,
    surfaceVariant = FluffyPalette.CardBrown,
    onSurfaceVariant = FluffyPalette.TextDark,
)
