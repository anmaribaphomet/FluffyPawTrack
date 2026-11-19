package com.example.fluffypawtrack

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.tooling.preview.Preview
import com.example.fluffypawtrack.model.AppNotification
import com.example.fluffypawtrack.ui.components.HomeDestination
import com.example.fluffypawtrack.ui.home.HomeScreen
import com.example.fluffypawtrack.ui.notifications.NotificationsScreen
import com.example.fluffypawtrack.ui.profile.ProfileScreen
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme

/** Pantallas de la app. Navegación simple basada en estado (sin librería de navegación). */
enum class AppScreen { Home, Profile, Notifications }

@Composable
@Preview
fun App() {
    FluffyPawTrackTheme {
        var screen by rememberSaveable { mutableStateOf(AppScreen.Home) }
        val goHome = { screen = AppScreen.Home }

        // Aún no hay backend: la lista empieza vacía y aquí se conectará la fuente real.
        var notifications by remember { mutableStateOf(emptyList<AppNotification>()) }

        when (screen) {
            AppScreen.Home -> HomeScreen(
                onProfileClick = { screen = AppScreen.Profile },
                onNavigate = { destination ->
                    if (destination == HomeDestination.Notificaciones) screen = AppScreen.Notifications
                },
            )
            AppScreen.Profile -> {
                SystemBackHandler(onBack = goHome)
                ProfileScreen(onBack = goHome)
            }
            AppScreen.Notifications -> {
                SystemBackHandler(onBack = goHome)
                NotificationsScreen(
                    notifications = notifications,
                    onBack = goHome,
                    onNotificationClick = { tapped ->
                        notifications = notifications.map { if (it.id == tapped.id) it.copy(isRead = true) else it }
                    },
                )
            }
        }
    }
}

/**
 * Botón/gesto "atrás" del sistema en Android y gesto de regreso en iOS.
 *
 * `BackHandler` de compose-ui-backhandler está deprecado en favor de `NavigationBackHandler`
 * (artefacto navigationevent-compose); se mantiene aquí para no sumar otra dependencia.
 */
@Suppress("DEPRECATION")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SystemBackHandler(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
}
