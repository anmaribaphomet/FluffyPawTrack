package com.example.fluffypawtrack.ui.affiliate

import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.domain.model.ListingRules
import com.example.fluffypawtrack.domain.model.ListingStatus
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ListingViewModelsTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun <T> TestScope.collecting(state: StateFlow<T>): StateFlow<T> {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
        return state
    }

    private fun AffiliateFakes.editor(listingId: String? = null) = ListingEditorViewModel(listingId, auth, adoptions)

    private fun ListingEditorViewModel.fillAll() {
        onNameChange("Rocky")
        onSpeciesChange("Perro")
        onBreedChange("Mestizo")
        onAgeChange("3 años")
        onVaccinationChange("Esquema completo")
        onDescriptionChange("Tranquilo y cariñoso.")
        onPhotoAdded("content://photo/1")
    }

    // --- Mis fichas ---

    @Test
    fun myListingsShowEveryAnimalOfTheCenter() = runTest {
        val fakes = AffiliateFakes()
        val state = collecting(MyListingsViewModel(fakes.auth, fakes.adoptions).uiState).value

        assertEquals(listOf("listing-1", "listing-2", "listing-3", "listing-4"), state.listings.map { it.id })
        assertNull(state.blockedMessage)
    }

    @Test
    fun markAdoptedChangesTheStatus() = runTest {
        val fakes = AffiliateFakes()
        val vm = MyListingsViewModel(fakes.auth, fakes.adoptions)
        val state = collecting(vm.uiState)

        vm.markAdopted("listing-1")

        assertEquals(ListingStatus.Adopted, state.value.listings.single { it.id == "listing-1" }.status)
        assertNull(state.value.message)
    }

    @Test
    fun markAdoptedTwiceShowsTheRepositoryError() = runTest {
        val fakes = AffiliateFakes()
        val vm = MyListingsViewModel(fakes.auth, fakes.adoptions)
        val state = collecting(vm.uiState)

        vm.markAdopted("listing-4")

        assertEquals("Este animal ya fue marcado como adoptado", state.value.message)
    }

    // --- Editor ---

    @Test
    fun eachRequiredFieldIsValidated() = runTest {
        val vm = AffiliateFakes().editor()

        vm.onSave()

        val errors = vm.uiState.value.errors
        assertEquals(REQUIRED_FIELD, errors.name)
        assertEquals(REQUIRED_FIELD, errors.species)
        assertEquals(REQUIRED_FIELD, errors.breed)
        assertEquals(REQUIRED_FIELD, errors.age)
        assertEquals(REQUIRED_FIELD, errors.vaccination)
        assertEquals(REQUIRED_FIELD, errors.description)
        assertEquals(ListingRules.MISSING_PHOTO, errors.photos)
        assertNull(vm.uiState.value.saved)
    }

    @Test
    fun atLeastOnePhotoIsRequired() = runTest {
        val vm = AffiliateFakes().editor()
        vm.fillAll()
        vm.onPhotoRemoved("content://photo/1")

        vm.onSave()

        assertEquals(ListingRules.MISSING_PHOTO, vm.uiState.value.errors.photos)
        assertNull(vm.uiState.value.errors.name)
        assertNull(vm.uiState.value.saved)
    }

    @Test
    fun createPublishesAnAvailableListing() = runTest {
        val fakes = AffiliateFakes()
        val vm = fakes.editor()
        vm.fillAll()
        vm.onPhotoAdded("content://photo/2")

        vm.onSave()

        val saved = assertNotNull(vm.uiState.value.saved)
        assertEquals("Rocky", saved.name)
        assertEquals(listOf("content://photo/1", "content://photo/2"), saved.photoUrls)
        assertEquals(ListingStatus.Available, saved.status)
        assertTrue(fakes.adoptions.observeListingsBy("org-patitas").first().any { it.id == saved.id })
    }

    @Test
    fun editLoadsTheListingAndUpdatesIt() = runTest {
        val fakes = AffiliateFakes()
        val vm = fakes.editor("listing-2")
        collecting(vm.uiState)

        assertEquals("Michi", vm.uiState.value.name)
        assertEquals(SampleData.adoptionListings[1].photoUrls, vm.uiState.value.photos)
        assertTrue(vm.uiState.value.isEditing)

        vm.onAgeChange("9 meses")
        vm.onSave()

        assertEquals("9 meses", fakes.adoptions.getListing("listing-2")?.age)
        assertNotNull(vm.uiState.value.saved)
    }

    @Test
    fun nonCenterAccountGetsTheServerError() = runTest {
        val shelter = SampleData.demoAffiliate.copy(organizationId = "org-huellitas")
        val vm = AffiliateFakes(shelter).editor()
        vm.fillAll()

        vm.onSave()

        assertEquals("Solo los centros de adopción afiliados pueden publicar animales", vm.uiState.value.message)
        assertFalse(vm.uiState.value.isSaving)
    }
}
