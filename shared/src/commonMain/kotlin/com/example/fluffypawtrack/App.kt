package com.example.fluffypawtrack

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.fluffypawtrack.ui.navigation.AppNavHost
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme

@Composable
@Preview
fun App() {
    FluffyPawTrackTheme {
        AppNavHost()
    }
}
