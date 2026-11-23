package com.example.fluffypawtrack.ui.admin

import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.domain.model.GeoPoint
import com.example.fluffypawtrack.domain.model.OrganizationStatus
import com.example.fluffypawtrack.domain.model.OrganizationType
import com.example.fluffypawtrack.platform.ReverseGeocoder
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
class PlaceEditorViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun PlaceEditorViewModel.fillValid() {
        onNameChange("Refugio Las Palmas")
        onTypeChange(OrganizationType.SafePoint)
        onLatitudeChange("29.0811")
        onLongitudeChange("-110.9555")
        onAddressChange("Calle Palmas 12, Hermosillo")
        onScheduleChange("Todos los días, 9:00 a 18:00")
        onCapacityChange("15")
        onPhoneChange("662 111 2233")
        onEmailChange("hola@palmas.example")
        onSocialChange("@refugiopalmas")
        onDescriptionChange("Punto seguro con sombra y agua.")
        onPhotoSelected("content://media/picker/0/3")
    }

    @Test
    fun offersTheFourPlaceTypes() {
        val fixture = AdminFixture()
        val vm = PlaceEditorViewModel(fixture.auth, fixture.organizations, organizationId = null)
        assertEquals(OrganizationType.entries, vm.uiState.value.typeOptions)
        assertFalse(vm.uiState.value.isEditing)
    }

    @Test
    fun emptySaveShowsAnErrorPerRequiredFieldAndCreatesNothing() = runTest {
        val fixture = AdminFixture()
        val vm = PlaceEditorViewModel(fixture.auth, fixture.organizations, organizationId = null)
        val before = fixture.organizations.observeAllForAdmin().first()

        vm.onSave()

        val errors = vm.uiState.value.errors
        assertEquals("Ingresa el nombre del lugar", errors.name)
        assertEquals("Ingresa una latitud entre -90 y 90", errors.latitude)
        assertEquals("Ingresa una longitud entre -180 y 180", errors.longitude)
        assertEquals("Ingresa la dirección", errors.address)
        assertEquals("Ingresa el horario", errors.schedule)
        assertNull(errors.capacity)
        assertEquals("Ingresa un teléfono de 10 dígitos", errors.phone)
        assertEquals("Ingresa el correo", errors.email)
        assertEquals("Agrega una descripción del lugar", errors.description)
        assertEquals("Agrega una foto del lugar o su logo", errors.photo)
        assertNull(vm.uiState.value.saved)
        assertEquals(before, fixture.organizations.observeAllForAdmin().first())
    }

    @Test
    fun rejectsInvalidCoordinatesCapacityAndEmail() = runTest {
        val fixture = AdminFixture()
        val vm = PlaceEditorViewModel(fixture.auth, fixture.organizations, organizationId = null)
        vm.fillValid()
        vm.onLatitudeChange("95")
        vm.onLongitudeChange("abc")
        vm.onCapacityChange("-3")
        vm.onEmailChange("hola@")

        vm.onSave()

        val errors = vm.uiState.value.errors
        assertEquals("Ingresa una latitud entre -90 y 90", errors.latitude)
        assertEquals("Ingresa una longitud entre -180 y 180", errors.longitude)
        assertEquals("La capacidad debe ser un número entero de 0 o más", errors.capacity)
        assertEquals("Ingresa un correo válido", errors.email)
        assertNull(vm.uiState.value.saved)
    }

    @Test
    fun aPlaceWithoutPhotoIsNotSaved() = runTest {
        val fixture = AdminFixture()
        val vm = PlaceEditorViewModel(fixture.auth, fixture.organizations, organizationId = null)
        vm.fillValid()
        vm.onPhotoRemoved()

        vm.onSave()

        assertEquals("Agrega una foto del lugar o su logo", vm.uiState.value.errors.photo)
        assertNull(vm.uiState.value.saved)
    }

    @Test
    fun validCreateAddsAnActivePlaceShownOnTheMap() = runTest {
        val fixture = AdminFixture()
        val vm = PlaceEditorViewModel(fixture.auth, fixture.organizations, organizationId = null)
        vm.fillValid()

        vm.onSave()

        val saved = assertNotNull(vm.uiState.value.saved)
        assertEquals("Refugio Las Palmas", saved.name)
        assertEquals(OrganizationType.SafePoint, saved.type)
        assertEquals(GeoPoint(29.0811, -110.9555), saved.location)
        assertEquals(15, saved.capacity)
        assertEquals("6621112233", saved.phone)
        assertEquals("@refugiopalmas", saved.social)
        assertEquals("content://media/picker/0/3", saved.photoUrl)
        assertEquals(OrganizationStatus.Approved, saved.status)
        assertTrue(fixture.organizations.observeActiveOrganizations().first().any { it.id == saved.id })
    }

    @Test
    fun capacityIsOptional() = runTest {
        val fixture = AdminFixture()
        val vm = PlaceEditorViewModel(fixture.auth, fixture.organizations, organizationId = null)
        vm.fillValid()
        vm.onCapacityChange("")

        vm.onSave()

        assertNull(assertNotNull(vm.uiState.value.saved).capacity)
    }

    @Test
    fun editingLoadsThePlaceAndUpdatesItWithoutChangingItsVisibility() = runTest {
        val fixture = AdminFixture()
        val vm = PlaceEditorViewModel(fixture.auth, fixture.organizations, organizationId = "org-vet-sur")

        val loaded = vm.uiState.value
        assertTrue(loaded.isEditing)
        assertEquals("Veterinaria del Sur", loaded.name)
        assertEquals(OrganizationType.Vet, loaded.type)
        assertEquals("res:sample_place_vet_clinic", loaded.photoRef)

        vm.onScheduleChange("Lunes a sábado, 9:00 a 19:00")
        vm.onSave()

        val updated = assertNotNull(fixture.organizations.getOrganization("org-vet-sur"))
        assertEquals("Lunes a sábado, 9:00 a 19:00", updated.schedule)
        assertFalse(updated.active)
        assertEquals(updated, vm.uiState.value.saved)
    }

    @Test
    fun editingAnUnknownPlaceShowsItIsMissing() = runTest {
        val fixture = AdminFixture()
        val vm = PlaceEditorViewModel(fixture.auth, fixture.organizations, organizationId = "org-missing")
        assertEquals("El lugar no existe", vm.uiState.value.blockedMessage)
    }

    @Test
    fun myLocationFillsTheCoordinatesAndAnEmptyAddress() = runTest {
        val fixture = AdminFixture()
        val vm = PlaceEditorViewModel(
            fixture.auth,
            fixture.organizations,
            organizationId = null,
            reverseGeocoder = ReverseGeocoder { "Blvd. Kino 100, Hermosillo" },
        )

        vm.onLocationResult(GeoPoint(29.1, -110.95))

        assertEquals("29.1", vm.uiState.value.latitude)
        assertEquals("-110.95", vm.uiState.value.longitude)
        assertEquals("Blvd. Kino 100, Hermosillo", vm.uiState.value.address)

        vm.onLocationResult(null)
        assertEquals("No pudimos obtener tu ubicación. Ingresa las coordenadas a mano.", vm.uiState.value.message)
        assertEquals("29.1", vm.uiState.value.latitude)
    }

    @Test
    fun aNonAdminCannotOpenTheEditor() = runTest {
        val fixture = AdminFixture(user = SampleData.demoUser)
        val vm = PlaceEditorViewModel(fixture.auth, fixture.organizations, organizationId = null)
        assertEquals(ADMIN_ONLY_MESSAGE, vm.uiState.value.blockedMessage)
    }
}
