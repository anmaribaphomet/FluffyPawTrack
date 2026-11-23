package com.example.fluffypawtrack.push

import com.example.fluffypawtrack.platform.PushTokenRefreshes
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives FCM messages. The Edge Function sends data messages with `title`, `body` and
 * `report_id` (see supabase/README.md), so the app builds the notification itself, also in the
 * background; a message with a `notification` block is shown by the system on the same channel.
 */
class PawTrackMessagingService : FirebaseMessagingService() {

    /** The running app re-registers it; otherwise the next app start registers the current token. */
    override fun onNewToken(token: String) {
        PushTokenRefreshes.publish(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = data["title"] ?: message.notification?.title ?: return
        val body = data["body"] ?: message.notification?.body.orEmpty()
        PushNotifications.show(this, title, body, data[PushNotifications.EXTRA_REPORT_ID])
    }
}
