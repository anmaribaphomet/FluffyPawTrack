package com.example.fluffypawtrack

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import com.example.fluffypawtrack.di.AppContainer
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.ui.navigation.AppNavHost
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme

/**
 * One container per process, not per composition: the fake repositories (and the
 * session) must survive activity recreation such as rotation or reopening the app.
 */
private val appContainer by lazy { AppContainer() }

@Composable
@Preview
fun App() {
    CompositionLocalProvider(LocalAppContainer provides appContainer) {
        FluffyPawTrackTheme {
            AppNavHost()
        }
    }
}
