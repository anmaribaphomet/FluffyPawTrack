package com.example.fluffypawtrack.ui.navigation

import com.example.fluffypawtrack.domain.model.Role
import com.example.fluffypawtrack.domain.model.UserProfile

/**
 * Screens opened after signing in or registering, bottom first. With the affiliation intent the
 * form opens over the role home, so leaving it goes back to the home instead of closing the app.
 */
fun routesAfterSignIn(user: UserProfile?, forAffiliation: Boolean): List<Route> =
    listOfNotNull(startRouteFor(user), Route.AffiliationRequest.takeIf { forAffiliation })

/** "Ya tengo cuenta" keeps the affiliation intent of the registration it comes from. */
fun loginRouteFrom(register: Route.Register): Route.Login = Route.Login(forAffiliation = register.forAffiliation)

/** First screen after the splash or sign-in: each role lands on its own home. */
fun startRouteFor(user: UserProfile?): Route = when (user?.role) {
    Role.Affiliate -> Route.AffiliateHome
    Role.Admin -> Route.AdminHome
    Role.User, null -> Route.Home
}
