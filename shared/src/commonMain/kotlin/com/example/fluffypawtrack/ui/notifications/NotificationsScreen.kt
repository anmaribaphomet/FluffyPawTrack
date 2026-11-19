package com.example.fluffypawtrack.ui.notifications

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fluffypawtrack.model.AppNotification
import com.example.fluffypawtrack.model.NotificationType
import com.example.fluffypawtrack.ui.components.NotificationCard
import com.example.fluffypawtrack.ui.components.ProfileHeader
import com.example.fluffypawtrack.ui.theme.FluffyPalette
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import com.example.fluffypawtrack.ui.theme.FluffyShadows
import com.example.fluffypawtrack.ui.theme.FluffyShapes
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.ic_notification
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

/** Pantalla SECCION DE NOTIFICACIONES (Figma nodo 75:207). */
@OptIn(ExperimentalTime::class)
@Composable
fun NotificationsScreen(
    notifications: List<AppNotification>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNotificationClick: (AppNotification) -> Unit = {},
) {
    val now = Clock.System.now()
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 167.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "title") {
                Text(
                    text = "Notificaciones",
                    modifier = Modifier.fillMaxWidth().padding(start = 13.dp, bottom = 13.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = FluffyPalette.TextBrown,
                )
            }
            if (notifications.isEmpty()) {
                item(key = "empty") {
                    EmptyNotifications(Modifier.fillParentMaxHeight(0.6f))
                }
            } else {
                items(notifications, key = { it.id }) { notification ->
                    NotificationCard(
                        notification = notification,
                        onClick = { onNotificationClick(notification) },
                        now = now,
                    )
                }
            }
        }

        ProfileHeader(onBack = onBack, showLogoBadge = false, modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun EmptyNotifications(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.width(260.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(80.dp)
                .dropShadow(FluffyShapes.Ellipse, FluffyShadows.Drop)
                .background(FluffyPalette.Sand, FluffyShapes.Ellipse),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(Res.drawable.ic_notification), contentDescription = null, modifier = Modifier.size(42.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No tienes notificaciones",
            style = MaterialTheme.typography.titleMedium,
            color = FluffyPalette.TextBrown,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Aquí verás los avisos sobre tus reportes y adopciones.",
            style = MaterialTheme.typography.labelMedium,
            color = FluffyPalette.MessageBrown,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalTime::class)
private fun sampleNotifications(): List<AppNotification> {
    val now = Clock.System.now()
    return listOf(
        AppNotification(
            id = "n1",
            title = "Nuevo reporte",
            message = "Gracias por tu aportación, te avisaremos apenas esté siendo atendido el caso.",
            receivedAt = now - 5.minutes,
            type = NotificationType.Report,
        ),
        AppNotification(
            id = "n2",
            title = "Reporte en atención",
            message = "Un refugio cercano ya está atendiendo el caso que reportaste.",
            receivedAt = now - 3.hours,
            type = NotificationType.Report,
        ),
        AppNotification(
            id = "n3",
            title = "Nuevos animalitos",
            message = "Hay nuevos animalitos en adopción cerca de ti. ¡Conócelos!",
            receivedAt = now - 2.days,
            type = NotificationType.Adoption,
            isRead = true,
        ),
    )
}

@Preview
@Composable
private fun NotificationsScreenPreview() {
    FluffyPawTrackTheme {
        NotificationsScreen(notifications = sampleNotifications(), onBack = {})
    }
}

@Preview
@Composable
private fun NotificationsScreenEmptyPreview() {
    FluffyPawTrackTheme {
        NotificationsScreen(notifications = emptyList(), onBack = {})
    }
}
