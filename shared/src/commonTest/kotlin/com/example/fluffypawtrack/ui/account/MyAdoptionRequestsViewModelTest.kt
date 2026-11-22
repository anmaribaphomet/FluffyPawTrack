package com.example.fluffypawtrack.ui.account

import com.example.fluffypawtrack.data.fake.FakeAdoptionRepository
import com.example.fluffypawtrack.data.fake.FakeAdoptionRequestRepository
import com.example.fluffypawtrack.data.fake.FakeAuthRepository
import com.example.fluffypawtrack.data.fake.FakeOrganizationRepository
import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.domain.model.AdoptionRequestStatus
import com.example.fluffypawtrack.domain.model.coverPhotoUrl
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MyAdoptionRequestsViewModelTest {

    private val demo = SampleData.demoUser

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun <T> TestScope.collecting(state: StateFlow<T>): StateFlow<T> {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
        return state
    }

    private fun viewModel(requests: FakeAdoptionRequestRepository, auth: FakeAuthRepository = FakeAuthRepository(demo)) =
        MyAdoptionRequestsViewModel(auth, requests, FakeAdoptionRepository(), FakeOrganizationRepository())

    @Test
    fun listsTheUserRequestsWithAnimalPhotoNameAndCenter() = runTest {
        val state = collecting(viewModel(FakeAdoptionRequestRepository()).uiState).value

        val expected = SampleData.adoptionRequests.filter { it.requesterId == demo.id }
        assertEquals(expected.map { it.id }.toSet(), state.items.map { it.request.id }.toSet())
        val michi = state.items.single { it.request.listingId == "listing-2" }
        assertEquals("Michi", michi.animalName)
        assertEquals(SampleData.adoptionListings.first { it.id == "listing-2" }.coverPhotoUrl, michi.photoUrl)
        assertEquals("Patitas Callejeras HMO", michi.centerName)
        assertTrue(state.loaded)
    }

    @Test
    fun cancelMovesAPendingRequestToCancelled() = runTest {
        val requests = FakeAdoptionRequestRepository()
        val viewModel = viewModel(requests)
        val state = collecting(viewModel.uiState)
        val id = state.value.items.first().request.id

        viewModel.cancel(id)

        assertEquals(AdoptionRequestStatus.Cancelled, state.value.items.single { it.request.id == id }.request.status)
        assertEquals(AdoptionRequestStatus.Cancelled, requests.observeByRequester(demo.id).first().single { it.id == id }.status)
        assertNull(state.value.message)
    }

    @Test
    fun cancellingTwiceShowsTheRepositoryMessage() = runTest {
        val viewModel = viewModel(FakeAdoptionRequestRepository())
        val state = collecting(viewModel.uiState)
        val id = state.value.items.first().request.id

        viewModel.cancel(id)
        viewModel.cancel(id)

        assertEquals("Solo puedes cancelar solicitudes pendientes", state.value.message)
        viewModel.dismissMessage()
        assertNull(state.value.message)
    }

    @Test
    fun signedOutUserSeesNoRequests() = runTest {
        val state = collecting(viewModel(FakeAdoptionRequestRepository(), FakeAuthRepository()).uiState).value
        assertTrue(state.items.isEmpty())
    }
}
