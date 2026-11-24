package com.example.fluffypawtrack.ui.navigation

import com.example.fluffypawtrack.data.fake.SampleData
import kotlin.test.Test
import kotlin.test.assertEquals

class AffiliationIntentTest {

    @Test
    fun signingInWithTheAffiliationIntentOpensTheFormOverTheHome() {
        assertEquals(
            listOf(Route.Home, Route.AffiliationRequest),
            routesAfterSignIn(SampleData.demoUser, forAffiliation = true),
        )
    }

    @Test
    fun signingInWithoutTheIntentOpensOnlyTheRoleHome() {
        assertEquals(listOf(Route.Home), routesAfterSignIn(SampleData.demoUser, forAffiliation = false))
    }

    @Test
    fun theIntentSurvivesFromRegisterToLogin() {
        val register = Route.Register(forAffiliation = true)
        assertEquals(Route.Login(forAffiliation = true), loginRouteFrom(register))
        assertEquals(Route.Login(), loginRouteFrom(Route.Register()))
    }
}
