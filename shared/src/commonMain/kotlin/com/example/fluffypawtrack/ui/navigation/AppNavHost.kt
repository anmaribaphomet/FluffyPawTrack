package com.example.fluffypawtrack.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fluffypawtrack.domain.model.AppNotification
import com.example.fluffypawtrack.ui.components.HomeDestination
import com.example.fluffypawtrack.ui.home.HomeScreen
import com.example.fluffypawtrack.ui.notifications.NotificationsScreen
import com.example.fluffypawtrack.ui.profile.ProfileScreen

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    // Temporary local state until the notifications repository is wired in.
    var notifications by remember { mutableStateOf(emptyList<AppNotification>()) }

    NavHost(navController = navController, startDestination = Route.Home) {
        composable<Route.Home> {
            HomeScreen(
                onProfileClick = { navController.navigate(Route.Profile) },
                onNavigate = { destination ->
                    when (destination) {
                        HomeDestination.Notificaciones -> navController.navigate(Route.Notifications)
                        else -> Unit // Remaining tabs arrive in Phase 1.
                    }
                },
            )
        }
        composable<Route.Profile> {
            ProfileScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.Notifications> {
            NotificationsScreen(
                notifications = notifications,
                onBack = { navController.popBackStack() },
                onNotificationClick = { tapped ->
                    notifications = notifications.map { if (it.id == tapped.id) it.copy(isRead = true) else it }
                },
            )
        }
    }
}
