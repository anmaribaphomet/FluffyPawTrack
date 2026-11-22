package com.example.fluffypawtrack.ui.adoptions

import com.example.fluffypawtrack.data.fake.FakeAdoptionRepository
import com.example.fluffypawtrack.data.fake.FakeAuthRepository
import com.example.fluffypawtrack.data.fake.FakeFavoritesRepository
import com.example.fluffypawtrack.data.fake.FakeOrganizationRepository
import com.example.fluffypawtrack.data.fake.SampleData
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
class AdoptionViewModelsTest {

    private val demo = SampleData.demoUser

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun <T> TestScope.collecting(state: StateFlow<T>): StateFlow<T> {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
        return state
    }

    // Center detail

    @Test
    fun centerDetailShowsTheCenterAndOnlyItsAvailableListings() = runTest {
        val state = collecting(
            CenterDetailViewModel("org-patitas", FakeOrganizationRepository(), FakeAdoptionRepository()).uiState,
        ).value

        assertEquals("org-patitas", state.organization?.id)
        val expected = SampleData.adoptionListings
            .filter { it.organizationId == "org-patitas" && it.status == ListingStatus.Available }
            .map { it.id }
        assertEquals(expected, state.listings.map { it.id })
        assertTrue(SampleData.adoptionListings.any { it.organizationId == "org-patitas" && it.status == ListingStatus.Adopted })
        assertTrue(state.loaded)
    }

    @Test
    fun centerDetailOfAnotherCenterHasNoListingsFromPatitas() = runTest {
        val state = collecting(
            CenterDetailViewModel("org-imbpa", FakeOrganizationRepository(), FakeAdoptionRepository()).uiState,
        ).value
        assertEquals("org-imbpa", state.organization?.id)
        assertTrue(state.listings.isEmpty())
    }

    @Test
    fun centerDetailOfUnknownCenterIsLoadedWithoutOrganization() = runTest {
        val state = collecting(
            CenterDetailViewModel("missing", FakeOrganizationRepository(), FakeAdoptionRepository()).uiState,
        ).value
        assertNull(state.organization)
        assertTrue(state.loaded)
    }

    // Animal detail

    private fun animalViewModel(auth: FakeAuthRepository, favorites: FakeFavoritesRepository) =
        AnimalDetailViewModel("listing-1", FakeAdoptionRepository(), FakeOrganizationRepository(), auth, favorites)

    @Test
    fun animalDetailShowsTheListingAndItsCenterName() = runTest {
        val state = collecting(
            animalViewModel(FakeAuthRepository(initialUser = demo), FakeFavoritesRepository()).uiState,
        ).value
        assertEquals("Canela", state.listing?.name)
        assertEquals("org-patitas", state.organizationId)
        assertEquals(SampleData.organizations.first { it.id == "org-patitas" }.name, state.centerName)
        assertFalse(state.isFavorite)
    }

    @Test
    fun signedInUserTogglesTheFavoriteOnAndOff() = runTest {
        val favorites = FakeFavoritesRepository()
        val viewModel = animalViewModel(FakeAuthRepository(initialUser = demo), favorites)
        val state = collecting(viewModel.uiState)

        viewModel.toggleFavorite()
        assertTrue(state.value.isFavorite)
        assertEquals(setOf("listing-1"), favorites.observeFavoriteIds(demo.id).first())

        viewModel.toggleFavorite()
        assertFalse(state.value.isFavorite)
        assertEquals(emptySet(), favorites.observeFavoriteIds(demo.id).first())
        assertNull(state.value.accountMessage)
    }

    @Test
    fun anonymousUserCannotToggleAndGetsTheAccountMessage() = runTest {
        val auth = FakeAuthRepository()
        val anonymous = auth.signInAnonymously().getOrThrow()
        val favorites = FakeFavoritesRepository()
        val viewModel = animalViewModel(auth, favorites)
        val state = collecting(viewModel.uiState)

        viewModel.toggleFavorite()

        assertFalse(state.value.isFavorite)
        assertEquals(ACCOUNT_REQUIRED_MESSAGE, state.value.accountMessage)
        assertEquals(emptySet(), favorites.observeFavoriteIds(anonymous.id).first())

        viewModel.dismissAccountMessage()
        assertNull(state.value.accountMessage)
    }

