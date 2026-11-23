package com.example.fluffypawtrack.ui.push

import com.example.fluffypawtrack.data.fake.FakeReportRepository
import com.example.fluffypawtrack.data.fake.SampleData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
        assertTrue(state.loaded)
    }
}
