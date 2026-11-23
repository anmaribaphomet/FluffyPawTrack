package com.example.fluffypawtrack.ui.push

import androidx.compose.foundation.layout.fillMaxWidth
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
import com.example.fluffypawtrack.ui.contact.CardActionButton
import com.example.fluffypawtrack.ui.map.ReportDetailSheetContent
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class ReportDetailUiState(
    val report: Report? = null,
    val loaded: Boolean = false,
    /** Readable message when loading failed or timed out; the screen offers "Reintentar". */
    val error: String? = null,
)

/** Loads the report a push notification points to (role `user`). */
class ReportDetailViewModel(
    private val reportId: String,
    private val reportRepository: ReportRepository,
    private val loadTimeout: Duration = 15.seconds,
) : ViewModel() {
    private val state = MutableStateFlow(ReportDetailUiState())
    val uiState: StateFlow<ReportDetailUiState> = state.asStateFlow()

    init {
        load()
    }

    fun onRetry() {
        state.value = ReportDetailUiState()
        load()
    }

    private fun load() {
        viewModelScope.launch {
            state.value = try {
                val report = withTimeout(loadTimeout) { reportRepository.getReport(reportId) }
                ReportDetailUiState(report = report, loaded = true)
            } catch (e: TimeoutCancellationException) {
                ReportDetailUiState(loaded = true, error = LOAD_ERROR)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ReportDetailUiState(loaded = true, error = LOAD_ERROR)
            }
        }
    }

    companion object {
        const val LOAD_ERROR = "No pudimos cargar el reporte. Revisa tu conexión e intenta de nuevo."
        const val NOT_AVAILABLE = "El reporte ya no está disponible."
    }
}

/** "Detalle del reporte" opened by tapping a push notification. */
@Composable
fun ReportDetailRoute(reportId: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = viewModel(key = "report-$reportId") { ReportDetailViewModel(reportId, container.reportRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReportDetailScreen(state = state, onBack = onBack, onRetry = viewModel::onRetry, modifier = modifier)
}

@Composable
fun ReportDetailScreen(
    state: ReportDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountScaffold(title = "Detalle del reporte", onBack = onBack, modifier = modifier) {
        val report = state.report
        when {
            report != null -> AccountCard { ReportDetailSheetContent(report) }
            state.error != null -> {
                AuthErrorMessage(state.error)
                CardActionButton("Reintentar", onClick = onRetry, modifier = Modifier.fillMaxWidth())
            }
            state.loaded -> AuthErrorMessage(ReportDetailViewModel.NOT_AVAILABLE)
            else -> Text("Cargando…", style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900)
        }
    }
}

@Preview
@Composable
private fun ReportDetailScreenPreview() {
    FluffyPawTrackTheme {
        ReportDetailScreen(ReportDetailUiState(report = SampleData.reports.first(), loaded = true), onBack = {}, onRetry = {})
    }
}