    @Test
    fun signedOutUserCannotToggleAndGetsTheAccountMessage() = runTest {
        val viewModel = animalViewModel(FakeAuthRepository(), FakeFavoritesRepository())
        val state = collecting(viewModel.uiState)

        viewModel.toggleFavorite()

        assertFalse(state.value.isFavorite)
        assertEquals(ACCOUNT_REQUIRED_MESSAGE, state.value.accountMessage)
    }

    // Adoption form

    private fun formViewModel(auth: FakeAuthRepository = FakeAuthRepository(), listingId: String? = "listing-1") =
        AdoptionFormViewModel("org-patitas", listingId, auth, FakeOrganizationRepository(), FakeAdoptionRepository())

    private val validMessage = "Tengo patio grande y mucho tiempo para pasear."

    @Test
    fun formPrefillsNameEmailAndPhoneFromTheSignedInUser() = runTest {
        val state = collecting(formViewModel(FakeAuthRepository(initialUser = demo)).uiState).value
        assertEquals("${demo.firstName} ${demo.lastName}", state.name)
        assertEquals(demo.email.orEmpty(), state.email)
        assertEquals(demo.phone.orEmpty(), state.phone)
        assertEquals("Canela", state.listingName)
    }

    @Test
    fun formIsEmptyWithoutASession() = runTest {
        val state = collecting(formViewModel().uiState).value
        assertEquals("", state.name)
        assertEquals("", state.email)
        assertEquals("", state.phone)
    }

    @Test
    fun formRequiresAName() = runTest {
        val viewModel = formViewModel()
        val state = collecting(viewModel.uiState)
        viewModel.onPhoneChange("6621234567")
        viewModel.onMessageChange(validMessage)

        viewModel.submit()

        assertEquals("Ingresa tu nombre", state.value.errors.name)
        assertFalse(state.value.submitted)
    }

    @Test
    fun formRequiresAPhoneOrAnEmail() = runTest {
        val viewModel = formViewModel()
        val state = collecting(viewModel.uiState)
        viewModel.onNameChange("Ana")
        viewModel.onMessageChange(validMessage)

        viewModel.submit()

        assertEquals("Ingresa un teléfono o un correo", state.value.errors.contact)
        assertFalse(state.value.submitted)

        viewModel.onEmailChange("ana@correo.com")
        viewModel.submit()
        assertNull(state.value.errors.contact)
        assertTrue(state.value.submitted)
    }

    @Test
    fun formRejectsAMessageShorterThanTwentyCharacters() = runTest {
        val viewModel = formViewModel()
        val state = collecting(viewModel.uiState)
        viewModel.onNameChange("Ana")
        viewModel.onPhoneChange("6621234567")
        viewModel.onMessageChange("Quiero adoptar")

        viewModel.submit()

        assertEquals("El mensaje debe tener al menos 20 caracteres", state.value.errors.message)
        assertFalse(state.value.submitted)
    }

    @Test
    fun validFormSubmitsWithHousingAndOtherPets() = runTest {
        val viewModel = formViewModel(FakeAuthRepository(initialUser = demo))
        val state = collecting(viewModel.uiState)
        viewModel.onHousingChange(Housing.Apartment)
        viewModel.onHasOtherPetsChange(true)
        viewModel.onMessageChange(validMessage)

        viewModel.submit()

        assertNotNull(state.value)
        assertFalse(state.value.errors.hasErrors)
        assertEquals(Housing.Apartment, state.value.housing)
        assertTrue(state.value.hasOtherPets)
        assertTrue(state.value.submitted)
    }

    @Test
    fun formWithoutListingStillNamesTheCenter() = runTest {
        val state = collecting(formViewModel(listingId = null).uiState).value
        assertNull(state.listingName)
        assertEquals(SampleData.organizations.first { it.id == "org-patitas" }.name, state.centerName)
    }
}
