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

/**
 * Deletes this device's FCM token in Firebase, so a row left in `device_tokens` stops receiving
 * pushes (FCM then reports it `UNREGISTERED`). The next token request creates a new one.
 * Never throws; on iOS and without Firebase config it does nothing.
 */
expect suspend fun deleteDevicePushToken()
