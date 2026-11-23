package com.example.fluffypawtrack.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.ui.account.AccountCard
import com.example.fluffypawtrack.ui.components.HomeHeader
import com.example.fluffypawtrack.ui.components.HomeHeaderHeight
import com.example.fluffypawtrack.ui.components.ProfileOptionRow
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import com.example.fluffypawtrack.ui.theme.FluffySemanticColors
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.ic_map
import fluffypawtrack.shared.generated.resources.ic_person_outline
import fluffypawtrack.shared.generated.resources.ic_report
import org.jetbrains.compose.resources.painterResource

/** Admin home (REQUIREMENTS §6 "Administrador"): shortcuts to the three RF-07 sections. */
@Composable
fun AdminHomeRoute(
    onProfileClick: () -> Unit,
    onAffiliationRequests: () -> Unit,
    onPlacesDirectory: () -> Unit,
    onReportSupervision: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { AdminHomeViewModel(container.authRepository, container.organizationRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AdminHomeScreen(
        state = state,
        onProfileClick = onProfileClick,
        onAffiliationRequests = onAffiliationRequests,
        onPlacesDirectory = onPlacesDirectory,
        onReportSupervision = onReportSupervision,
        modifier = modifier,
    )
}

internal fun pendingAffiliationsText(count: Int): String = when (count) {
    0 -> "Sin solicitudes pendientes"
    1 -> "1 solicitud pendiente"
    else -> "$count solicitudes pendientes"
}

@Composable
fun AdminHomeScreen(
    state: AdminHomeUiState,
    onProfileClick: () -> Unit,
    onAffiliationRequests: () -> Unit,
    onPlacesDirectory: () -> Unit,
    onReportSupervision: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(FluffySemanticColors.BackgroundHome)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = HomeHeaderHeight, bottom = 32.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = "Panel de\nadministración",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.displaySmall,
                color = FluffyColors.Brown900,
                textAlign = TextAlign.Center,
            )
            val blocked = state.blockedMessage
            if (blocked != null) {
                AccountCard {
                    Text(blocked, style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900)
                }
            } else {
                ProfileOptionRow(
                    icon = painterResource(Res.drawable.ic_person_outline),
                    title = "Solicitudes de afiliación",
                    subtitle = pendingAffiliationsText(state.pendingCount),
                    onClick = onAffiliationRequests,
                )
                ProfileOptionRow(
                    icon = painterResource(Res.drawable.ic_map),
                    title = "Directorio de lugares",
                    subtitle = "Albergues, centros, veterinarias y puntos seguros",
                    onClick = onPlacesDirectory,
                )
                ProfileOptionRow(
                    icon = painterResource(Res.drawable.ic_report),
                    title = "Supervisión de reportes",
                    subtitle = "Ver, ocultar y cerrar reportes",
                    onClick = onReportSupervision,
                )
            }
        }
        HomeHeader(onProfileClick = onProfileClick, modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Preview
@Composable
private fun AdminHomeScreenPreview() {
    FluffyPawTrackTheme {
        AdminHomeScreen(
            state = AdminHomeUiState(pendingCount = 1),
            onProfileClick = {},
            onAffiliationRequests = {},
            onPlacesDirectory = {},
            onReportSupervision = {},
        )
    }
}
