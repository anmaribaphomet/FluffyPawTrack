package com.example.fluffypawtrack.platform

// iOS is not built; push is Android only (FCM), so there is never a token.
actual suspend fun currentPushToken(): String? = null

actual suspend fun deleteDevicePushToken() = Unit
