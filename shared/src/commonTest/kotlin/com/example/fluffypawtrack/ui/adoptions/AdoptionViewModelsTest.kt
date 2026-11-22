package com.example.fluffypawtrack.ui.adoptions

import com.example.fluffypawtrack.data.fake.FakeAdoptionRepository
import com.example.fluffypawtrack.data.fake.FakeAdoptionRequestRepository
import com.example.fluffypawtrack.data.fake.FakeAuthRepository
import com.example.fluffypawtrack.data.fake.FakeFavoritesRepository
import com.example.fluffypawtrack.data.fake.FakeOrganizationRepository
import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.domain.model.AdoptionRequestStatus
import com.example.fluffypawtrack.domain.model.HousingTenure
import com.example.fluffypawtrack.domain.model.HousingType
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

    private fun formViewModel(
        auth: FakeAuthRepository = FakeAuthRepository(initialUser = demo),
        requests: FakeAdoptionRequestRepository = FakeAdoptionRequestRepository(initialRequests = emptyList()),
        listingId: String = "listing-1",
    ) = AdoptionFormViewModel(listingId, auth, FakeAdoptionRepository(), requests)

    private val validReason = "Tengo patio grande y mucho tiempo para pasear."

    @Test
    fun formShowsTheLockedInterestedInTextOfTheListing() = runTest {
        val state = collecting(formViewModel().uiState).value
        assertEquals("Canela · Perro · Mestiza", state.interestedIn)
        assertNull(state.blockedMessage)
    }

    @Test
    fun formPrefillsNameEmailAndPhoneFromTheProfileAndKeepsThemEditable() = runTest {
        val viewModel = formViewModel()
        val state = collecting(viewModel.uiState)
        assertEquals("${demo.firstName} ${demo.lastName}", state.value.fullName)
        assertEquals(demo.email.orEmpty(), state.value.email)
        assertEquals(demo.phone.orEmpty(), state.value.phone)

        viewModel.onPhoneChange("6629998877")
        assertEquals("6629998877", state.value.phone)
    }

    @Test
    fun formRequiresTheReason() = runTest {
        val requests = FakeAdoptionRequestRepository(initialRequests = emptyList())
        val viewModel = formViewModel(requests = requests)
        val state = collecting(viewModel.uiState)

        viewModel.submit()

        assertEquals(ADOPTION_REASON_ERROR, state.value.errors.reason)
        assertFalse(state.value.submitted)
        assertTrue(requests.observeByRequester(demo.id).first().isEmpty())
    }

    @Test
    fun formAsksWhichOtherPetsWhenTheAnswerIsYes() = runTest {
        val viewModel = formViewModel()
        val state = collecting(viewModel.uiState)
        viewModel.onReasonChange(validReason)
        viewModel.onHasOtherPetsChange(true)

        viewModel.submit()

        assertEquals(OTHER_PETS_ERROR, state.value.errors.otherPets)
        assertFalse(state.value.submitted)

        viewModel.onHasOtherPetsChange(false)
        viewModel.submit()
        assertTrue(state.value.submitted)
    }

    @Test
    fun validFormIsSavedInTheRepository() = runTest {
        val requests = FakeAdoptionRequestRepository(initialRequests = emptyList())
        val viewModel = formViewModel(requests = requests)
        val state = collecting(viewModel.uiState)
        viewModel.onHousingTypeChange(HousingType.Apartment)
        viewModel.onHousingTenureChange(HousingTenure.Rented)
        viewModel.onHasOtherPetsChange(true)
        viewModel.onOtherPetsChange("Un gato")
        viewModel.onReasonChange(validReason)

        viewModel.submit()

        assertTrue(state.value.submitted)
        assertNull(state.value.submitError)
        val saved = requests.observeByRequester(demo.id).first().single()
        assertEquals("listing-1", saved.listingId)
        assertEquals("org-patitas", saved.organizationId)
        assertEquals(HousingType.Apartment, saved.housingType)
        assertEquals(HousingTenure.Rented, saved.housingTenure)
        assertEquals("Un gato", saved.otherPets)
        assertEquals(validReason, saved.reason)
        assertEquals(AdoptionRequestStatus.Pending, saved.status)
    }

    @Test
    fun formShowsTheRepositoryErrorForASecondActiveRequest() = runTest {
        val requests = FakeAdoptionRequestRepository(initialRequests = emptyList())
        val first = formViewModel(requests = requests)
        first.onReasonChange(validReason)
        first.submit()

        val second = formViewModel(requests = requests)
        val state = collecting(second.uiState)
        second.onReasonChange(validReason)
        second.submit()

        assertEquals(FakeAdoptionRequestRepository.ALREADY_ACTIVE, state.value.submitError)
        assertFalse(state.value.submitted)
    }

    @Test
    fun formIsBlockedWithoutAnAccount() = runTest {
        val signedOut = collecting(formViewModel(auth = FakeAuthRepository()).uiState).value
        assertEquals(ADOPTION_ACCOUNT_REQUIRED, signedOut.blockedMessage)

        val auth = FakeAuthRepository()
        auth.signInAnonymously()
        val anonymous = collecting(formViewModel(auth = auth).uiState).value
        assertEquals(ADOPTION_ACCOUNT_REQUIRED, anonymous.blockedMessage)
    }

    @Test
    fun formIsBlockedForAnAffiliate() = runTest {
        val viewModel = formViewModel(auth = FakeAuthRepository(initialUser = SampleData.demoAffiliate))
        val state = collecting(viewModel.uiState)
        assertEquals(ADOPTION_ONLY_USERS, state.value.blockedMessage)

        viewModel.onReasonChange(validReason)
        viewModel.submit()
        assertFalse(state.value.submitted)
    }
}
