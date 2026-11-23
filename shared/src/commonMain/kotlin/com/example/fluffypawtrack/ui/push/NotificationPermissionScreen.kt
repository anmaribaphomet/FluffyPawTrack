package com.example.fluffypawtrack.ui.push

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fluffypawtrack.platform.NotificationPermissionStatus
import com.example.fluffypawtrack.platform.rememberNotificationPermissionController
import com.example.fluffypawtrack.ui.account.AccountCard
import com.example.fluffypawtrack.ui.account.AccountScaffold
import com.example.fluffypawtrack.ui.contact.CardActionButton
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.ic_notification
import org.jetbrains.compose.resources.painterResource

/** Android 13 (API 33) added the runtime POST_NOTIFICATIONS permission. */
private const val POST_NOTIFICATIONS_API_LEVEL = 33

/**
 * RF-03: on Android 13 or newer, explain push notifications before asking for the permission,
 * unless it is granted, permanently denied, or the user already chose "Ahora no".
 */
fun shouldShowNotificationExplainer(status: NotificationPermissionStatus): Boolean =
    status.apiLevel >= POST_NOTIFICATIONS_API_LEVEL &&
        !status.granted &&
        !status.permanentlyDenied &&
        !status.dismissed

/** Shows [NotificationPermissionScreen] over the app while [active] (an account is signed in). */
@Composable
fun NotificationPermissionGate(active: Boolean) {
    val permission = rememberNotificationPermissionController()
    if (active && shouldShowNotificationExplainer(permission.status)) {
        NotificationPermissionScreen(onAllow = permission::request, onNotNow = permission::dismiss)
    }
}

@Composable
fun NotificationPermissionScreen(onAllow: () -> Unit, onNotNow: () -> Unit, modifier: Modifier = Modifier) {
    AccountScaffold(title = "Notificaciones", onBack = onNotNow, modifier = modifier) {
        AccountCard {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(painterResource(Res.drawable.ic_notification), contentDescription = null, Modifier.size(56.dp))
                Text(
                    "Activa las notificaciones",
                    style = MaterialTheme.typography.titleLarge,
                    color = FluffyColors.Brown900,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    "Te avisaremos cuando un albergue atienda tu reporte o haya reportes cerca de tu organización.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FluffyColors.Brown900,
                    textAlign = TextAlign.Center,
                )
                CardActionButton("Permitir notificaciones", onClick = onAllow, modifier = Modifier.fillMaxWidth())
                CardActionButton(
                    "Ahora no",
                    onClick = onNotNow,
                    color = FluffyColors.Peach300,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview
@Composable
private fun NotificationPermissionScreenPreview() {
    FluffyPawTrackTheme {
        NotificationPermissionScreen(onAllow = {}, onNotNow = {})
    }
}
