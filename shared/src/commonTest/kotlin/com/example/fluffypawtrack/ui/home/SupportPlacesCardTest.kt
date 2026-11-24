package com.example.fluffypawtrack.ui.home

import kotlin.test.Test
import kotlin.test.assertEquals

class SupportPlacesCardTest {

    @Test
    fun thirdHomeCardUsesTheRequestedCopy() {
        assertEquals("¿Necesitas apoyo cerca?", SupportPlacesCardCopy.TITLE)
        assertEquals("Encuentra veterinarias, albergues y puntos seguros.", SupportPlacesCardCopy.DESCRIPTION)
        assertEquals("Ver lugares de apoyo", SupportPlacesCardCopy.ACTION)
    }
}
