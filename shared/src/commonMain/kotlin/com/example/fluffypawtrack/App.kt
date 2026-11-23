package com.example.fluffypawtrack

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import com.example.fluffypawtrack.data.supabase.SupabaseConfig
import com.example.fluffypawtrack.di.AppContainer
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.ui.navigation.AppNavHost
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme

/**
 * One container per process, not per composition: the fake repositories (and the
 * session) must survive activity recreation such as rotation or reopening the app.
 * The first [App] call decides the config; later calls reuse the same container.
 */
private var appContainer: AppContainer? = null

private fun appContainer(config: SupabaseConfig?): AppContainer =
    appContainer ?: AppContainer(supabaseConfig = config).also {
        it.startPushTokenSync()
        appContainer = it
    }

@Composable
@Preview
fun App() {
    App(config = null)
}

/**
 * [config] null or blank keeps the app on the fake repositories. [openReportId] is the report of a
 * tapped push notification; [onReportOpened] runs once the app navigated to it.
 */
@Composable
fun App(config: SupabaseConfig?, openReportId: String? = null, onReportOpened: () -> Unit = {}) {
    CompositionLocalProvider(LocalAppContainer provides appContainer(config)) {
        FluffyPawTrackTheme {
            AppNavHost(pendingReportId = openReportId, onReportOpened = onReportOpened)
        }
    }
}
