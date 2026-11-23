package com.example.fluffypawtrack.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.fluffypawtrack.MainActivity
import com.example.fluffypawtrack.R
import com.example.fluffypawtrack.ui.theme.FluffyColors

/** The "Avisos" channel and the notifications shown for FCM data messages (RF-03). */
object PushNotifications {
    /** Also declared as FCM's default channel in the manifest. */
    const val CHANNEL_ID = "avisos"

    /** Intent extra (and FCM data key) with the report a notification opens. */
    const val EXTRA_REPORT_ID = "report_id"

    /** Creating an existing channel is a no-op, so this is safe on every start. */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Avisos", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Reportes cercanos y cambios en tus reportes y solicitudes."
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun show(context: Context, title: String, body: String, reportId: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel(context)
        val id = System.currentTimeMillis().toInt()
        val intent = Intent(context, MainActivity::class.java).apply {
            // Reuses the running activity (onNewIntent) instead of stacking a second one.
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (!reportId.isNullOrBlank()) putExtra(EXTRA_REPORT_ID, reportId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_paw)
            .setColor(FluffyColors.Orange700.toArgb())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // The permission was revoked between the check and the call.
        }
    }
}
