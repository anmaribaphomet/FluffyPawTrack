package com.example.fluffypawtrack.ui.admin

import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.domain.model.Report
import com.example.fluffypawtrack.domain.model.ReportCategory
import com.example.fluffypawtrack.domain.model.ReportStatus
import com.example.fluffypawtrack.domain.repository.ReportRepository
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
class ReportSupervisionViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun listsEveryReportIncludingHiddenAndClosedOnes() = runTest {
        val fixture = AdminFixture()
        fixture.reports.setHidden("report-1", hidden = true)
        val vm = ReportSupervisionViewModel(fixture.auth, fixture.reports)

        val ids = vm.uiState.value.reports.map { it.id }
        assertEquals(SampleData.reports.size, ids.size)
        assertTrue("report-1" in ids && "report-6" in ids)
    }

    @Test
    fun filtersHiddenClosedAndByCategory() = runTest {
        val fixture = AdminFixture()
        fixture.reports.setHidden("report-2", hidden = true)
        val vm = ReportSupervisionViewModel(fixture.auth, fixture.reports)

        vm.onFilter(SupervisionFilter.Hidden)
        assertEquals(listOf("report-2"), vm.uiState.value.reports.map { it.id })

        vm.onFilter(SupervisionFilter.Closed)
        assertEquals(listOf("report-6"), vm.uiState.value.reports.map { it.id })

        vm.onFilter(SupervisionFilter.All)
        vm.onCategory(ReportCategory.PossiblyLost)
        assertEquals(setOf("report-2", "report-5", "report-7"), vm.uiState.value.reports.map { it.id }.toSet())

        vm.onFilter(SupervisionFilter.Hidden)
        assertEquals(listOf("report-2"), vm.uiState.value.reports.map { it.id })

        vm.onCategory(null)
        vm.onFilter(SupervisionFilter.All)
        assertEquals(SampleData.reports.size, vm.uiState.value.reports.size)
    }

    @Test
    fun hidingAndShowingAReportFromItsDetail() = runTest {
        val fixture = AdminFixture()
        val vm = AdminReportDetailViewModel(fixture.auth, fixture.reports, "report-1")

        assertFalse(assertNotNull(vm.uiState.value.report).hidden)
        vm.toggleHidden()
        assertTrue(assertNotNull(vm.uiState.value.report).hidden)
        assertEquals("El reporte quedó oculto del mapa y de las bandejas.", vm.uiState.value.notice)
        assertTrue(fixture.reports.observeActiveReports().first().none { it.id == "report-1" })

        vm.toggleHidden()
        assertFalse(assertNotNull(vm.uiState.value.report).hidden)
        assertEquals("El reporte vuelve a estar visible.", vm.uiState.value.notice)
        assertTrue(fixture.reports.observeActiveReports().first().any { it.id == "report-1" })
    }

    @Test
    fun closingTheCaseAsksForConfirmationAndRecordsTheHistory() = runTest {
        val fixture = AdminFixture()
        val vm = AdminReportDetailViewModel(fixture.auth, fixture.reports, "report-3")
        val historyBefore = vm.uiState.value.history.size

        vm.requestClose()
        assertTrue(vm.uiState.value.confirmingClose)
        vm.dismissClose()
        assertEquals(ReportStatus.OnTheWay, fixture.reports.getReport("report-3")?.status)

        vm.requestClose()
        vm.confirmClose()

        assertFalse(vm.uiState.value.confirmingClose)
        assertEquals(ReportStatus.Closed, assertNotNull(vm.uiState.value.report).status)
        assertEquals("El caso se cerró.", vm.uiState.value.notice)
        val history = vm.uiState.value.history
        assertEquals(historyBefore + 1, history.size)
        assertEquals(ReportStatus.Closed, history.last().status)
        assertFalse(vm.uiState.value.canClose)
    }

    @Test
    fun showsTheServerErrorWhenAnActionFails() = runTest {
        val fixture = AdminFixture()
        val failing = object : ReportRepository by fixture.reports {
            override suspend fun setHidden(reportId: String, hidden: Boolean): Result<Report> =
                Result.failure(IllegalStateException("Solo el administrador puede ocultar reportes"))
        }
        val vm = AdminReportDetailViewModel(fixture.auth, failing, "report-1")

        vm.toggleHidden()

        assertEquals("Solo el administrador puede ocultar reportes", vm.uiState.value.message)
        assertFalse(assertNotNull(vm.uiState.value.report).hidden)
        vm.dismissMessage()
        assertNull(vm.uiState.value.message)
    }

    @Test
    fun anUnknownReportShowsItIsMissing() = runTest {
        val fixture = AdminFixture()
        val vm = AdminReportDetailViewModel(fixture.auth, fixture.reports, "report-x")
        assertEquals("El reporte no existe", vm.uiState.value.blockedMessage)
        assertNull(vm.uiState.value.report)
    }

    @Test
    fun aNonAdminSeesABlockedStateAndNoReports() = runTest {
        val fixture = AdminFixture(user = SampleData.demoUser)
        val list = ReportSupervisionViewModel(fixture.auth, fixture.reports)
        val detail = AdminReportDetailViewModel(fixture.auth, fixture.reports, "report-1")

        assertEquals(ADMIN_ONLY_MESSAGE, list.uiState.value.blockedMessage)
        assertTrue(list.uiState.value.reports.isEmpty())
        assertEquals(ADMIN_ONLY_MESSAGE, detail.uiState.value.blockedMessage)
        assertNull(detail.uiState.value.report)
    }
}
