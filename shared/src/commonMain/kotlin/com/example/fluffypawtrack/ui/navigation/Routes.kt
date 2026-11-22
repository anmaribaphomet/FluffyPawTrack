package com.example.fluffypawtrack.ui.navigation

import kotlinx.serialization.Serializable

/** Type-safe navigation destinations. New screens add their route here. */
sealed interface Route {
    @Serializable
    data object Splash : Route

    @Serializable
    data object Login : Route

    @Serializable
    data object Register : Route

    @Serializable
    data object Home : Route

    @Serializable
    data object Profile : Route

    @Serializable
    data object Notifications : Route
}
