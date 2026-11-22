package com.example.fluffypawtrack.ui.adoptions

import com.example.fluffypawtrack.data.fake.FakeAdoptionRepository
import com.example.fluffypawtrack.data.fake.FakeOrganizationRepository
import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.domain.model.ListingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
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
import kotlin.test.assertTrue

/** "Animales en adopción" section of the center detail (SC). */
@OptIn(ExperimentalCoroutinesApi::class)
class CenterDetailViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.stateOf(organizationId: String): StateFlow<CenterDetailUiState> {
        val state = CenterDetailViewModel(organizationId, FakeOrganizationRepository(), FakeAdoptionRepository()).uiState
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
        return state
    }

    @Test
    fun centerExposesOneCollapsedCardPerAvailableAnimal() = runTest {
        val cards = stateOf("org-patitas").value.animalCards

        val expected = SampleData.adoptionListings
            .filter { it.organizationId == "org-patitas" && it.status == ListingStatus.Available }
            .map { AnimalCard(it.id, it.name, it.species, it.breed, it.age, it.photoUrls.first()) }
        assertEquals(expected, cards)
        assertEquals(
            AnimalCard("listing-1", "Canela", "Perro", "Mestiza", "2 años", "res:sample_listing_canela_1"),
            cards.first(),
        )
    }

    @Test
    fun adoptedAnimalsHaveNoCard() = runTest {
        val cards = stateOf("org-patitas").value.animalCards

        assertTrue(cards.none { it.listingId == "listing-4" })
    }

    @Test
    fun centerWithoutAvailableAnimalsHasNoCards() = runTest {
        val state = stateOf("org-imbpa").value

        assertEquals("org-imbpa", state.organization?.id)
        assertTrue(state.animalCards.isEmpty())
    }
}
