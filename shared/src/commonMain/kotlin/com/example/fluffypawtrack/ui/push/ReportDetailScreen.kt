package com.example.fluffypawtrack.ui.push

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.domain.model.Report
import com.example.fluffypawtrack.domain.repository.ReportRepository
import com.example.fluffypawtrack.ui.account.AccountCard
import com.example.fluffypawtrack.ui.account.AccountScaffold
import com.example.fluffypawtrack.ui.auth.AuthErrorMessage
import com.example.fluffypawtrack.ui.map.ReportDetailSheetContent
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReportDetailUiState(
    val report: Report? = null,
    val loaded: Boolean = false,
)

/** Loads the report a push notification points to (role `user`). */
class ReportDetailViewModel(reportId: String, reportRepository: ReportRepository) : ViewModel() {
    private val state = MutableStateFlow(ReportDetailUiState())
    val uiState: StateFlow<ReportDetailUiState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            state.value = ReportDetailUiState(report = reportRepository.getReport(reportId), loaded = true)
        }
    }
}

/** "Detalle del reporte" opened by tapping a push notification. */
@Composable
fun ReportDetailRoute(reportId: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = viewModel(key = "report-$reportId") { ReportDetailViewModel(reportId, container.reportRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReportDetailScreen(state = state, onBack = onBack, modifier = modifier)
}

@Composable
fun ReportDetailScreen(state: ReportDetailUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    AccountScaffold(title = "Detalle del reporte", onBack = onBack, modifier = modifier) {
        val report = state.report
        when {
            report != null -> AccountCard { ReportDetailSheetContent(report) }
            state.loaded -> AuthErrorMessage("No encontramos este reporte. Es posible que se haya eliminado.")
            else -> Text("Cargando…", style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900)
        }
    }
}

@Preview
@Composable
private fun ReportDetailScreenPreview() {
    FluffyPawTrackTheme {
        ReportDetailScreen(ReportDetailUiState(report = SampleData.reports.first(), loaded = true), onBack = {})
    }
}
