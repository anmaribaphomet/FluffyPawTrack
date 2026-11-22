package com.example.fluffypawtrack.ui.common

import com.example.fluffypawtrack.domain.model.GeoPoint
import kotlin.test.Test
import kotlin.test.assertEquals

class ExternalLinksTest {

    @Test
    fun directionsUrlUsesTheGoogleMapsDirFormat() {
        assertEquals(
            "https://www.google.com/maps/dir/?api=1&destination=29.0729,-110.9559",
            directionsUrl(GeoPoint(29.0729, -110.9559)),
        )
    }

    @Test
    fun directionsUrlKeepsEveryDecimalWithADotAndNoSpaces() {
        assertEquals(
            "https://www.google.com/maps/dir/?api=1&destination=29.123456789,-110.987654321",
            directionsUrl(GeoPoint(29.123456789, -110.987654321)),
        )
    }

    @Test
    fun directionsUrlNeverUsesScientificNotation() {
        assertEquals(
            "https://www.google.com/maps/dir/?api=1&destination=0.00001,-0.0000005",
            directionsUrl(GeoPoint(0.00001, -0.0000005)),
        )
    }

    @Test
    fun phoneUriRemovesSpaces() {
        assertEquals("tel:6621234567", phoneUri("662 123 4567"))
    }

    @Test
    fun emailUriUsesMailto() {
        assertEquals("mailto:adopciones@patitas.example", emailUri(" adopciones@patitas.example "))
    }

    @Test
    fun socialUrlTurnsABareHandleIntoAnInstagramProfile() {
        assertEquals("https://www.instagram.com/patitascallejerashmo/", socialUrl("patitascallejerashmo"))
        assertEquals("https://www.instagram.com/patitascallejerashmo/", socialUrl("@patitascallejerashmo"))
    }

    @Test
    fun socialUrlPassesFullUrlsThrough() {
        assertEquals("https://www.facebook.com/patitas", socialUrl("https://www.facebook.com/patitas"))
        assertEquals("http://patitas.example", socialUrl("http://patitas.example"))
    }
}
