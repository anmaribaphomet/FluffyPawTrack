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
import androidx.compose.runtime.rememberCoroutineScope
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.domain.model.OrganizationType
import com.example.fluffypawtrack.ui.account.FavoritesRoute
import com.example.fluffypawtrack.ui.adoptions.AdoptionFormRoute
import com.example.fluffypawtrack.ui.adoptions.AnimalDetailRoute
import com.example.fluffypawtrack.ui.adoptions.CenterDetailRoute
import com.example.fluffypawtrack.ui.components.PillActionButton
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.ic_heart
import fluffypawtrack.shared.generated.resources.ic_home
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
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
                onListingClick = { id -> navController.navigate(Route.AnimalDetail(id)) },
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
                placeActions = { organization ->
                    if (organization.type == OrganizationType.AdoptionCenter || organization.type == OrganizationType.Shelter) {
                        PillActionButton(
                            text = "Ver centro",
                            icon = painterResource(Res.drawable.ic_home),
                            onClick = { navController.navigate(Route.CenterDetail(organization.id)) },
                        )
                    }
                },
                adoptionActions = { listing ->
                    PillActionButton(
                        text = "Ver ficha",
                        icon = painterResource(Res.drawable.ic_heart),
                        onClick = { navController.navigate(Route.AnimalDetail(listing.id)) },
                    )
                },
            )
        }
        composable<Route.CenterDetail> { entry ->
            CenterDetailRoute(
                organizationId = entry.toRoute<Route.CenterDetail>().organizationId,
                onBack = dropUnlessResumed { navController.popBackStack() },
                onListingClick = { id -> navController.navigate(Route.AnimalDetail(id)) },
                onAdoptionForm = { orgId, listingId -> navController.navigate(Route.AdoptionForm(orgId, listingId)) },
            )
        }
        composable<Route.AnimalDetail> { entry ->
            val adoptionRepository = LocalAppContainer.current.adoptionRepository
            val scope = rememberCoroutineScope()
            AnimalDetailRoute(
                listingId = entry.toRoute<Route.AnimalDetail>().listingId,
                onBack = dropUnlessResumed { navController.popBackStack() },
                onCenterClick = { orgId -> navController.navigate(Route.CenterDetail(orgId)) },
                onAdopt = { listingId ->
                    scope.launch {
                        val listing = adoptionRepository.getListing(listingId) ?: return@launch
                        navController.navigate(Route.AdoptionForm(listing.organizationId, listingId))
                    }
                },
            )
        }
        composable<Route.AdoptionForm> { entry ->
            val route = entry.toRoute<Route.AdoptionForm>()
            AdoptionFormRoute(
                organizationId = route.organizationId,
                listingId = route.listingId,
                onBack = dropUnlessResumed { navController.popBackStack() },
                onDone = { navController.popBackStack() },
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
