package com.example.fluffypawtrack.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fluffypawtrack.domain.model.AppNotification
import com.example.fluffypawtrack.domain.model.NotificationType
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyPawTrackTheme
import com.example.fluffypawtrack.ui.theme.FluffyShapes
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.ic_heart
import fluffypawtrack.shared.generated.resources.ic_notification
import fluffypawtrack.shared.generated.resources.ic_report
import fluffypawtrack.shared.generated.resources.ic_star
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private val NotificationType.icon: DrawableResource
    get() = when (this) {
        NotificationType.Report -> Res.drawable.ic_report
        NotificationType.Adoption -> Res.drawable.ic_heart
        NotificationType.Favorite -> Res.drawable.ic_star
        NotificationType.General -> Res.drawable.ic_notification
    }

/**
 * Tarjeta de una notificación (diseño "Nuevo reporte" de Figma).
 * Las no leídas usan el fondo del diseño y un punto naranja; las leídas, un fondo más tenue.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun NotificationCard(
    notification: AppNotification,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    now: Instant = Clock.System.now(),
) {
    val background = if (notification.isRead) FluffyColors.White.copy(alpha = 0.6f).copy(alpha = 0.35f) else FluffyColors.White.copy(alpha = 0.6f)
    Row(
        modifier = modifier
            .width(324.dp)
            .heightIn(min = 59.dp)
            .clip(FluffyShapes.OptionRow)
            .background(background)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 17.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(notification.type.icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = notification.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = FluffyColors.Brown900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!notification.isRead) {
                    Box(Modifier.size(6.dp).background(FluffyColors.Orange500, FluffyShapes.Ellipse))
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = relativeTimeLabel(notification.receivedAt, now),
                    style = MaterialTheme.typography.labelSmall,
                    color = FluffyColors.Brown900,
                )
            }
            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodyMedium,
                color = FluffyColors.Brown900,
            )
        }
    }
}

/** "Ahora", "Hace 5 min", "Hace 3 h", "Ayer", "Hace 4 días". */
@OptIn(ExperimentalTime::class)
internal fun relativeTimeLabel(time: Instant, now: Instant): String {
    val elapsed = now - time
    return when {
        elapsed < 1.minutes -> "Ahora"
        elapsed < 1.hours -> "Hace ${elapsed.inWholeMinutes} min"
        elapsed < 1.days -> "Hace ${elapsed.inWholeHours} h"
        elapsed < 2.days -> "Ayer"
        else -> "Hace ${elapsed.inWholeDays} días"
    }
}

@OptIn(ExperimentalTime::class)
@Preview
@Composable
private fun NotificationCardPreview() {
    val now = Clock.System.now()
    FluffyPawTrackTheme {
        Column(
            Modifier.background(FluffyColors.Peach200).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NotificationCard(
                AppNotification(
                    id = "1",
                    title = "Nuevo reporte",
                    message = "Gracias por tu aportación, te avisaremos apenas esté siendo atendido el caso.",
                    receivedAt = now - 5.minutes,
                    type = NotificationType.Report,
                ),
                onClick = {},
                now = now,
            )
            NotificationCard(
                AppNotification(
                    id = "2",
                    title = "Adopción disponible",
                    message = "Hay nuevos animalitos esperando una familia cerca de ti.",
                    receivedAt = now - 26.hours,
                    type = NotificationType.Adoption,
                    isRead = true,
                ),
                onClick = {},
                now = now,
            )
        }
    }
}
