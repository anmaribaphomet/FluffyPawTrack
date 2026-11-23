package com.example.fluffypawtrack.ui.affiliate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.domain.model.ReportStatus
import com.example.fluffypawtrack.domain.model.StatusHistoryEntry
import com.example.fluffypawtrack.ui.account.AccountCard
import com.example.fluffypawtrack.ui.account.AccountScaffold
import com.example.fluffypawtrack.ui.auth.AuthErrorMessage
import com.example.fluffypawtrack.ui.common.color
import com.example.fluffypawtrack.ui.common.formatDateTime
import com.example.fluffypawtrack.ui.common.label
import com.example.fluffypawtrack.ui.contact.CardActionButton
import com.example.fluffypawtrack.ui.map.ReportDetailSheetContent
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import kotlin.time.ExperimentalTime

/** Report detail for an affiliate: take the case and move it forward (RF-04). */
@Composable
fun CaseDetailRoute(reportId: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = viewModel(key = "case-$reportId") {
        CaseDetailViewModel(reportId, container.authRepository, container.reportRepository, container.organizationRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CaseDetailScreen(
        state = state,
        actions = CaseDetailActions(
            onTake = viewModel::takeCase,
            onAdvance = viewModel::advance,
            onConfirmClose = viewModel::confirmClose,
            onDismissClose = viewModel::dismissClose,
            onDismissMessage = viewModel::dismissMessage,
        ),
        onBack = onBack,
        modifier = modifier,
    )
}

data class CaseDetailActions(
    val onTake: () -> Unit = {},
    val onAdvance: () -> Unit = {},
    val onConfirmClose: () -> Unit = {},
    val onDismissClose: () -> Unit = {},
    val onDismissMessage: () -> Unit = {},
)

/** Button text for moving the case to [status]. */
internal val ReportStatus.actionLabel: String
    get() = when (this) {
        ReportStatus.Pending -> "Pendiente de atención"
        ReportStatus.OnTheWay -> "Marcar en camino"
        ReportStatus.Sheltered -> "Marcar como resguardado"
        ReportStatus.Closed -> "Cerrar caso"
    }

@Composable
fun CaseDetailScreen(
    state: CaseDetailUiState,
    actions: CaseDetailActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountScaffold(title = "Detalle del reporte", onBack = onBack, modifier = modifier) {
        state.message?.let { message ->
            AuthErrorMessage(message)
            if (state.report != null) {
                CardActionButton("Entendido", onClick = actions.onDismissMessage, color = FluffyColors.Peach300)
            }
        }
        val report = state.report ?: return@AccountScaffold
        AccountCard {
            ReportDetailSheetContent(report) {
                state.takenByOtherLabel?.let { taken ->
                    LabelChip(taken, report.status.color)
                }
                if (state.canTake) {
                    CardActionButton(
                        "Tomar caso",
                        onClick = actions.onTake,
                        enabled = !state.isWorking,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                state.nextStatus?.let { next ->
                    CardActionButton(
                        next.actionLabel,
                        onClick = actions.onAdvance,
                        enabled = !state.isWorking,
                        color = if (next == ReportStatus.Closed) FluffyColors.Peach300 else FluffyColors.Sage400,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        if (state.history.isNotEmpty()) {
            AccountCard {
                Text(
                    "Historial de estados",
                    style = MaterialTheme.typography.titleLarge,
                    color = FluffyColors.Brown900,
                    modifier = Modifier.semantics { heading() },
                )
                state.history.forEach { HistoryRow(it) }
            }
        }
    }
    if (state.confirmingClose) {
        AlertDialog(
            onDismissRequest = actions.onDismissClose,
            title = { Text("¿Cerrar el caso?", color = FluffyColors.Brown900) },
            text = {
                Text(
                    "El reporte pasará a \"${ReportStatus.Closed.label}\" y ya no podrá cambiar de estado.",
                    color = FluffyColors.Brown900,
                )
            },
            confirmButton = {
                TextButton(onClick = actions.onConfirmClose) { Text("Cerrar caso", color = FluffyColors.Brown900) }
            },
            dismissButton = {
                TextButton(onClick = actions.onDismissClose) { Text("Cancelar", color = FluffyColors.Brown900) }
            },
        )
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun HistoryRow(entry: StatusHistoryEntry) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.padding(top = 4.dp).size(12.dp).background(entry.status.color, CircleShape))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(entry.status.label, style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
            Text(formatDateTime(entry.changedAt), style = MaterialTheme.typography.bodySmall, color = FluffyColors.Brown900)
            Text(entry.responsibleName, style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900)
        }
    }
}

@Preview
@Composable
private fun CaseDetailScreenPreview() {
    FluffyPawTrackTheme {
        CaseDetailScreen(
            state = CaseDetailUiState(report = SampleData.reports.first(), canTake = true, loaded = true),
            actions = CaseDetailActions(),
            onBack = {},
        )
    }
}
