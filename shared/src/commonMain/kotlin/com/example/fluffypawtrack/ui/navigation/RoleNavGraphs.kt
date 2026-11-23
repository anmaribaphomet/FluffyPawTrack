package com.example.fluffypawtrack.ui.navigation

import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.fluffypawtrack.ui.admin.AdminHomeRoute
import com.example.fluffypawtrack.ui.admin.AdminReportDetailRoute
import com.example.fluffypawtrack.ui.admin.AffiliationRequestsRoute
import com.example.fluffypawtrack.ui.admin.PlaceEditorRoute
import com.example.fluffypawtrack.ui.admin.PlacesDirectoryRoute
import com.example.fluffypawtrack.ui.admin.ReportSupervisionRoute
import com.example.fluffypawtrack.ui.affiliate.AffiliateHomeNavigation
import com.example.fluffypawtrack.ui.affiliate.AffiliateHomeRoute
import com.example.fluffypawtrack.ui.affiliate.CaseDetailRoute
import com.example.fluffypawtrack.ui.affiliate.ListingEditorRoute
import com.example.fluffypawtrack.ui.affiliate.MyCasesRoute
import com.example.fluffypawtrack.ui.affiliate.MyListingsRoute
import com.example.fluffypawtrack.ui.affiliate.NotificationPreferencesRoute
import com.example.fluffypawtrack.ui.affiliate.OrganizationProfileRoute
import com.example.fluffypawtrack.ui.affiliate.ReportInboxRoute
import com.example.fluffypawtrack.ui.contact.ContactRequestsRoute
import com.example.fluffypawtrack.ui.contact.ContactRequestsTab

/** Opens [route] once even if the tap repeats before the transition starts. */
private fun NavHostController.open(route: Route) = navigate(route) { launchSingleTop = true }

/** Screens of role `affiliate` (§6 Afiliado). `CenterAdoptionRequests` lives in the main graph. */
internal fun NavGraphBuilder.affiliateScreens(navController: NavHostController) {
    composable<Route.AffiliateHome> {
        AffiliateHomeRoute(
            navigation = AffiliateHomeNavigation(
                onProfileClick = dropUnlessResumed { navController.open(Route.Profile) },
                onInbox = dropUnlessResumed { navController.open(Route.ReportInbox) },
                onMyCases = dropUnlessResumed { navController.open(Route.MyCases) },
                onContactRequests = dropUnlessResumed { navController.open(Route.ReceivedContactRequests) },
                onAdoptionRequests = dropUnlessResumed { navController.open(Route.CenterAdoptionRequests) },
                onMyListings = dropUnlessResumed { navController.open(Route.MyListings) },
                onOrganizationProfile = dropUnlessResumed { navController.open(Route.OrganizationProfile) },
                onPreferences = dropUnlessResumed { navController.open(Route.NotificationPreferences) },
            ),
        )
    }
    composable<Route.ReportInbox> {
        ReportInboxRoute(
            onBack = dropUnlessResumed { navController.popBackStack() },
            onReportClick = { id -> navController.open(Route.CaseDetail(id)) },
        )
    }
    composable<Route.CaseDetail> { entry ->
        CaseDetailRoute(
            reportId = entry.toRoute<Route.CaseDetail>().reportId,
            onBack = dropUnlessResumed { navController.popBackStack() },
        )
    }
    composable<Route.MyCases> {
        MyCasesRoute(
            onBack = dropUnlessResumed { navController.popBackStack() },
            onCaseClick = { id -> navController.open(Route.CaseDetail(id)) },
        )
    }
    composable<Route.ReceivedContactRequests> {
        ContactRequestsRoute(
            onBack = dropUnlessResumed { navController.popBackStack() },
            initialTab = ContactRequestsTab.Received,
        )
    }
    composable<Route.MyListings> {
        MyListingsRoute(
            onBack = dropUnlessResumed { navController.popBackStack() },
            onCreateListing = dropUnlessResumed { navController.open(Route.ListingEditor()) },
            onEditListing = { id -> navController.open(Route.ListingEditor(id)) },
        )
    }
    composable<Route.ListingEditor> { entry ->
        ListingEditorRoute(
            listingId = entry.toRoute<Route.ListingEditor>().listingId,
            onBack = dropUnlessResumed { navController.popBackStack() },
            onSaved = { navController.popBackStack() },
        )
    }
    composable<Route.OrganizationProfile> {
        OrganizationProfileRoute(onBack = dropUnlessResumed { navController.popBackStack() })
    }
    composable<Route.NotificationPreferences> {
        NotificationPreferencesRoute(onBack = dropUnlessResumed { navController.popBackStack() })
    }
}

/** Screens of role `admin` (§6 Administrador). */
internal fun NavGraphBuilder.adminScreens(navController: NavHostController) {
    composable<Route.AdminHome> {
        AdminHomeRoute(
            onProfileClick = dropUnlessResumed { navController.open(Route.Profile) },
            onAffiliationRequests = dropUnlessResumed { navController.open(Route.AffiliationRequests) },
            onPlacesDirectory = dropUnlessResumed { navController.open(Route.PlacesDirectory) },
            onReportSupervision = dropUnlessResumed { navController.open(Route.ReportSupervision) },
        )
    }
    composable<Route.AffiliationRequests> {
        AffiliationRequestsRoute(onBack = dropUnlessResumed { navController.popBackStack() })
    }
    composable<Route.PlacesDirectory> {
        PlacesDirectoryRoute(
            onBack = dropUnlessResumed { navController.popBackStack() },
            onAddPlace = dropUnlessResumed { navController.open(Route.PlaceEditor()) },
            onEditPlace = { id -> navController.open(Route.PlaceEditor(id)) },
        )
    }
    composable<Route.PlaceEditor> { entry ->
        PlaceEditorRoute(
            organizationId = entry.toRoute<Route.PlaceEditor>().organizationId,
            onBack = dropUnlessResumed { navController.popBackStack() },
            onSaved = { navController.popBackStack() },
        )
    }
    composable<Route.ReportSupervision> {
        ReportSupervisionRoute(
            onBack = dropUnlessResumed { navController.popBackStack() },
            onReportClick = { id -> navController.open(Route.AdminReportDetail(id)) },
        )
    }
    composable<Route.AdminReportDetail> { entry ->
        AdminReportDetailRoute(
            reportId = entry.toRoute<Route.AdminReportDetail>().reportId,
            onBack = dropUnlessResumed { navController.popBackStack() },
        )
    }
}
