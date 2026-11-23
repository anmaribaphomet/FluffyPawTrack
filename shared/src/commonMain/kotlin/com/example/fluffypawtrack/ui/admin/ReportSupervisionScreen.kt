package com.example.fluffypawtrack.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.domain.model.Report
import com.example.fluffypawtrack.domain.model.ReportCategory
import com.example.fluffypawtrack.domain.model.StatusHistoryEntry
import com.example.fluffypawtrack.ui.account.AccountCard
import com.example.fluffypawtrack.ui.account.AccountScaffold
import com.example.fluffypawtrack.ui.common.formatDateTime
import com.example.fluffypawtrack.ui.common.label
import com.example.fluffypawtrack.ui.contact.CardActionButton
import com.example.fluffypawtrack.ui.contact.ReportThumbnail
import com.example.fluffypawtrack.ui.map.ReportDetailSheetContent
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import kotlin.time.ExperimentalTime

/** Admin → "Supervisión de reportes" (RF-07). [onReportClick] opens [AdminReportDetailRoute]. */
@Composable
fun ReportSupervisionRoute(onBack: () -> Unit, onReportClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { ReportSupervisionViewModel(container.authRepository, container.reportRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReportSupervisionScreen(
        state = state,
        onBack = onBack,
        onFilter = viewModel::onFilter,
        onCategory = viewModel::onCategory,
        onReportClick = onReportClick,
        modifier = modifier,
    )
}

private val CategoryOptions: List<ReportCategory?> = listOf(null) + ReportCategory.entries

@Composable
fun ReportSupervisionScreen(
    state: ReportSupervisionUiState,
    onBack: () -> Unit,
    onFilter: (SupervisionFilter) -> Unit,
    onCategory: (ReportCategory?) -> Unit,
    onReportClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountScaffold(title = "Supervisión de reportes", onBack = onBack, modifier = modifier) {
        state.blockedMessage?.let { blocked ->
            AccountCard { Text(blocked, style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900) }
            return@AccountScaffold
        }
        FilterRow(SupervisionFilter.entries, state.filter, { it.label }, onFilter)
        Text("Categoría", style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
        FilterRow(CategoryOptions, state.category, { it?.label ?: "Todas" }, onCategory)
        if (state.loaded && state.reports.isEmpty()) {
            AccountCard {
                Text("No hay reportes con estos filtros.", style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900)
            }
        }
        state.reports.forEach { report -> SupervisionRow(report) { onReportClick(report.id) } }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun SupervisionRow(report: Report, onClick: () -> Unit) {
    AccountCard(Modifier.clickable(role = Role.Button, onClickLabel = "Ver detalle del reporte", onClick = onClick)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ReportThumbnail(report)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(report.category.label, style = MaterialTheme.typography.titleMedium, color = FluffyColors.Brown900)
                Text(
                    "${report.status.label} · Prioridad ${report.priority.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = FluffyColors.Brown900,
                )
                Text(formatDateTime(report.createdAt), style = MaterialTheme.typography.bodySmall, color = FluffyColors.Brown900)
            }
        }
        if (report.hidden) AdminChip("Oculto", FluffyColors.Neutral300)
    }
}

/** Admin detail of one report with its status history and the RF-07 actions. */
@Composable
fun AdminReportDetailRoute(reportId: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = viewModel(key = "admin-report-$reportId") {
        AdminReportDetailViewModel(container.authRepository, container.reportRepository, reportId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AdminReportDetailScreen(
        state = state,
        onBack = onBack,
        onToggleHidden = viewModel::toggleHidden,
        onClose = viewModel::requestClose,
        onConfirmClose = viewModel::confirmClose,
        onDismissClose = viewModel::dismissClose,
        onDismissMessage = viewModel::dismissMessage,
        modifier = modifier,
    )
}

@Composable
fun AdminReportDetailScreen(
    state: AdminReportDetailUiState,
    onBack: () -> Unit,
    onToggleHidden: () -> Unit,
    onClose: () -> Unit,
    onConfirmClose: () -> Unit,
    onDismissClose: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountScaffold(title = "Detalle del reporte", onBack = onBack, modifier = modifier) {
        state.blockedMessage?.let { blocked ->
            AccountCard { Text(blocked, style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900) }
            return@AccountScaffold
        }
        val report = state.report ?: return@AccountScaffold
        ResultBanner(state.message, state.notice, onDismissMessage)
        AccountCard {
            if (report.hidden) AdminChip("Oculto", FluffyColors.Neutral300)
            ReportDetailSheetContent(report) {
                CardActionButton(
                    text = if (report.hidden) "Mostrar" else "Ocultar",
                    onClick = onToggleHidden,
                    color = FluffyColors.Peach300,
                    enabled = !state.isWorking,
                )
                if (state.canClose) {
                    CardActionButton(text = "Cerrar caso", onClick = onClose, enabled = !state.isWorking)
                }
            }
        }
        AccountCard { StatusHistory(state.history) }
    }
    if (state.confirmingClose) {
        ConfirmDialog(
            title = "Cerrar caso",
            text = "El reporte pasará a \"Caso cerrado\", saldrá del mapa y se avisará a quien lo hizo.",
            confirmText = "Cerrar caso",
            onConfirm = onConfirmClose,
            onDismiss = onDismissClose,
        )
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun StatusHistory(history: List<StatusHistoryEntry>) {
    Text("Historial de estados", style = MaterialTheme.typography.titleMedium, color = FluffyColors.Brown900)
    if (history.isEmpty()) {
        Text("Sin cambios registrados.", style = MaterialTheme.typography.bodySmall, color = FluffyColors.Brown900)
    }
    history.forEachIndexed { index, entry ->
        if (index > 0) HorizontalDivider(color = FluffyColors.Neutral300)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(entry.status.label, style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
            Text(
                "${formatDateTime(entry.changedAt)} · ${entry.responsibleName}",
                style = MaterialTheme.typography.bodySmall,
                color = FluffyColors.Brown900,
            )
        }
    }
}

@Preview
@Composable
private fun ReportSupervisionScreenPreview() {
    FluffyPawTrackTheme {
        ReportSupervisionScreen(
            state = ReportSupervisionUiState(loaded = true, allReports = SampleData.reports),
            onBack = {},
            onFilter = {},
            onCategory = {},
            onReportClick = {},
        )
    }
}
