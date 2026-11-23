package com.example.fluffypawtrack.ui.affiliate

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fluffypawtrack.di.LocalAppContainer
import com.example.fluffypawtrack.ui.components.HomeHeader
import com.example.fluffypawtrack.ui.components.HomeHeaderHeight
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import com.example.fluffypawtrack.ui.theme.FluffySemanticColors
import com.example.fluffypawtrack.ui.theme.FluffyShadows
import com.example.fluffypawtrack.ui.theme.FluffyShapes
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.ic_chevron_right
import fluffypawtrack.shared.generated.resources.ic_heart
import fluffypawtrack.shared.generated.resources.ic_home
import fluffypawtrack.shared.generated.resources.ic_notification
import fluffypawtrack.shared.generated.resources.ic_person_outline
import fluffypawtrack.shared.generated.resources.ic_report
import fluffypawtrack.shared.generated.resources.ic_settings
import fluffypawtrack.shared.generated.resources.ic_star
import org.jetbrains.compose.resources.painterResource

/** Where each shortcut of the affiliate home leads; the parent wires them to navigation. */
data class AffiliateHomeNavigation(
    val onProfileClick: () -> Unit = {},
    val onInbox: () -> Unit = {},
    val onMyCases: () -> Unit = {},
    val onContactRequests: () -> Unit = {},
    val onAdoptionRequests: () -> Unit = {},
    val onMyListings: () -> Unit = {},
    val onOrganizationProfile: () -> Unit = {},
    val onPreferences: () -> Unit = {},
)

/** Home of an affiliate account (§6 Afiliado). */
@Composable
fun AffiliateHomeRoute(navigation: AffiliateHomeNavigation, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = viewModel {
        AffiliateHomeViewModel(container.authRepository, container.organizationRepository, container.reportRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AffiliateHomeScreen(state = state, navigation = navigation, modifier = modifier)
}

@Composable
fun AffiliateHomeScreen(
    state: AffiliateHomeUiState,
    navigation: AffiliateHomeNavigation,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(FluffySemanticColors.BackgroundHome)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = HomeHeaderHeight, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "¡Hola, ${state.organizationName.ifBlank { "equipo" }}!",
                modifier = Modifier.fillMaxWidth().semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
                color = FluffyColors.Brown900,
                textAlign = TextAlign.Center,
            )
            state.blockedMessage?.let {
                InfoCard(it)
                return@Column
            }
            ShortcutRow(
                painterResource(Res.drawable.ic_report),
                "Bandeja de reportes",
                pendingLabel(state.pendingReports),
                navigation.onInbox,
            )
            ShortcutRow(
                painterResource(Res.drawable.ic_home),
                "Mis casos en curso",
                casesLabel(state.openCases),
                navigation.onMyCases,
            )
            ShortcutRow(
                painterResource(Res.drawable.ic_notification),
                "Solicitudes de contacto",
                "\"Es mi mascota\" en los reportes de tu organización",
                navigation.onContactRequests,
            )
            if (state.isAdoptionCenter) {
                ShortcutRow(
                    painterResource(Res.drawable.ic_heart),
                    "Solicitudes de adopción",
                    "Aprueba o rechaza con motivo",
                    navigation.onAdoptionRequests,
                )
                ShortcutRow(
                    painterResource(Res.drawable.ic_star),
                    "Mis fichas de adopción",
                    "Publica, edita y marca como adoptado",
                    navigation.onMyListings,
                )
            }
            ShortcutRow(
                painterResource(Res.drawable.ic_person_outline),
                "Perfil de la organización",
                "Horario, contacto y foto",
                navigation.onOrganizationProfile,
            )
            ShortcutRow(
                painterResource(Res.drawable.ic_settings),
                "Preferencias de notificación",
                "Radio y prioridades",
                navigation.onPreferences,
            )
        }
        HomeHeader(onProfileClick = navigation.onProfileClick, modifier = Modifier.align(Alignment.TopCenter))
    }
}

private fun pendingLabel(count: Int): String = when (count) {
    0 -> "Sin reportes pendientes"
    1 -> "1 reporte pendiente"
    else -> "$count reportes pendientes"
}

private fun casesLabel(count: Int): String = when (count) {
    0 -> "Sin casos en curso"
    1 -> "1 caso en curso"
    else -> "$count casos en curso"
}

@Composable
private fun ShortcutRow(icon: Painter, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .dropShadow(FluffyShapes.Card, FluffyShadows.Drop)
            .clip(FluffyShapes.Card)
            .background(FluffyColors.White)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = FluffyColors.Brown900)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = FluffyColors.Brown900)
        }
        Image(painterResource(Res.drawable.ic_chevron_right), contentDescription = null, modifier = Modifier.size(16.dp))
    }
}

@Preview
@Composable
private fun AffiliateHomeScreenPreview() {
    FluffyPawTrackTheme {
        AffiliateHomeScreen(
            state = AffiliateHomeUiState(
                organizationName = "Patitas Callejeras HMO",
                isAdoptionCenter = true,
                pendingReports = 3,
                openCases = 1,
                loaded = true,
            ),
            navigation = AffiliateHomeNavigation(),
        )
    }
}
