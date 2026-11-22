package com.example.fluffypawtrack.data.fake

import com.example.fluffypawtrack.domain.model.OrganizationStatus
import com.example.fluffypawtrack.domain.model.OrganizationType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FakeAffiliationRequestTest {

    @Test
    fun affiliationRequestIsPendingAndStaysOffTheMap() = runTest {
        val repository = FakeOrganizationRepository()
        val requested = repository.requestAffiliation(
            type = OrganizationType.Shelter,
            name = "Rescate Huellas Sonora",
            address = "Calle Reforma 10, Hermosillo",
            phone = "6620009999",
            email = "Rescate@Huellas.example",
            description = "Rescatistas independientes del sur de la ciudad.",
        ).getOrThrow()

        assertEquals(OrganizationStatus.Pending, requested.status)
        assertEquals(OrganizationType.Shelter, requested.type)
        assertEquals("rescate@huellas.example", requested.email)
        assertEquals(requested, repository.getOrganization(requested.id))
        assertFalse(repository.observeActiveOrganizations().first().any { it.id == requested.id })
    }

    @Test
    fun affiliationRequestNeedsEveryField() = runTest {
        val result = FakeOrganizationRepository().requestAffiliation(
            OrganizationType.AdoptionCenter, "Nombre", "", "6620009999", "a@b.example", "Descripción",
        )
        assertTrue(result.isFailure)
        assertEquals("Completa todos los datos de la organización", result.exceptionOrNull()?.message)
    }

    @Test
    fun closeReportMovesItToClosedAndOffTheMap() = runTest {
        val repository = FakeReportRepository()
        assertEquals(
            com.example.fluffypawtrack.domain.model.ReportStatus.Closed,
            repository.closeReport("report-2").getOrThrow().status,
        )
        assertFalse(repository.observeActiveReports().first().any { it.id == "report-2" })
        assertTrue(repository.closeReport("missing").isFailure)
    }
}
