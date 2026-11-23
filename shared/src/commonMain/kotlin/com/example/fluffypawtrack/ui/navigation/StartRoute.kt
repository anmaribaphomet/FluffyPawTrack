package com.example.fluffypawtrack.ui.navigation

import com.example.fluffypawtrack.domain.model.Role
import com.example.fluffypawtrack.domain.model.UserProfile

/** First screen after the splash or sign-in: each role lands on its own home. */
fun startRouteFor(user: UserProfile?): Route = when (user?.role) {
    Role.Affiliate -> Route.AffiliateHome
    Role.Admin -> Route.AdminHome
    Role.User, null -> Route.Home
}
