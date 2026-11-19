package com.example.fluffypawtrack.model

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Categoría de la notificación; define el ícono que se muestra. */
enum class NotificationType {
    /** Avisos sobre un reporte de animal (creado, en atención, resuelto…). */
    Report,

    /** Novedades de adopciones. */
    Adoption,

    /** Cambios en animales o publicaciones guardadas. */
    Favorite,

    /** Cualquier otro aviso de la app. */
    General,
}

/** Notificación recibida por el usuario. */
@OptIn(ExperimentalTime::class)
data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val receivedAt: Instant,
    val type: NotificationType = NotificationType.General,
    val isRead: Boolean = false,
)
