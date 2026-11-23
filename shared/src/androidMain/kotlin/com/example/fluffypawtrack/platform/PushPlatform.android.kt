package com.example.fluffypawtrack.platform

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual suspend fun currentPushToken(): String? = try {
    // getInstance() throws IllegalStateException when the build had no google-services.json.
    val task = FirebaseMessaging.getInstance().token
    suspendCancellableCoroutine { continuation ->
        task.addOnCompleteListener { result ->
            continuation.resume(if (result.isSuccessful) result.result?.takeIf { it.isNotBlank() } else null)
        }
    }
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    null
}

actual suspend fun deleteDevicePushToken() {
    try {
        val task = FirebaseMessaging.getInstance().deleteToken()
        suspendCancellableCoroutine { continuation ->
            task.addOnCompleteListener { continuation.resume(Unit) }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        // No Firebase config or no Play services: there is no token to delete.
    }
}
