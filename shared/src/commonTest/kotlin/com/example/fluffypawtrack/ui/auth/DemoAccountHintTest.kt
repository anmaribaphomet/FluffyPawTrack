package com.example.fluffypawtrack.ui.auth

import com.example.fluffypawtrack.di.AppContainer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DemoAccountHintTest {
    @Test
    fun hintIsShownOnTheFakeRepositories() {
        assertEquals("Cuenta de prueba: demo@correo.com / demo1234", demoAccountHint(usesSupabase = false))
    }

    @Test
    fun hintIsHiddenOnSupabase() {
        assertNull(demoAccountHint(usesSupabase = true))
    }

    @Test
    fun containerWithoutConfigShowsTheHint() {
        assertEquals(DEMO_ACCOUNT_HINT, demoAccountHint(AppContainer().usesSupabase))
    }
}
