package com.example.fluffypawtrack.platform

import androidx.compose.runtime.Composable

/**
 * The POST_NOTIFICATIONS permission as the explainer screen needs it. [apiLevel] is the Android
 * SDK level (0 on platforms without the permission); [dismissed] means the user chose "Ahora no"
 * (stored on the device) or already answered the system dialog in this session.
 */
data class NotificationPermissionStatus(
    val apiLevel: Int,
    val granted: Boolean,
    val permanentlyDenied: Boolean,
    val dismissed: Boolean,
)

interface NotificationPermissionController {
    /** Snapshot state: reading it in a composable recomposes when it changes. */
    val status: NotificationPermissionStatus

    /** Shows the system permission dialog. */
    fun request()

    /** "Ahora no": remembered on the device so the explainer is not shown on every launch. */
    fun dismiss()
}

/**
 * Android only lets an app show the system dialog twice; after a second denial it no longer appears.
 * A dismissed dialog also reports "denied" without counting for the system, so [denials] alone
 * overcounts: the rationale flag ([showRationale], true after a real first denial) breaks the tie.
 */
fun isNotificationPermissionPermanentlyDenied(granted: Boolean, denials: Int, showRationale: Boolean): Boolean =
    !granted && denials >= PERMANENT_DENIAL_COUNT && !showRationale

private const val PERMANENT_DENIAL_COUNT = 2

@Composable
expect fun rememberNotificationPermissionController(): NotificationPermissionController
