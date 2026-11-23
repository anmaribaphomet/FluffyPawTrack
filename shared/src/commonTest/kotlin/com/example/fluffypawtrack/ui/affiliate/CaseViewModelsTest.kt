package com.example.fluffypawtrack.ui.affiliate

import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.domain.model.Priority
import com.example.fluffypawtrack.domain.model.ReportStatus
import com.example.fluffypawtrack.domain.model.ReportStatusRules
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CaseViewModelsTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun <T> TestScope.collecting(state: StateFlow<T>): StateFlow<T> {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
        return state
    }

    private fun AffiliateFakes.inbox() = ReportInboxViewModel(auth, reports)

    private fun AffiliateFakes.detail(reportId: String) = CaseDetailViewModel(reportId, auth, reports, organizations)

    // --- Bandeja de reportes ---

    @Test
    fun inboxListsOpenReportsNotTakenByOtherOrganizations() = runTest {
        val state = collecting(AffiliateFakes().inbox().uiState).value

        assertNull(state.blockedMessage)
        val ids = state.reports.map { it.id }
        assertTrue("report-1" in ids)
        assertTrue("report-7" in ids, "Own cases stay in the inbox")
        assertFalse("report-3" in ids, "Taken by Huellitas")
        assertFalse("report-6" in ids, "Closed")
    }

    @Test
    fun inboxFiltersByPriority() = runTest {
        val vm = AffiliateFakes().inbox()
        val state = collecting(vm.uiState)

        vm.selectPriority(Priority.Medium)
        assertTrue(state.value.reports.isNotEmpty())
        assertTrue(state.value.reports.all { it.priority == Priority.Medium })
        assertEquals(Priority.Medium, state.value.priorityFilter)

        vm.selectPriority(Priority.Low)
        assertEquals(listOf("report-1"), state.value.reports.map { it.id })

        vm.selectPriority(null)
        assertTrue(state.value.reports.size > 1)
    }

    @Test
    fun urgentFirstOrdersByPriority() = runTest {
        val vm = AffiliateFakes().inbox()
        val state = collecting(vm.uiState)

        vm.setUrgentFirst(true)

        val priorities = state.value.reports.map { it.priority }
        assertEquals(priorities.sortedDescending(), priorities)
    }

    @Test
    fun inboxIsBlockedForAccountsWithoutOrganization() = runTest {
        val state = collecting(AffiliateFakes(SampleData.demoUser).inbox().uiState).value

        assertEquals(AFFILIATE_ONLY_MESSAGE, state.blockedMessage)
        assertTrue(state.reports.isEmpty())
    }

    // --- Detalle del caso ---

    @Test
    fun pendingReportOffersTakeCase() = runTest {
        val state = collecting(AffiliateFakes().detail("report-1").uiState).value

        assertEquals("report-1", state.report?.id)
        assertTrue(state.canTake)
        assertNull(state.nextStatus)
        assertNull(state.takenByOtherLabel)
        assertEquals(listOf(ReportStatus.Pending), state.history.map { it.status })
    }

    @Test
    fun takingTheCaseMovesItOnTheWayAndOffersTheNextStatus() = runTest {
        val fakes = AffiliateFakes()
        val vm = fakes.detail("report-1")
        val state = collecting(vm.uiState)

        vm.takeCase()

        assertEquals(ReportStatus.OnTheWay, state.value.report?.status)
        assertEquals("org-patitas", state.value.report?.assignedOrganizationId)
        assertFalse(state.value.canTake)
        assertEquals(ReportStatus.Sheltered, state.value.nextStatus)
        assertEquals(listOf(ReportStatus.Pending, ReportStatus.OnTheWay), state.value.history.map { it.status })
        assertEquals("Patitas Callejeras HMO", state.value.history.last().responsibleName)
        assertNull(state.value.message)
    }

    @Test
    fun takingAnAlreadyTakenCaseShowsTheServerError() = runTest {
        val fakes = AffiliateFakes()
        val vm = fakes.detail("report-1")
        val state = collecting(vm.uiState)
        // Another organization takes it after the screen loaded.
        fakes.auth.updateUsers { it.copy(organizationId = if (it.id == SampleData.demoAffiliate.id) "org-huellitas" else it.organizationId) }
        fakes.reports.takeReport("report-1")
        fakes.auth.updateUsers { it.copy(organizationId = if (it.id == SampleData.demoAffiliate.id) "org-patitas" else it.organizationId) }

        vm.takeCase()

        assertEquals("Este caso ya fue tomado por otro albergue", state.value.message)
        assertFalse(state.value.canTake)
        assertEquals("En camino (Refugio Huellitas del Desierto)", state.value.takenByOtherLabel)
    }

    @Test
    fun caseOfAnotherOrganizationShowsEnCaminoWithItsNameAndNoActions() = runTest {
        val state = collecting(AffiliateFakes().detail("report-3").uiState).value

        assertEquals("En camino (Refugio Huellitas del Desierto)", state.takenByOtherLabel)
        assertFalse(state.canTake)
        assertNull(state.nextStatus)
    }

    @Test
    fun ownCaseOffersOnlyForwardStatusesAndClosingNeedsConfirmation() = runTest {
        val vm = AffiliateFakes().detail("report-7")
        val state = collecting(vm.uiState)

        assertEquals(ReportStatus.Sheltered, state.value.nextStatus)
        vm.advance()
        assertEquals(ReportStatus.Sheltered, state.value.report?.status)
        assertEquals(ReportStatus.Closed, state.value.nextStatus)

        vm.advance()
        assertTrue(state.value.confirmingClose, "Closing asks first")
        assertEquals(ReportStatus.Sheltered, state.value.report?.status)

        vm.dismissClose()
        assertFalse(state.value.confirmingClose)

        vm.advance()
        vm.confirmClose()
        assertEquals(ReportStatus.Closed, state.value.report?.status)
        assertNull(state.value.nextStatus)
        assertFalse(state.value.confirmingClose)
        assertEquals(ReportStatus.Closed, state.value.history.last().status)
    }

    @Test
    fun historyShowsTheReporterAsResponsibleOfTheFirstStatus() = runTest {
        val state = collecting(AffiliateFakes().detail("report-7").uiState).value

        assertEquals(ReportStatusRules.REPORTER_RESPONSIBLE, state.history.first().responsibleName)
    }

    @Test
    fun unknownReportShowsNotFound() = runTest {
        val state = collecting(AffiliateFakes().detail("missing").uiState).value

        assertNull(state.report)
        assertNotNull(state.message)
    }
}
