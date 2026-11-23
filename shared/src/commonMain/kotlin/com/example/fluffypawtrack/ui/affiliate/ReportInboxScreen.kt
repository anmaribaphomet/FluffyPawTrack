package com.example.fluffypawtrack.ui.affiliate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.domain.model.Priority
import com.example.fluffypawtrack.ui.account.AccountScaffold
import com.example.fluffypawtrack.ui.common.label
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme

/** "Bandeja de reportes"; [onReportClick] opens the case detail. */
@Composable
fun ReportInboxRoute(onBack: () -> Unit, onReportClick: (reportId: String) -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { ReportInboxViewModel(container.authRepository, container.reportRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReportInboxScreen(
        state = state,
        onBack = onBack,
        onSelectPriority = viewModel::selectPriority,
        onUrgentFirstChange = viewModel::setUrgentFirst,
        onReportClick = onReportClick,
        modifier = modifier,
    )
}

/** Filter chips: null is "Todas". */
private val PriorityOptions: List<Priority?> = listOf(null) + Priority.entries

@Composable
fun ReportInboxScreen(
    state: ReportInboxUiState,
    onBack: () -> Unit,
    onSelectPriority: (Priority?) -> Unit,
    onUrgentFirstChange: (Boolean) -> Unit,
    onReportClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountScaffold(title = "Bandeja de reportes", onBack = onBack, modifier = modifier) {
        state.blockedMessage?.let {
            InfoCard(it)
            return@AccountScaffold
        }
        Text("Prioridad", style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
        SelectableChips(
            options = PriorityOptions,
            isSelected = { it == state.priorityFilter },
            label = { it?.label ?: "Todas" },
            onSelect = onSelectPriority,
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Urgentes primero", style = MaterialTheme.typography.bodyLarge, color = FluffyColors.Brown900)
            Switch(
                checked = state.urgentFirst,
                onCheckedChange = onUrgentFirstChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = FluffyColors.Sage700,
                    checkedThumbColor = FluffyColors.White,
                    uncheckedTrackColor = FluffyColors.Cream100,
                    uncheckedThumbColor = FluffyColors.Brown600,
                    uncheckedBorderColor = FluffyColors.Sage700,
                ),
            )
        }
        if (state.loaded && state.reports.isEmpty()) {
            InfoCard("No hay reportes con esta prioridad.")
        }
        state.reports.forEach { report -> ReportListCard(report, onClick = { onReportClick(report.id) }) }
    }
}

@Preview
@Composable
private fun ReportInboxScreenPreview() {
    FluffyPawTrackTheme {
        ReportInboxScreen(
            state = ReportInboxUiState(reports = SampleData.reports.take(3), loaded = true),
            onBack = {},
            onSelectPriority = {},
            onUrgentFirstChange = {},
            onReportClick = {},
        )
    }
}
