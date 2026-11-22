package com.example.fluffypawtrack.ui.map

import com.example.fluffypawtrack.data.fake.FakeAdoptionRepository
import com.example.fluffypawtrack.data.fake.FakeOrganizationRepository
import com.example.fluffypawtrack.data.fake.FakeReportRepository
import com.example.fluffypawtrack.domain.model.ReportCategory
import com.example.fluffypawtrack.ui.common.PinColors
import com.example.fluffypawtrack.ui.common.pinColor
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    private val reports = FakeReportRepository()
    private val organizations = FakeOrganizationRepository()
    private val adoptions = FakeAdoptionRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(initialFilter: MapFilter = MapFilter.All) =
        MapViewModel(reports, organizations, adoptions, initialFilter)

    private suspend fun activeReportIds() = reports.observeActiveReports().first().map { it.id }.toSet()
    private suspend fun activePlaceIds() = organizations.observeActiveOrganizations().first().map { it.id }.toSet()
    private suspend fun listingIds() = adoptions.observeAvailableListings().first().map { it.id }.toSet()

    private fun MapViewModel.visibleIds() = uiState.value.items.map { it.id }.toSet()

    @Test
    fun allFilterShowsActiveReportsPlacesAndAvailableListings() = runTest {
        val vm = viewModel()

        assertEquals(MapFilter.All, vm.uiState.value.filter)
        assertEquals(activeReportIds() + activePlaceIds() + listingIds(), vm.visibleIds())
    }

    @Test
    fun filterOptionsListTodosEachCategoryAdoptionsAndSupportPlaces() {
        val options = viewModel().uiState.value.filterOptions

        assertEquals(
            listOf(MapFilter.All) + ReportCategory.entries.map { MapFilter.Category(it) } +
                listOf(MapFilter.Adoptions, MapFilter.SupportPlaces),
            options,
        )
        assertEquals(
            listOf("Todos", "Situación de calle", "Posible mascota extraviada", "Maltratado",
                "Herido o lesionado", "Estado crítico o moribundo", "Abandono", "Adopciones", "Lugares de apoyo"),
            options.map { it.label },
        )
    }

    @Test
    fun eachCategoryFilterShowsOnlyReportsOfThatCategory() = runTest {
        val vm = viewModel()
        val active = reports.observeActiveReports().first()

        ReportCategory.entries.forEach { category ->
            vm.onFilterSelected(MapFilter.Category(category))

            val expected = active.filter { it.category == category }.map { it.id }.toSet()
            assertEquals(expected, vm.visibleIds(), "filter $category")
            assertTrue(vm.uiState.value.items.all { it.color == category.pinColor })
        }
    }

    @Test
    fun adoptionsFilterShowsOnlyAvailableListings() = runTest {
        val vm = viewModel()

        vm.onFilterSelected(MapFilter.Adoptions)

        assertEquals(listingIds(), vm.visibleIds())
        assertTrue(vm.uiState.value.items.all { it is MapItem.Adoption && it.color == PinColors.Adoption })
    }

    @Test
    fun supportPlacesFilterShowsOnlyActiveOrganizations() = runTest {
        val vm = viewModel()

        vm.onFilterSelected(MapFilter.SupportPlaces)

        assertEquals(activePlaceIds(), vm.visibleIds())
        assertTrue(vm.uiState.value.items.all { it is MapItem.Place && it.color == PinColors.SupportPlace })
    }

    @Test
    fun initialFilterIsApplied() = runTest {
        val vm = viewModel(initialFilter = MapFilter.Adoptions)

        assertEquals(MapFilter.Adoptions, vm.uiState.value.filter)
        assertEquals(listingIds(), vm.visibleIds())
    }

    @Test
    fun adoptionListingsArePlacedNearTheirOrganization() = runTest {
        val vm = viewModel(initialFilter = MapFilter.Adoptions)
        val places = organizations.observeActiveOrganizations().first().associateBy { it.id }

        vm.uiState.value.items.filterIsInstance<MapItem.Adoption>().forEach { item ->
            val center = places.getValue(item.listing.organizationId).location
            assertTrue(kotlin.math.abs(item.location.latitude - center.latitude) < 0.002)
            assertTrue(kotlin.math.abs(item.location.longitude - center.longitude) < 0.002)
        }
    }

    @Test
    fun searchMatchesDescriptionAddressAndNameIgnoringCaseAndAccents() = runTest {
        val vm = viewModel()

        vm.onQueryChange("LABRADOR")
        // report-5 description and listing-3 breed.
        assertEquals(setOf("report-5", "listing-3"), vm.visibleIds())

        vm.onQueryChange("pino suarez")
        // report-2 address "Calle Pino Suárez 210".
        assertEquals(setOf("report-2"), vm.visibleIds())

        vm.onQueryChange("proteccion")
        // Place name "Instituto Municipal de Protección y Bienestar Animal".
        assertEquals(setOf("org-imbpa"), vm.visibleIds())

        vm.onQueryChange("  ")
        assertEquals(activeReportIds() + activePlaceIds() + listingIds(), vm.visibleIds())
    }

    @Test
    fun searchCombinesWithTheSelectedFilter() = runTest {
        val vm = viewModel()

        vm.onFilterSelected(MapFilter.Adoptions)
        vm.onQueryChange("labrador")

        assertEquals(setOf("listing-3"), vm.visibleIds())
        assertEquals("labrador", vm.uiState.value.query)
    }

    @Test
    fun pinClickSelectsTheItemAndDismissClearsIt() = runTest {
        val vm = viewModel()

        vm.onPinClick("report-3")
        val selected = vm.uiState.value.selected
        assertTrue(selected is MapItem.Report && selected.report.id == "report-3")

        vm.onPinClick("org-petcare")
        assertTrue(vm.uiState.value.selected is MapItem.Place)

        vm.onDismissDetail()
        assertNull(vm.uiState.value.selected)
    }

    @Test
    fun pinsMirrorVisibleItems() = runTest {
        val vm = viewModel()

        vm.onFilterSelected(MapFilter.Category(ReportCategory.PossiblyLost))

        val pins = vm.uiState.value.pins
        assertEquals(vm.visibleIds(), pins.map { it.id }.toSet())
        assertTrue(pins.all { it.color == ReportCategory.PossiblyLost.pinColor })
    }
}
