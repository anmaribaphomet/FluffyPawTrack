package com.example.fluffypawtrack.domain.model

/**
 * RF-03 notification preferences (`notification_prefs`). The defaults are the column defaults,
 * used while the user has no row yet.
 */
data class NotificationPrefs(
    val radiusKm: Double = DEFAULT_RADIUS_KM,
    val priorities: Set<Priority> = Priority.entries.toSet(),
    val pushEnabled: Boolean = true,
) {
    companion object {
        const val DEFAULT_RADIUS_KM = 5.0
        const val INVALID_RADIUS = "El radio debe ser mayor a 0 km"
    }
}
