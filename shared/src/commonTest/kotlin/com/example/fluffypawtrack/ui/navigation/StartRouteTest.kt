package com.example.fluffypawtrack.ui.navigation

import com.example.fluffypawtrack.domain.model.Role
import com.example.fluffypawtrack.domain.model.UserProfile
import kotlin.test.Test
import kotlin.test.assertEquals

class StartRouteTest {

    private fun user(role: Role, anonymous: Boolean = false) = UserProfile(
        id = "u1",
        email = "a@b.com",
        firstName = "Ana",
        lastName = "Pérez",
        phone = null,
        role = role,
        organizationId = null,
        isAnonymous = anonymous,
    )

    @Test
    fun userGoesToHome() {
        assertEquals(Route.Home, startRouteFor(user(Role.User)))
    }

    @Test
    fun anonymousUserGoesToHome() {
        assertEquals(Route.Home, startRouteFor(user(Role.User, anonymous = true)))
    }

    @Test
    fun affiliateGoesToAffiliateHome() {
        assertEquals(Route.AffiliateHome, startRouteFor(user(Role.Affiliate)))
    }

    @Test
    fun adminGoesToAdminHome() {
        assertEquals(Route.AdminHome, startRouteFor(user(Role.Admin)))
    }

    @Test
    fun noUserGoesToHome() {
        assertEquals(Route.Home, startRouteFor(null))
    }
}
