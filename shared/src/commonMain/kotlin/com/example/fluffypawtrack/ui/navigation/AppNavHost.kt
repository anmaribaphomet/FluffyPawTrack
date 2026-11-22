package com.example.fluffypawtrack.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.fluffypawtrack.ui.auth.LoginRoute
import com.example.fluffypawtrack.ui.auth.RegisterRoute
import com.example.fluffypawtrack.ui.components.HomeDestination
import com.example.fluffypawtrack.ui.splash.SplashRoute
import com.example.fluffypawtrack.ui.home.HomeScreen
import com.example.fluffypawtrack.ui.map.MapFilter
import com.example.fluffypawtrack.ui.map.MapRoute
import com.example.fluffypawtrack.ui.notifications.NotificationsRoute
import com.example.fluffypawtrack.ui.profile.ProfileScreen

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Route.Splash) {
        composable<Route.Splash> {
            SplashRoute(
                onFinished = { isSignedIn ->
                    navController.navigate(if (isSignedIn) Route.Home else Route.Login) {
                        popUpTo<Route.Splash> { inclusive = true }
                    }
                },
            )
        }
        composable<Route.Login> {
            LoginRoute(
                onSignedIn = { navController.navigateClearingStack(Route.Home) },
                onCreateAccount = dropUnlessResumed { navController.navigate(Route.Register) },
                // The new-report flow arrives with T2; until then the anonymous user lands on Home.
                onReportAnonymously = { navController.navigateClearingStack(Route.Home) },
            )
        }
        composable<Route.Register> {
            RegisterRoute(
                onBack = dropUnlessResumed { navController.popBackStack() },
                onRegistered = { navController.navigateClearingStack(Route.Home) },
            )
        }
        composable<Route.Home> {
            HomeScreen(
                // dropUnlessResumed ignores taps while a transition is running (double taps).
                onProfileClick = dropUnlessResumed { navController.navigate(Route.Profile) },
                onSeeAdoptions = dropUnlessResumed { navController.navigate(Route.Map(adoptionsOnly = true)) },
                onNavigate = { destination ->
                    when (destination) {
                        HomeDestination.Notificaciones -> navController.navigate(Route.Notifications) {
                            launchSingleTop = true
                        }
                        HomeDestination.Mapa -> navController.navigate(Route.Map()) { launchSingleTop = true }
                        else -> Unit // Remaining tabs arrive in Phase 1.
                    }
                },
            )
        }
        composable<Route.Profile> {
            ProfileScreen(onBack = dropUnlessResumed { navController.popBackStack() })
        }
        composable<Route.Map> { entry ->
            val route = entry.toRoute<Route.Map>()
            MapRoute(
                onBack = dropUnlessResumed { navController.popBackStack() },
                initialFilter = if (route.adoptionsOnly) MapFilter.Adoptions else MapFilter.All,
            )
        }
        composable<Route.Notifications> {
            NotificationsRoute(onBack = dropUnlessResumed { navController.popBackStack() })
        }
    }
}

/** Navigates to [route] and drops the whole back stack, e.g. after signing in or out. */
private fun NavHostController.navigateClearingStack(route: Route) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
