package com.example.fluffypawtrack.ui.affiliate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.ui.account.AccountScaffold
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme

/** "Mis casos en curso"; [onCaseClick] opens the case detail. */
@Composable
fun MyCasesRoute(onBack: () -> Unit, onCaseClick: (reportId: String) -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { MyCasesViewModel(container.authRepository, container.reportRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MyCasesScreen(state = state, onBack = onBack, onCaseClick = onCaseClick, modifier = modifier)
}

@Composable
fun MyCasesScreen(
    state: MyCasesUiState,
    onBack: () -> Unit,
    onCaseClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountScaffold(title = "Mis casos en curso", onBack = onBack, modifier = modifier) {
        state.blockedMessage?.let {
            InfoCard(it)
            return@AccountScaffold
        }
        if (state.loaded && state.cases.isEmpty()) {
            InfoCard("No tienes casos en curso. Toma uno desde la bandeja de reportes.")
        }
        state.cases.forEach { report -> ReportListCard(report, onClick = { onCaseClick(report.id) }) }
    }
}

@Preview
@Composable
private fun MyCasesScreenPreview() {
    FluffyPawTrackTheme {
        MyCasesScreen(
            state = MyCasesUiState(cases = SampleData.reports.filter { it.assignedOrganizationId != null }, loaded = true),
            onBack = {},
            onCaseClick = {},
        )
    }
}
