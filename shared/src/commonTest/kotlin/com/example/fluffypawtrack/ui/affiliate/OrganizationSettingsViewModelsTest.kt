package com.example.fluffypawtrack.ui.affiliate

import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.domain.model.OrganizationRules
import com.example.fluffypawtrack.domain.model.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OrganizationSettingsViewModelsTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun <T> TestScope.collecting(state: StateFlow<T>): StateFlow<T> {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
        return state
    }

    // --- Perfil de la organización ---

    @Test
    fun profileLoadsTheOwnOrganization() = runTest {
        val fakes = AffiliateFakes()
        val state = OrganizationProfileViewModel(fakes.auth, fakes.organizations).uiState.value

        assertEquals("Patitas Callejeras HMO", state.organization?.name)
        assertEquals("Sábados y domingos, 10:00 a 14:00", state.schedule)
        assertEquals("res:sample_place_adoption_center", state.photoRef)
    }

    @Test
    fun profileNeedsAPhoto() = runTest {
        val fakes = AffiliateFakes()
        val vm = OrganizationProfileViewModel(fakes.auth, fakes.organizations)

        vm.onPhotoRemoved()
        vm.onSave()

        assertEquals(OrganizationRules.MISSING_PHOTO, vm.uiState.value.photoError)
        assertFalse(vm.uiState.value.saved)
        assertEquals("res:sample_place_adoption_center", fakes.organizations.getOrganization("org-patitas")?.photoUrl)
    }

    @Test
    fun profileSavesTheEditableFields() = runTest {
        val fakes = AffiliateFakes()
        val vm = OrganizationProfileViewModel(fakes.auth, fakes.organizations)

        vm.onScheduleChange("Lunes a viernes, 9:00 a 18:00")
        vm.onPhoneChange("6620009999")
        vm.onSocialChange("")
        vm.onPhotoSelected("content://logo")
        vm.onSave()

        val saved = fakes.organizations.getOrganization("org-patitas")
        assertEquals("Lunes a viernes, 9:00 a 18:00", saved?.schedule)
        assertEquals("6620009999", saved?.phone)
        assertNull(saved?.social)
        assertEquals("content://logo", saved?.photoUrl)
        assertTrue(vm.uiState.value.saved)
        assertNull(vm.uiState.value.message)
    }

    // --- Preferencias de notificación ---

    @Test
    fun preferencesStartWithTheDefaults() = runTest {
        val fakes = AffiliateFakes()
        val state = collecting(NotificationPreferencesViewModel(fakes.auth, fakes.prefs).uiState).value

        assertEquals(5, state.radiusKm)
        assertEquals(Priority.entries.toSet(), state.priorities)
        assertTrue(state.pushEnabled)
    }

    @Test
    fun preferencesAreSaved() = runTest {
        val fakes = AffiliateFakes()
        val vm = NotificationPreferencesViewModel(fakes.auth, fakes.prefs)
        collecting(vm.uiState)

        vm.onRadiusChange(12.4f)
        vm.togglePriority(Priority.Low)
        vm.onPushChange(false)
        vm.onSave()

        val saved = fakes.prefs.observe(SampleData.demoAffiliate.id).first()
        assertEquals(12.0, saved.radiusKm)
        assertEquals(setOf(Priority.Medium, Priority.High, Priority.Urgent), saved.priorities)
        assertFalse(saved.pushEnabled)
        assertTrue(vm.uiState.value.saved)
    }

    @Test
    fun atLeastOnePriorityIsRequired() = runTest {
        val fakes = AffiliateFakes()
        val vm = NotificationPreferencesViewModel(fakes.auth, fakes.prefs)
        collecting(vm.uiState)

        Priority.entries.forEach(vm::togglePriority)
        vm.onSave()

        assertEquals(MISSING_PRIORITY, vm.uiState.value.message)
        assertFalse(vm.uiState.value.saved)
    }

    @Test
    fun radiusStaysBetween1And20Km() = runTest {
        val fakes = AffiliateFakes()
        val vm = NotificationPreferencesViewModel(fakes.auth, fakes.prefs)
        collecting(vm.uiState)

        vm.onRadiusChange(0f)
        assertEquals(1, vm.uiState.value.radiusKm)
        vm.onRadiusChange(40f)
        assertEquals(20, vm.uiState.value.radiusKm)
    }

    // --- Inicio del afiliado ---

    @Test
    fun homeShowsTheOrganizationNameAndCenterShortcuts() = runTest {
        val fakes = AffiliateFakes()
        val state = collecting(AffiliateHomeViewModel(fakes.auth, fakes.organizations, fakes.reports).uiState).value

        assertEquals("Patitas Callejeras HMO", state.organizationName)
        assertTrue(state.isAdoptionCenter)
        assertEquals(1, state.openCases)
    }

    @Test
    fun shelterHomeHidesAdoptionShortcuts() = runTest {
        val fakes = AffiliateFakes(SampleData.demoAffiliate.copy(organizationId = "org-huellitas"))
        val state = collecting(AffiliateHomeViewModel(fakes.auth, fakes.organizations, fakes.reports).uiState).value

        assertEquals("Refugio Huellitas del Desierto", state.organizationName)
        assertFalse(state.isAdoptionCenter)
    }
}
