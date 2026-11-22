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
import com.example.fluffypawtrack.ui.account.FavoritesRoute
import com.example.fluffypawtrack.ui.account.MyReportsRoute
import com.example.fluffypawtrack.ui.account.PersonalInfoRoute
import com.example.fluffypawtrack.ui.profile.ProfileRoute
import com.example.fluffypawtrack.ui.report.NewReportRoute
import com.example.fluffypawtrack.ui.settings.SettingsInfoRoute
import com.example.fluffypawtrack.ui.settings.SettingsRoute
import com.example.fluffypawtrack.ui.settings.SettingsTopic

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
                onReportAnonymously = { navController.navigateClearingStack(Route.NewReport) },
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
                onReportAnimal = dropUnlessResumed { navController.navigate(Route.NewReport) },
                onNavigate = { destination ->
                    when (destination) {
                        HomeDestination.Notificaciones -> navController.navigate(Route.Notifications) {
                            launchSingleTop = true
                        }
                        HomeDestination.Inicio -> Unit
                        HomeDestination.Mapa -> navController.navigate(Route.Map()) { launchSingleTop = true }
                        HomeDestination.Nuevo -> navController.navigate(Route.NewReport) { launchSingleTop = true }
                        HomeDestination.Ajustes -> navController.navigate(Route.Settings) { launchSingleTop = true }
                    }
                },
            )
        }
        composable<Route.Profile> {
            ProfileRoute(
                onBack = dropUnlessResumed { navController.popBackStack() },
                onPersonalInfo = dropUnlessResumed { navController.navigate(Route.PersonalInfo) },
                onMyReports = dropUnlessResumed { navController.navigate(Route.MyReports) },
                onFavorites = dropUnlessResumed { navController.navigate(Route.Favorites) },
            )
        }
        composable<Route.PersonalInfo> {
            PersonalInfoRoute(onBack = dropUnlessResumed { navController.popBackStack() })
        }
        composable<Route.MyReports> {
            MyReportsRoute(onBack = dropUnlessResumed { navController.popBackStack() })
        }
        composable<Route.Favorites> {
            FavoritesRoute(
                onBack = dropUnlessResumed { navController.popBackStack() },
                // The animal detail screen arrives with T5; until then open adoptions on the map.
                onListingClick = { navController.navigate(Route.Map(adoptionsOnly = true)) },
            )
        }
        composable<Route.NewReport> {
            NewReportRoute(
                onBack = dropUnlessResumed { navController.popBackStack() },
                onSeeMap = {
                    navController.navigate(Route.Map()) {
                        popUpTo<Route.NewReport> { inclusive = true }
                    }
                },
            )
        }
        composable<Route.Settings> {
            SettingsRoute(
                onBack = dropUnlessResumed { navController.popBackStack() },
                onSignedOut = { navController.navigateClearingStack(Route.Login) },
                onTopicClick = { topic -> navController.navigate(Route.SettingsInfo(topic.name)) },
            )
        }
        composable<Route.SettingsInfo> { entry ->
            SettingsInfoRoute(
                topic = SettingsTopic.valueOf(entry.toRoute<Route.SettingsInfo>().topic),
                onBack = dropUnlessResumed { navController.popBackStack() },
            )
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
