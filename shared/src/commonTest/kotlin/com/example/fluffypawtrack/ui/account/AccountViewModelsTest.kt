package com.example.fluffypawtrack.ui.account

import com.example.fluffypawtrack.data.fake.FakeAdoptionRepository
import com.example.fluffypawtrack.data.fake.FakeAuthRepository
import com.example.fluffypawtrack.data.fake.FakeFavoritesRepository
import com.example.fluffypawtrack.data.fake.FakeReportRepository
import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.domain.model.ReportStatus
import com.example.fluffypawtrack.ui.profile.ProfileViewModel
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class AccountViewModelsTest {

    private val demo = SampleData.demoUser

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    /** Keeps the state collected, as the screen does, so `value` stays current. */
    private fun <T> TestScope.collecting(state: StateFlow<T>): StateFlow<T> {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
        return state
    }

    @Test
    fun profileCountsReportsAttendedAndFavoritesOfTheSignedInUser() = runTest {
        val auth = FakeAuthRepository()
        auth.signIn("carlos@correo.com", SampleData.DEMO_PASSWORD).getOrThrow()
        val favorites = FakeFavoritesRepository()
        favorites.toggleFavorite("user-2", "listing-1")
        val reports = SampleData.reports.filter { it.reporterId == "user-2" }

        val state = collecting(ProfileViewModel(auth, FakeReportRepository(), favorites).uiState).value

        assertEquals("Carlos Moreno", state.name)
        assertEquals("carlos@correo.com", state.email)
        assertEquals(reports.size, state.reports)
        assertEquals(reports.count { it.status != ReportStatus.Pending }, state.attended)
        assertTrue(state.attended in 1 until state.reports, "sample data should mix pending and attended reports")
        assertEquals(1, state.favorites)
    }

    @Test
    fun profileIsEmptyWithoutASession() = runTest {
        val state = collecting(
            ProfileViewModel(FakeAuthRepository(), FakeReportRepository(), FakeFavoritesRepository()).uiState,
        ).value
        assertEquals(0, state.reports)
        assertEquals("", state.name)
    }

    @Test
    fun personalInfoShowsTheSignedInUser() = runTest {
        val state = collecting(PersonalInfoViewModel(FakeAuthRepository(initialUser = demo)).uiState).value
        assertEquals(demo.firstName, state.firstName)
        assertEquals(demo.lastName, state.lastName)
        assertEquals(demo.email, state.email)
        assertEquals(demo.phone, state.phone)
    }

    @Test
    fun myReportsListsOnlyTheUsersReportsAndDeleteRemovesOne() = runTest {
        val repository = FakeReportRepository()
        val viewModel = MyReportsViewModel(FakeAuthRepository(initialUser = demo), repository)
        val state = collecting(viewModel.uiState)
        // Newest first.
        val own = SampleData.reports.filter { it.reporterId == demo.id }.sortedByDescending { it.createdAt }.map { it.id }
        assertEquals(own, state.value.reports.map { it.id })

        viewModel.select(own.first())
        viewModel.delete(own.first())

        assertEquals(own.drop(1), state.value.reports.map { it.id })
        assertNull(state.value.selected)
        assertNull(repository.getReport(own.first()))
    }

    @Test
    fun myReportsEditUpdatesThePendingReport() = runTest {
        val viewModel = MyReportsViewModel(FakeAuthRepository(initialUser = demo), FakeReportRepository())
        val state = collecting(viewModel.uiState)
        val id = state.value.reports.first().id
        viewModel.select(id)

        viewModel.update(id, "Sigue en la esquina", "  ")

        val selected = assertNotNull(state.value.selected)
        assertEquals("Sigue en la esquina", selected.description)
        assertNull(selected.reference, "a blank reference is stored as no reference")
        assertNull(state.value.message)
    }

    @Test
    fun myReportsShowsAMessageWhenTheUpdateFails() = runTest {
        val viewModel = MyReportsViewModel(FakeAuthRepository(initialUser = demo), FakeReportRepository())
        val state = collecting(viewModel.uiState)

        viewModel.update("missing", "Texto", null)

        assertNotNull(state.value.message)
    }

    @Test
    fun favoritesListsSavedListingsAndRemoveDropsOne() = runTest {
        val favorites = FakeFavoritesRepository()
        favorites.toggleFavorite(demo.id, "listing-1")
        favorites.toggleFavorite(demo.id, "listing-2")
        val viewModel = FavoritesViewModel(FakeAuthRepository(initialUser = demo), favorites, FakeAdoptionRepository())
        val state = collecting(viewModel.uiState)
        assertEquals(setOf("listing-1", "listing-2"), state.value.listings.map { it.id }.toSet())

        viewModel.remove("listing-1")

        assertEquals(listOf("listing-2"), state.value.listings.map { it.id })
    }

    @Test
    fun favoritesIsEmptyWhenNothingWasSaved() = runTest {
        val viewModel = FavoritesViewModel(
            FakeAuthRepository(initialUser = demo),
            FakeFavoritesRepository(),
            FakeAdoptionRepository(),
        )
        val state = collecting(viewModel.uiState).value
        assertTrue(state.loaded)
        assertTrue(state.listings.isEmpty())
    }
}
