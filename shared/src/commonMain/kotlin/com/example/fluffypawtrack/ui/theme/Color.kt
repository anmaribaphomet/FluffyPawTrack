package com.example.fluffypawtrack.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Primitive color tokens from docs/design-system.md §1.1. */
object FluffyColors {
    val Brown900 = Color(0xFF633D14)
    val Brown600 = Color(0xFFAA9885)
    val Brown300 = Color(0xFFC5A67E)
    val Cream50 = Color(0xFFF8F3E7)
    val Cream100 = Color(0xFFF0E2D2)
    val White = Color(0xFFFFFFFF)
    val Peach100 = Color(0xFFFFE3BB)
    val Peach200 = Color(0xFFFCD4A1)
    val Peach300 = Color(0xFFFEBE99)
    val Orange500 = Color(0xFFF8AE45)
    val Orange700 = Color(0xFFFF8300)
    val Sage200 = Color(0xFFD9E1B2)
    val Sage400 = Color(0xFFBFC98D)
    val Sage700 = Color(0xFF7A8542)
    val Pink400 = Color(0xFFFF8F9E)
    val Neutral300 = Color(0xFFD9D9D9)
    val Neutral900 = Color(0xFF2D2D2D)
}

/**
 * Semantic tokens from docs/design-system.md §1.2 that have no Material slot.
 * Text on any of these surfaces is [FluffyColors.Brown900].
 */
object FluffySemanticColors {
    val BackgroundHome = FluffyColors.Peach200
    val SurfaceCard = FluffyColors.Brown300
    val SurfaceSettings = FluffyColors.Sage400
    val PrimaryStrong = FluffyColors.Orange700
    val OnSurfaceMuted = FluffyColors.Brown600
    val Accent = FluffyColors.Pink400
    val Disabled = FluffyColors.Neutral300
}

internal val FluffyLightColorScheme = lightColorScheme(
    primary = FluffyColors.Orange500,
    onPrimary = FluffyColors.Brown900,
    primaryContainer = FluffyColors.Peach200,
    onPrimaryContainer = FluffyColors.Brown900,
    secondary = FluffyColors.Sage400,
    onSecondary = FluffyColors.Brown900,
    secondaryContainer = FluffyColors.Cream100,
    onSecondaryContainer = FluffyColors.Brown900,
    tertiary = FluffyColors.Pink400,
    onTertiary = FluffyColors.Brown900,
    background = FluffyColors.Cream50,
    onBackground = FluffyColors.Brown900,
    surface = FluffyColors.White,
    onSurface = FluffyColors.Brown900,
    surfaceVariant = FluffyColors.Cream100,
    onSurfaceVariant = FluffyColors.Brown900,
    outline = FluffyColors.Sage700,
)
