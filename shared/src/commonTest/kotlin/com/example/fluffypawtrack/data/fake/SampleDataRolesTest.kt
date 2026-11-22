package com.example.fluffypawtrack.data.fake

import com.example.fluffypawtrack.domain.model.Role
import com.example.fluffypawtrack.di.AppContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Fake mode needs data for every role's screens. */
class SampleDataRolesTest {

    @Test
    fun demoAdminSignsInWithTheDemoPassword() = runTest {
        val admin = FakeAuthRepository().signIn("admin@correo.com", SampleData.DEMO_PASSWORD).getOrThrow()
        assertEquals(Role.Admin, admin.role)
        assertEquals(SampleData.demoAdmin, admin)
    }

    @Test
    fun demoAffiliateHasACaseInProgressAndAnOrganizationRequest() = runTest {
        val container = AppContainer()
        val affiliate = container.authRepository.signIn("patitas@correo.com", SampleData.DEMO_PASSWORD).getOrThrow()

        assertTrue(container.reportRepository.observeCasesOf("org-patitas").first().isNotEmpty())
        assertTrue(container.contactRequestRepository.observeReceived(affiliate.id).first().any { it.recipientOrganizationId == "org-patitas" })
    }

    @Test
    fun adminSeesThePendingAffiliationInFakeMode() = runTest {
        val container = AppContainer()
        container.authRepository.signIn("admin@correo.com", SampleData.DEMO_PASSWORD).getOrThrow()

        val pending = container.organizationRepository.observePendingAffiliations().first()
        assertTrue(pending.any { it.name == "Rescate Animal Norte" })
        container.organizationRepository.reviewAffiliation("org-norte", approve = true).getOrThrow()
        // The requester of the sample affiliation becomes its affiliate.
        container.authRepository.signIn("carlos@correo.com", SampleData.DEMO_PASSWORD).getOrThrow()
        assertEquals("org-norte", container.authRepository.currentUser.value?.organizationId)
        assertEquals(Role.Affiliate, container.authRepository.currentUser.value?.role)
    }
}
