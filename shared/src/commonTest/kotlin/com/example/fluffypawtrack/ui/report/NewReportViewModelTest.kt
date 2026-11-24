package com.example.fluffypawtrack.ui.report

import com.example.fluffypawtrack.data.fake.FakeAuthRepository
import com.example.fluffypawtrack.data.fake.FakeReportRepository
import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.domain.model.GeoPoint
import com.example.fluffypawtrack.domain.model.Priority
import com.example.fluffypawtrack.domain.model.ReportCategory
import com.example.fluffypawtrack.platform.ReverseGeocoder
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NewReportViewModelTest {

    private val reports = FakeReportRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        signedIn: Boolean = true,
        geocoder: ReverseGeocoder = ReverseGeocoder { null },
    ) = NewReportViewModel(
        reportRepository = reports,
        authRepository = FakeAuthRepository(initialUser = if (signedIn) SampleData.demoUser else null),
        reverseGeocoder = geocoder,
    )

    private fun goToLocation(vm: NewReportViewModel) {
        vm.selectCategory(ReportCategory.Street)
        vm.next()
        vm.onPhotoSelected("memory://photo/1")
        vm.next()
    }

    @Test
    fun gpsFixStoresTheCoordinatesAndTheGeocodedAddress() = runTest {
        val point = GeoPoint(29.0729, -110.9559)
        val vm = viewModel(geocoder = { if (it == point) "Blvd. Hidalgo 45, Centro, Hermosillo" else null })
        goToLocation(vm)
        vm.onLocationResult(point)
        assertEquals(LocationStatus.Found, vm.uiState.value.locationStatus)
        assertEquals(point, vm.uiState.value.location)
        assertEquals("Blvd. Hidalgo 45, Centro, Hermosillo", vm.uiState.value.address)

        vm.next()
        vm.onDescriptionChange("Perrito en la banqueta")
        vm.next()
        vm.submit()
        val published = assertNotNull(vm.uiState.value.publishedReport)
        assertEquals(point, published.location)
        assertEquals("Blvd. Hidalgo 45, Centro, Hermosillo", published.address)
    }

    @Test
    fun gpsFixWithoutAGeocodedAddressShowsTheCoordinates() = runTest {
        val vm = viewModel(geocoder = { null })
        goToLocation(vm)
        vm.onLocationResult(GeoPoint(29.0729, -110.9559))
        assertEquals(GeoPoint(29.0729, -110.9559), vm.uiState.value.location)
        assertEquals("29.07290, -110.95590", vm.uiState.value.address)
        assertTrue(vm.uiState.value.canContinue)
    }

    @Test
    fun aFailingGeocoderFallsBackToTheCoordinates() = runTest {
        val vm = viewModel(geocoder = { error("service unavailable") })
        goToLocation(vm)
        vm.onLocationResult(GeoPoint(29.0729, -110.9559))
        assertEquals("29.07290, -110.95590", vm.uiState.value.address)
    }

    @Test
    fun selectingACategorySuggestsItsPriority() {
        val vm = viewModel()
        vm.selectCategory(ReportCategory.Injured)
        assertEquals(Priority.High, vm.uiState.value.priority)
        assertFalse(vm.uiState.value.isPriorityLocked)
    }

    @Test
    fun criticalCategoryIsLockedToUrgent() {
        val vm = viewModel()
        vm.selectCategory(ReportCategory.Critical)
        vm.selectPriority(Priority.Low)
        assertEquals(Priority.Urgent, vm.uiState.value.priority)
        assertTrue(vm.uiState.value.isPriorityLocked)
    }

    @Test
    fun userCanOverrideTheSuggestedPriority() {
        val vm = viewModel()
        vm.selectCategory(ReportCategory.Street)
        vm.selectPriority(Priority.Urgent)
        assertEquals(Priority.Urgent, vm.uiState.value.priority)
    }

    @Test
    fun cannotLeaveTheCategoryStepWithoutACategory() {
        val vm = viewModel()
        assertFalse(vm.uiState.value.canContinue)
        vm.next()
        assertEquals(ReportStep.Category, vm.uiState.value.step)

        vm.selectCategory(ReportCategory.Street)
        vm.next()
        assertEquals(ReportStep.Photo, vm.uiState.value.step)
    }

    @Test
    fun cannotLeaveThePhotoStepWithoutAPhoto() {
        val vm = viewModel()
        vm.selectCategory(ReportCategory.Street)
        vm.next()
        assertEquals(ReportStep.Photo, vm.uiState.value.step)
        assertFalse(vm.uiState.value.canContinue)
        vm.next()
        assertEquals(ReportStep.Photo, vm.uiState.value.step)

        vm.onPhotoSelected("memory://photo/7")
        assertTrue(vm.uiState.value.canContinue)
        vm.next()
        assertEquals(ReportStep.Location, vm.uiState.value.step)
    }

    @Test
    fun cannotLeaveTheDetailsStepWithoutADescription() {
        val vm = viewModel()
        goToDetails(vm)
        vm.onDescriptionChange("   ")
        vm.next()
        assertEquals(ReportStep.Details, vm.uiState.value.step)

        vm.onDescriptionChange("Perrito con collar azul")
        vm.next()
        assertEquals(ReportStep.Summary, vm.uiState.value.step)
    }

    @Test
    fun deniedLocationFallsBackToAManualAddress() {
        val vm = viewModel()
        vm.selectCategory(ReportCategory.Street)
        vm.next()
        vm.onPhotoSelected("memory://photo/1")
        vm.next()
        vm.onLocationResult(null)
        assertTrue(vm.uiState.value.isManualAddress)
        assertFalse(vm.uiState.value.canContinue)
        vm.onAddressChange("Calle Rosales y Blvd. Hidalgo")
        assertTrue(vm.uiState.value.canContinue)
    }

    @Test
    fun backMovesToThePreviousStepAndReportsTheFirstStep() {
        val vm = viewModel()
        assertFalse(vm.back())
        vm.selectCategory(ReportCategory.Street)
        vm.next()
        assertTrue(vm.back())
        assertEquals(ReportStep.Category, vm.uiState.value.step)
    }

    @Test
    fun submitCreatesTheReportWithTheCurrentUserId() = runTest {
        val vm = viewModel(signedIn = true)
        fillAndSubmit(vm)
        val published = assertNotNull(vm.uiState.value.publishedReport)
        assertEquals(SampleData.demoUser.id, published.reporterId)
        assertEquals(ReportCategory.Injured, published.category)
        assertEquals(Priority.High, published.priority)
        assertEquals("Perrito herido en la pata", published.description)
        assertEquals("Frente a la tienda", published.reference)
        assertEquals("memory://photo/1", published.photoUrl)
        assertTrue(reports.observeReportsBy(SampleData.demoUser.id).first().any { it.id == published.id })
    }

    @Test
    fun submitWithoutSessionCreatesAnAnonymousReport() = runTest {
        val vm = viewModel(signedIn = false)
        fillAndSubmit(vm)
        val published = assertNotNull(vm.uiState.value.publishedReport)
        assertNull(published.reporterId)
    }

    private suspend fun anonymousViewModel(auth: FakeAuthRepository) = auth.also { it.signInAnonymously() }.let {
        NewReportViewModel(reportRepository = reports, authRepository = it)
    }

    @Test
    fun anonymousBackOnTheFirstStepAsksToDiscardTheReport() = runTest {
        val vm = anonymousViewModel(FakeAuthRepository())
        assertTrue(vm.uiState.value.isAnonymous)

        assertTrue(vm.back())
        assertTrue(vm.uiState.value.showDiscardDialog)

        vm.onKeepReporting()
        assertFalse(vm.uiState.value.showDiscardDialog)
        assertEquals(ReportStep.Category, vm.uiState.value.step)
        assertFalse(vm.uiState.value.exitedToLogin)
    }

    @Test
    fun anonymousBackOnALaterStepGoesToThePreviousStep() = runTest {
        val vm = anonymousViewModel(FakeAuthRepository())
        vm.selectCategory(ReportCategory.Street)
        vm.next()

        assertTrue(vm.back())
        assertEquals(ReportStep.Category, vm.uiState.value.step)
        assertFalse(vm.uiState.value.showDiscardDialog)
    }

    @Test
    fun discardSignsOutTheAnonymousSessionAndLeavesToLogin() = runTest {
        val auth = FakeAuthRepository()
        val vm = anonymousViewModel(auth)
        vm.back()

        vm.onDiscard()

        assertNull(auth.currentUser.value)
        assertFalse(vm.uiState.value.showDiscardDialog)
        assertTrue(vm.uiState.value.exitedToLogin)
    }

    @Test
    fun signedInBackOnTheFirstStepNeverShowsTheDiscardDialog() {
        val vm = viewModel(signedIn = true)
        assertFalse(vm.uiState.value.isAnonymous)
        assertFalse(vm.back())
        assertFalse(vm.uiState.value.showDiscardDialog)
    }

    @Test
    fun anonymousPublishBackToStartSignsOutAndLeavesToLogin() = runTest {
        val auth = FakeAuthRepository()
        val vm = anonymousViewModel(auth)
        fillAndSubmit(vm)
        assertNotNull(vm.uiState.value.publishedReport)

        vm.onBackToStart()

        assertNull(auth.currentUser.value)
        assertTrue(vm.uiState.value.exitedToLogin)
    }

    private fun goToDetails(vm: NewReportViewModel) {
        vm.selectCategory(ReportCategory.Injured)
        vm.next() // photo
        vm.onPhotoSelected("memory://photo/1")
        vm.next() // location
        vm.onLocationResult(GeoPoint(29.08, -110.95))
        vm.next() // details
        assertEquals(ReportStep.Details, vm.uiState.value.step)
    }

    private fun fillAndSubmit(vm: NewReportViewModel) {
        goToDetails(vm)
        vm.onReferenceChange("Frente a la tienda")
        vm.onDescriptionChange("Perrito herido en la pata")
        vm.next()
        vm.submit()
    }
}
