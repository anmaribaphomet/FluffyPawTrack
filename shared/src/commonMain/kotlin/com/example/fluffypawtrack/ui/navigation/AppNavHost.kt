package com.example.fluffypawtrack.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fluffypawtrack.ui.components.HomeDestination
import com.example.fluffypawtrack.ui.home.HomeScreen
import com.example.fluffypawtrack.ui.notifications.NotificationsRoute
import com.example.fluffypawtrack.ui.profile.ProfileScreen

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Route.Home) {
        composable<Route.Home> {
            HomeScreen(
                // dropUnlessResumed ignores taps while a transition is running (double taps).
                onProfileClick = dropUnlessResumed { navController.navigate(Route.Profile) },
                onNavigate = { destination ->
                    when (destination) {
                        HomeDestination.Notificaciones -> navController.navigate(Route.Notifications) {
                            launchSingleTop = true
                        }
                        else -> Unit // Remaining tabs arrive in Phase 1.
                    }
                },
            )
        }
        composable<Route.Profile> {
            ProfileScreen(onBack = dropUnlessResumed { navController.popBackStack() })
        }
        composable<Route.Notifications> {
            NotificationsRoute(onBack = dropUnlessResumed { navController.popBackStack() })
        }
    }
}
