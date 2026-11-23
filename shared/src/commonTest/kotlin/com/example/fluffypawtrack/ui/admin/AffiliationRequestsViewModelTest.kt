package com.example.fluffypawtrack.ui.admin

import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.domain.model.OrganizationStatus
import com.example.fluffypawtrack.domain.model.Role
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AffiliationRequestsViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun listsThePendingAffiliationsAndTheHomeCountsThem() = runTest {
        val fixture = AdminFixture()
        val vm = AffiliationRequestsViewModel(fixture.auth, fixture.organizations)
        val home = AdminHomeViewModel(fixture.auth, fixture.organizations)

        val state = vm.uiState.value
        assertNull(state.blockedMessage)
        assertTrue(state.loaded)
        assertEquals(listOf("org-norte"), state.pending.map { it.id })
        assertEquals(1, home.uiState.value.pendingCount)
        assertNull(home.uiState.value.blockedMessage)
    }

    @Test
    fun approvingAsksForConfirmationThenMakesThePlaceActiveOnTheMap() = runTest {
        val fixture = AdminFixture()
        val vm = AffiliationRequestsViewModel(fixture.auth, fixture.organizations)
        val home = AdminHomeViewModel(fixture.auth, fixture.organizations)

        vm.requestReview("org-norte", approve = true)
        val review = assertNotNull(vm.uiState.value.confirming)
        assertTrue(review.approve)
        assertEquals(OrganizationStatus.Pending, fixture.organizations.getOrganization("org-norte")?.status)

        vm.confirmReview()

        val state = vm.uiState.value
        assertNull(state.confirming)
        assertTrue(state.pending.isEmpty())
        assertEquals("Se aprobó Rescate Animal Norte. Ya aparece en el mapa.", state.notice)
        assertEquals(OrganizationStatus.Approved, fixture.organizations.getOrganization("org-norte")?.status)
        assertTrue(fixture.organizations.observeActiveOrganizations().first().any { it.id == "org-norte" })
        assertEquals(Role.Affiliate, fixture.auth.findUser(SampleData.affiliationRequesters.getValue("org-norte"))?.role)
        assertEquals(0, home.uiState.value.pendingCount)
    }

    @Test
    fun rejectingRemovesItFromPendingAndKeepsItOffTheMap() = runTest {
        val fixture = AdminFixture()
        val vm = AffiliationRequestsViewModel(fixture.auth, fixture.organizations)

        vm.requestReview("org-norte", approve = false)
        vm.confirmReview()

        assertTrue(vm.uiState.value.pending.isEmpty())
        assertEquals("Se rechazó la solicitud de Rescate Animal Norte.", vm.uiState.value.notice)
        assertEquals(OrganizationStatus.Rejected, fixture.organizations.getOrganization("org-norte")?.status)
        assertTrue(fixture.organizations.observeActiveOrganizations().first().none { it.id == "org-norte" })
    }

    @Test
    fun dismissingTheConfirmationChangesNothing() = runTest {
        val fixture = AdminFixture()
        val vm = AffiliationRequestsViewModel(fixture.auth, fixture.organizations)

        vm.requestReview("org-norte", approve = true)
        vm.dismissReview()

        assertNull(vm.uiState.value.confirming)
        assertEquals(listOf("org-norte"), vm.uiState.value.pending.map { it.id })
    }

    @Test
    fun showsTheRepositoryErrorWhenTheRequestWasAlreadyReviewed() = runTest {
        val fixture = AdminFixture()
        val vm = AffiliationRequestsViewModel(fixture.auth, fixture.organizations)

        vm.requestReview("org-norte", approve = true)
        fixture.organizations.reviewAffiliation("org-norte", approve = false)
        vm.confirmReview()

        assertEquals("Esta solicitud de afiliación ya fue revisada", vm.uiState.value.message)
        vm.dismissMessage()
        assertNull(vm.uiState.value.message)
    }

    @Test
    fun aNonAdminSeesABlockedStateAndNoRequests() = runTest {
        val fixture = AdminFixture(user = SampleData.demoUser)
        val vm = AffiliationRequestsViewModel(fixture.auth, fixture.organizations)
        val home = AdminHomeViewModel(fixture.auth, fixture.organizations)

        assertEquals(ADMIN_ONLY_MESSAGE, vm.uiState.value.blockedMessage)
        assertTrue(vm.uiState.value.pending.isEmpty())
        assertEquals(ADMIN_ONLY_MESSAGE, home.uiState.value.blockedMessage)
        assertEquals(0, home.uiState.value.pendingCount)
    }
}
