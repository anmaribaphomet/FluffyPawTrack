package com.example.fluffypawtrack.ui.affiliate

import com.example.fluffypawtrack.data.fake.FakeAdoptionRepository
import com.example.fluffypawtrack.data.fake.FakeAdoptionRequestRepository
import com.example.fluffypawtrack.data.fake.FakeAuthRepository
import com.example.fluffypawtrack.data.fake.FakeNotificationRepository
import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.domain.model.AdoptionRequestStatus
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CenterAdoptionRequestsViewModelTest {

    private val affiliate = SampleData.demoAffiliate
    private val demo = SampleData.demoUser

    private val listings = FakeAdoptionRepository()
    private val notifications = FakeNotificationRepository()
    private val requests = FakeAdoptionRequestRepository(listings, notifications)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun <T> TestScope.collecting(state: StateFlow<T>): StateFlow<T> {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
        return state
    }

    private fun viewModel(auth: FakeAuthRepository = FakeAuthRepository(affiliate)) =
        CenterAdoptionRequestsViewModel(auth, requests, listings)

    private suspend fun statusOf(id: String) =
        requests.observeByOrganization("org-patitas").first().single { it.id == id }.status

    @Test
    fun groupsTheCenterRequestsByAnimal() = runTest {
        val state = collecting(viewModel().uiState).value

        assertNull(state.blockedMessage)
        val michi = state.groups.single { it.listing.id == "listing-2" }
        assertEquals("Michi", michi.listing.name)
        assertEquals(listOf("adoption-request-1"), michi.requests.map { it.id })
        assertTrue(state.groups.none { it.listing.organizationId != "org-patitas" })
    }

    @Test
    fun approveMovesTheRequestToApprovedAndNotifiesTheRequester() = runTest {
        val viewModel = viewModel()
        collecting(viewModel.uiState)

        viewModel.approve("adoption-request-1")

        assertEquals(AdoptionRequestStatus.Approved, statusOf("adoption-request-1"))
        assertEquals(
            "¡Buenas noticias! El centro aprobó tu solicitud para adoptar a Michi. Pronto se pondrán en contacto contigo.",
            notifications.observeNotifications(demo.id).first().last().message,
        )
    }

    @Test
    fun rejectAsksForAReasonAndSendsIt() = runTest {
        val viewModel = viewModel()
        val state = collecting(viewModel.uiState)

        viewModel.startReject("adoption-request-1")
        assertEquals("adoption-request-1", state.value.rejecting?.requestId)
        viewModel.confirmReject()
        assertEquals(FakeAdoptionRequestRepository.REASON_REQUIRED, state.value.rejecting?.error)
        assertEquals(AdoptionRequestStatus.Pending, statusOf("adoption-request-1"))

        viewModel.onRejectReasonChange("Buscamos un hogar con patio")
        viewModel.confirmReject()

        assertNull(state.value.rejecting)
        assertEquals(AdoptionRequestStatus.Rejected, statusOf("adoption-request-1"))
        assertEquals(
            "Tu solicitud para adoptar a Michi no fue aprobada esta vez. ¡Hay muchos más esperando un hogar!\n" +
                "Motivo: Buscamos un hogar con patio",
            notifications.observeNotifications(demo.id).first().last().message,
        )
    }

    @Test
    fun markAdoptedClosesTheListingAndRejectsItsPendingRequests() = runTest {
        val viewModel = viewModel()
        collecting(viewModel.uiState)

        viewModel.markAdopted("listing-2")

        assertEquals(ListingStatus.Adopted, listings.getListing("listing-2")?.status)
        assertEquals(AdoptionRequestStatus.Rejected, statusOf("adoption-request-1"))
        assertEquals(
            "El animal ya fue adoptado",
            requests.observeByOrganization("org-patitas").first().single { it.id == "adoption-request-1" }.rejectionReason,
        )
    }

    @Test
    fun aUserAccountIsBlocked() = runTest {
        val state = collecting(viewModel(FakeAuthRepository(demo)).uiState).value
        assertEquals(CENTER_ONLY_MESSAGE, state.blockedMessage)
        assertTrue(state.groups.isEmpty())
    }
}
