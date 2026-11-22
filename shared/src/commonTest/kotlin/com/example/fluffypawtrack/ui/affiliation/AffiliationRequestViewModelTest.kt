package com.example.fluffypawtrack.ui.affiliation

import com.example.fluffypawtrack.data.fake.FakeOrganizationRepository
import com.example.fluffypawtrack.domain.model.OrganizationStatus
import com.example.fluffypawtrack.domain.model.OrganizationType
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

@OptIn(ExperimentalCoroutinesApi::class)
class AffiliationRequestViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun AffiliationRequestViewModel.fillValid() {
        onNameChange("Rescate Huellas Sonora")
        onTypeChange(OrganizationType.AdoptionCenter)
        onAddressChange("Calle Reforma 10, Hermosillo")
        onPhoneChange("662 000 9999")
        onEmailChange("rescate@huellas.example")
        onDescriptionChange("Rescatistas independientes del sur de la ciudad.")
    }

    @Test
    fun offersShelterAndAdoptionCenterTypes() {
        val vm = AffiliationRequestViewModel(FakeOrganizationRepository())
        assertEquals(listOf(OrganizationType.Shelter, OrganizationType.AdoptionCenter), vm.uiState.value.typeOptions)
        assertEquals(OrganizationType.Shelter, vm.uiState.value.type)
    }

    @Test
    fun emptySubmitShowsAnErrorPerFieldAndSendsNothing() = runTest {
        val repository = FakeOrganizationRepository()
        val vm = AffiliationRequestViewModel(repository)
        val before = repository.observeActiveOrganizations().first()

        vm.onSubmit()

        val errors = vm.uiState.value.errors
        assertEquals("Ingresa el nombre de la organización", errors.name)
        assertEquals("Ingresa la dirección", errors.address)
        assertEquals("Ingresa un teléfono de 10 dígitos", errors.phone)
        assertEquals("Ingresa el correo", errors.email)
        assertEquals("Describe brevemente a tu organización", errors.description)
        assertNull(vm.uiState.value.submitted)
        assertEquals(before, repository.observeActiveOrganizations().first())
    }

    @Test
    fun invalidEmailAndPhoneAreRejected() = runTest {
        val vm = AffiliationRequestViewModel(FakeOrganizationRepository())
        vm.fillValid()
        vm.onEmailChange("rescate@")
        vm.onPhoneChange("12345")
        vm.onSubmit()
        assertEquals("Ingresa un correo válido", vm.uiState.value.errors.email)
        assertEquals("Ingresa un teléfono de 10 dígitos", vm.uiState.value.errors.phone)
        assertNull(vm.uiState.value.submitted)
    }

    @Test
    fun validSubmitCreatesPendingOrganizationOffTheMap() = runTest {
        val repository = FakeOrganizationRepository()
        val vm = AffiliationRequestViewModel(repository)
        vm.fillValid()
        vm.onSubmit()

        val submitted = assertNotNull(vm.uiState.value.submitted)
        assertEquals(OrganizationStatus.Pending, submitted.status)
        assertEquals(OrganizationType.AdoptionCenter, submitted.type)
        assertEquals("6620009999", submitted.phone)
        assertFalse(repository.observeActiveOrganizations().first().any { it.id == submitted.id })
    }
}
