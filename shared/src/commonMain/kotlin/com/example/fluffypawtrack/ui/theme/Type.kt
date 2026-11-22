package com.example.fluffypawtrack.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.inter_bold
import fluffypawtrack.shared.generated.resources.inter_italic
import fluffypawtrack.shared.generated.resources.inter_regular
import fluffypawtrack.shared.generated.resources.inter_semibold
import org.jetbrains.compose.resources.Font

@Composable
fun interFamily() = FontFamily(
    Font(Res.font.inter_regular, FontWeight.Normal),
    Font(Res.font.inter_italic, FontWeight.Normal, FontStyle.Italic),
    Font(Res.font.inter_semibold, FontWeight.SemiBold),
    Font(Res.font.inter_bold, FontWeight.Bold),
)

/**
 * Type scale from docs/design-system.md §2, mapped onto Material slots:
 *
 * | Design token | Material slot   |
 * |--------------|-----------------|
 * | display      | displaySmall    |
 * | headline     | headlineSmall   |
 * | title        | titleLarge      |
 * | title-card   | titleMedium     |
 * | body-large   | bodyLarge       |
 * | body         | bodyMedium      |
 * | label        | labelLarge      |
 * | helper       | bodySmall       |
 * | caption      | labelSmall      |
 */
@Composable
internal fun fluffyTypography(): Typography {
    val inter = interFamily()
    fun style(size: Int, lineHeight: Int, weight: FontWeight, fontStyle: FontStyle = FontStyle.Normal) = TextStyle(
        fontFamily = inter,
        fontWeight = weight,
        fontStyle = fontStyle,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = 0.sp,
    )
    val display = style(32, 40, FontWeight.SemiBold)
    val headline = style(24, 32, FontWeight.SemiBold)
    val title = style(20, 28, FontWeight.SemiBold)
    val body = style(14, 20, FontWeight.Normal)
    val label = style(12, 16, FontWeight.SemiBold)
    // Slots outside the table reuse the closest token so nothing falls back to the default font.
    return Typography(
        displayLarge = display,
        displayMedium = display,
        displaySmall = display,
        headlineLarge = headline,
        headlineMedium = headline,
        headlineSmall = headline,
        titleLarge = title,
        titleMedium = style(20, 28, FontWeight.Bold),
        titleSmall = title,
        bodyLarge = style(16, 24, FontWeight.SemiBold),
        bodyMedium = body,
        bodySmall = style(12, 16, FontWeight.Normal, FontStyle.Italic),
        labelLarge = label,
        labelMedium = label,
        labelSmall = style(10, 14, FontWeight.SemiBold),
    )
}
