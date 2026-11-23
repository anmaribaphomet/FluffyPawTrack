package com.example.fluffypawtrack.platform

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** This device's FCM token, or null when push is unavailable (iOS stub, or no Firebase config). */
expect suspend fun currentPushToken(): String?

/** Tokens FCM rotated while the app runs; the Android messaging service publishes them (`onNewToken`). */
object PushTokenRefreshes {
    private val tokens = MutableSharedFlow<String>(extraBufferCapacity = 8)

    val refreshed: SharedFlow<String> = tokens.asSharedFlow()

    fun publish(token: String) {
        tokens.tryEmit(token)
    }
}
