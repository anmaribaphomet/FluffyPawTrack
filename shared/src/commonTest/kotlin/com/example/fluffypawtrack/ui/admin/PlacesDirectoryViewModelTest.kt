package com.example.fluffypawtrack.ui.admin

import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.domain.model.OrganizationType
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PlacesDirectoryViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun listsEveryPlaceIncludingPendingAndInactiveOnes() = runTest {
        val fixture = AdminFixture()
        val vm = PlacesDirectoryViewModel(fixture.auth, fixture.organizations)

        val ids = vm.uiState.value.places.map { it.id }
        assertEquals(SampleData.organizations.size, ids.size)
        assertTrue("org-norte" in ids && "org-vet-sur" in ids)
    }

    @Test
    fun filtersByTypeAndByStatus() = runTest {
        val fixture = AdminFixture()
        val vm = PlacesDirectoryViewModel(fixture.auth, fixture.organizations)

        vm.onTypeFilter(OrganizationType.Vet)
        assertTrue(vm.uiState.value.places.isNotEmpty())
        assertTrue(vm.uiState.value.places.all { it.type == OrganizationType.Vet })

        vm.onStatusFilter(PlaceStatus.Inactive)
        assertEquals(listOf("org-vet-sur"), vm.uiState.value.places.map { it.id })

        vm.onTypeFilter(null)
        vm.onStatusFilter(PlaceStatus.Pending)
        assertEquals(listOf("org-norte"), vm.uiState.value.places.map { it.id })

        fixture.organizations.reviewAffiliation("org-norte", approve = false)
        vm.onStatusFilter(PlaceStatus.Rejected)
        assertEquals(listOf("org-norte"), vm.uiState.value.places.map { it.id })

        vm.onStatusFilter(PlaceStatus.Active)
        assertTrue(vm.uiState.value.places.all { it.placeStatus == PlaceStatus.Active })
        assertTrue(vm.uiState.value.places.none { it.id == "org-vet-sur" || it.id == "org-norte" })
    }

    @Test
    fun togglingActiveShowsOrHidesThePlaceOnTheMap() = runTest {
        val fixture = AdminFixture()
        val vm = PlacesDirectoryViewModel(fixture.auth, fixture.organizations)

        vm.toggleActive("org-vet-sur")
        assertTrue(fixture.organizations.observeActiveOrganizations().first().any { it.id == "org-vet-sur" })
        assertEquals("Veterinaria del Sur ya aparece en el mapa.", vm.uiState.value.notice)

        vm.toggleActive("org-petcare")
        assertFalse(fixture.organizations.observeActiveOrganizations().first().any { it.id == "org-petcare" })
        assertEquals(PlaceStatus.Inactive, vm.uiState.value.places.single { it.id == "org-petcare" }.placeStatus)
    }

    @Test
    fun deletingAsksForConfirmationAndSendsItsAffiliatesBackToUsers() = runTest {
        val fixture = AdminFixture()
        val vm = PlacesDirectoryViewModel(fixture.auth, fixture.organizations)

        vm.requestDelete("org-patitas")
        assertEquals("org-patitas", assertNotNull(vm.uiState.value.confirmingDelete).id)
        vm.dismissDelete()
        assertNull(vm.uiState.value.confirmingDelete)
        assertNotNull(fixture.organizations.getOrganization("org-patitas"))

        vm.requestDelete("org-patitas")
        vm.confirmDelete()

        assertNull(fixture.organizations.getOrganization("org-patitas"))
        assertTrue(vm.uiState.value.places.none { it.id == "org-patitas" })
        assertEquals("Se eliminó Patitas Callejeras HMO del directorio.", vm.uiState.value.notice)
        val formerAffiliate = assertNotNull(fixture.auth.findUser(SampleData.demoAffiliate.id))
        assertEquals(Role.User, formerAffiliate.role)
        assertNull(formerAffiliate.organizationId)
    }

    @Test
    fun showsTheRepositoryErrorWhenDeleteFails() = runTest {
        val fixture = AdminFixture()
        val vm = PlacesDirectoryViewModel(fixture.auth, fixture.organizations)

        vm.requestDelete("org-zaragoza")
        fixture.organizations.deleteOrganization("org-zaragoza")
        vm.confirmDelete()

        assertEquals("La organización no existe", vm.uiState.value.message)
    }

    @Test
    fun aNonAdminSeesABlockedStateAndNoPlaces() = runTest {
        val fixture = AdminFixture(user = SampleData.demoAffiliate)
        val vm = PlacesDirectoryViewModel(fixture.auth, fixture.organizations)

        assertEquals(ADMIN_ONLY_MESSAGE, vm.uiState.value.blockedMessage)
        assertTrue(vm.uiState.value.places.isEmpty())
    }
}
