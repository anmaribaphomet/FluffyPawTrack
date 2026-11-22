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

    /** Active adoption centers with at least one available listing: only Patitas in the sample data. */
    private val adoptionCenterIds = setOf("org-patitas")

    private fun MapViewModel.visibleIds() = uiState.value.items.map { it.id }.toSet()

    @Test
    fun allFilterShowsActiveReportsAndOnePinPerPlace() = runTest {
        val vm = viewModel()

        assertEquals(MapFilter.All, vm.uiState.value.filter)
        assertEquals(activeReportIds() + activePlaceIds(), vm.visibleIds())
        val pinIds = vm.uiState.value.pins.map { it.id }
        assertEquals(pinIds.size, pinIds.toSet().size)
    }

    @Test
    fun mapHasNoIndividualAdoptionListingPins() = runTest {
        val vm = viewModel()
        val listings = listingIds()
        assertTrue(listings.isNotEmpty())

        listOf(MapFilter.All, MapFilter.Adoptions).forEach { filter ->
            vm.onFilterSelected(filter)
            assertTrue(vm.uiState.value.pins.none { it.id in listings }, "filter $filter")
        }
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
                "Herido o lesionado", "Estado crítico", "Abandono", "Adopciones", "Lugares de apoyo"),
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
    fun adoptionsFilterShowsExactlyOnePinPerAdoptionCenterWithAnimals() = runTest {
        val vm = viewModel()

        vm.onFilterSelected(MapFilter.Adoptions)

        assertEquals(adoptionCenterIds, vm.visibleIds())
        assertEquals(adoptionCenterIds.toList(), vm.uiState.value.pins.map { it.id })
        assertTrue(vm.uiState.value.items.all { it is MapItem.AdoptionCenter && it.color == PinColors.Adoption })
        val patitas = vm.uiState.value.items.single() as MapItem.AdoptionCenter
        assertEquals(listingIds(), patitas.listings.map { it.id }.toSet())
    }

    @Test
    fun adoptionCenterPinSitsOnTheCenter() = runTest {
        val vm = viewModel(initialFilter = MapFilter.Adoptions)
        val places = organizations.observeActiveOrganizations().first().associateBy { it.id }

        vm.uiState.value.items.forEach { item ->
            assertEquals(places.getValue(item.id).location, item.location)
        }
    }

    @Test
    fun supportPlacesFilterShowsTheOtherActiveOrganizations() = runTest {
        val vm = viewModel()

        vm.onFilterSelected(MapFilter.SupportPlaces)

        assertEquals(activePlaceIds() - adoptionCenterIds, vm.visibleIds())
        assertTrue(vm.uiState.value.items.all { it is MapItem.Place && it.color == PinColors.SupportPlace })
    }

    @Test
    fun initialFilterIsApplied() = runTest {
        val vm = viewModel(initialFilter = MapFilter.Adoptions)

        assertEquals(MapFilter.Adoptions, vm.uiState.value.filter)
        assertEquals(adoptionCenterIds, vm.visibleIds())
    }

    @Test
    fun searchMatchesDescriptionAddressAndNameIgnoringCaseAndAccents() = runTest {
        val vm = viewModel()

        vm.onQueryChange("LABRADOR")
        // report-5 description and Patitas, whose animal Toby is a "Labrador mestizo".
        assertEquals(setOf("report-5", "org-patitas"), vm.visibleIds())

        vm.onQueryChange("pino suarez")
        // report-2 address "Calle Pino Suárez 210".
        assertEquals(setOf("report-2"), vm.visibleIds())

        vm.onQueryChange("proteccion")
        // Place name "Instituto Municipal de Protección y Bienestar Animal".
        assertEquals(setOf("org-imbpa"), vm.visibleIds())

        vm.onQueryChange("  ")
        assertEquals(activeReportIds() + activePlaceIds(), vm.visibleIds())
    }

    @Test
    fun searchCombinesWithTheSelectedFilter() = runTest {
        val vm = viewModel()

        vm.onFilterSelected(MapFilter.Adoptions)
        vm.onQueryChange("labrador")

        assertEquals(setOf("org-patitas"), vm.visibleIds())
        assertEquals("labrador", vm.uiState.value.query)
    }

    @Test
    fun adoptionsSearchByAnimalNameOrSpeciesFindsItsCenter() = runTest {
        val vm = viewModel(initialFilter = MapFilter.Adoptions)

        vm.onQueryChange("Canela")
        assertEquals(setOf("org-patitas"), vm.visibleIds())

        vm.onQueryChange("gato")
        assertEquals(setOf("org-patitas"), vm.visibleIds())

        vm.onQueryChange("patitas")
        assertEquals(setOf("org-patitas"), vm.visibleIds())

        // Luna was already adopted, so she does not lead to the center.
        vm.onQueryChange("Luna")
        assertTrue(vm.visibleIds().isEmpty())
    }

    @Test
    fun pinClickSelectsTheItemAndDismissClearsIt() = runTest {
        val vm = viewModel()

        vm.onPinClick("report-3")
        val selected = vm.uiState.value.selected
        assertTrue(selected is MapItem.Report && selected.report.id == "report-3")

        vm.onPinClick("org-petcare")
        assertTrue(vm.uiState.value.selected is MapItem.Place)

        vm.onPinClick("org-patitas")
        val center = vm.uiState.value.selected
        assertTrue(center is MapItem.AdoptionCenter && center.organization.id == "org-patitas")

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
