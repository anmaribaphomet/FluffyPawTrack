package com.example.fluffypawtrack.ui.common

import com.example.fluffypawtrack.domain.model.OrganizationType
import com.example.fluffypawtrack.domain.model.ReportCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class LabelsTest {

    @Test
    fun formatsDateTimeInHermosilloTime() {
        // 18:20 UTC = 11:20 in Hermosillo (UTC−7).
        assertEquals("21 nov 2026, 11:20", formatDateTime(Instant.parse("2026-11-21T18:20:00Z")))
    }

    @Test
    fun earlyUtcMorningIsStillThePreviousDayInHermosillo() {
        assertEquals("31 dic 2025, 20:05", formatDateTime(Instant.parse("2026-01-01T03:05:00Z")))
    }

    @Test
    fun handlesLeapDay() {
        assertEquals("29 feb 2028, 5:00", formatDateTime(Instant.parse("2028-02-29T12:00:00Z")))
    }

    @Test
    fun criticalCategoryIsShownAsEstadoCritico() {
        assertEquals("Estado crítico", ReportCategory.Critical.label)
    }

    @Test
    fun vetTypeIsShownAsVeterinaria() {
        assertEquals("Veterinaria", OrganizationType.Vet.label)
    }

    @Test
    fun everyCategoryHasADistinctPinColor() {
        val colors = ReportCategory.entries.map { it.pinColor }
        assertEquals(colors.size, colors.toSet().size)
    }
}
