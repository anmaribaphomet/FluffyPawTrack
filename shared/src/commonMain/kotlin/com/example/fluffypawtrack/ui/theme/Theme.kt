package com.example.fluffypawtrack.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun FluffyPawTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FluffyLightColorScheme,
        typography = fluffyTypography(),
        content = content,
    )
}
