package com.example.fluffypawtrack.data.fake

import com.example.fluffypawtrack.domain.model.OrganizationStatus
import com.example.fluffypawtrack.domain.model.OrganizationType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FakeAffiliationPendingTest {

    @Test
    fun aSentAffiliationRequestJoinsTheAdminPendingList() = runTest {
        val repository = FakeOrganizationRepository()
        val before = repository.observePendingAffiliations().first()

        val requested = repository.requestAffiliation(
            type = OrganizationType.AdoptionCenter,
            name = "Patitas del Norte",
            address = "Blvd. Kino 200, Hermosillo",
            phone = "6621112233",
            email = "contacto@patitas.example",
            description = "Centro de adopción en el norte de la ciudad.",
            photoUrl = "memory://photo/1",
        ).getOrThrow()

        val after = repository.observePendingAffiliations().first()
        assertEquals(before.size + 1, after.size)
        assertEquals(OrganizationStatus.Pending, after.single { it.id == requested.id }.status)
    }
}
