package com.example.fluffypawtrack.ui.push

import com.example.fluffypawtrack.data.fake.FakeReportRepository
import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.domain.model.Report
import com.example.fluffypawtrack.domain.repository.ReportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ReportDetailViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadsTheReportFromTheNotification() {
        val report = SampleData.reports.first()

        val state = ReportDetailViewModel(report.id, FakeReportRepository()).uiState.value

        assertEquals(report.id, state.report?.id)
        assertTrue(state.loaded)
    }

    @Test
    fun anUnknownReportEndsLoadedWithoutAReport() {
        val state = ReportDetailViewModel("missing", FakeReportRepository()).uiState.value

        assertNull(state.report)
        assertNull(state.error)
        assertTrue(state.loaded)
    }

    @Test
    fun aFailedLoadShowsAnErrorAndRetryLoadsTheReport() {
        val report = SampleData.reports.first()
        var failNext = true
        val repository = reportsWith { id ->
            if (failNext) {
                failNext = false
                error("offline")
            }
            FakeReportRepository().getReport(id)
        }
        val viewModel = ReportDetailViewModel(report.id, repository)

        assertEquals(ReportDetailViewModel.LOAD_ERROR, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.report)

        viewModel.onRetry()

        assertEquals(report.id, viewModel.uiState.value.report?.id)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun aLoadThatNeverAnswersEndsInAnErrorAfterTheTimeout() = runTest {
        val viewModel = ReportDetailViewModel("any", reportsWith { awaitCancellation() }, loadTimeout = 15.seconds)
        assertFalse(viewModel.uiState.value.loaded)

        advanceTimeBy(15.seconds + 1.seconds)

        assertTrue(viewModel.uiState.value.loaded)
        assertEquals(ReportDetailViewModel.LOAD_ERROR, viewModel.uiState.value.error)
    }

    private fun reportsWith(load: suspend (String) -> Report?): ReportRepository =
        object : ReportRepository by FakeReportRepository() {
            override suspend fun getReport(id: String): Report? = load(id)
        }
}
