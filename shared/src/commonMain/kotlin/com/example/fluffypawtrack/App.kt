package com.example.fluffypawtrack

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.example.fluffypawtrack.di.AppContainer
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.ui.navigation.AppNavHost
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme

@Composable
@Preview
fun App() {
    val container = remember { AppContainer() }
    CompositionLocalProvider(LocalAppContainer provides container) {
        FluffyPawTrackTheme {
            AppNavHost()
        }
    }
}
