package com.example.fluffypawtrack.data

/** Failure messages shared by the fake and Supabase `device_tokens` repositories. */
internal object PushTokenFailures {
    const val NO_SESSION = "No session: sign in to manage push notifications"
    const val ANONYMOUS_SESSION = "Anonymous sessions do not receive push notifications"
    const val EMPTY_TOKEN = "Empty FCM token"
    const val UNKNOWN_DEVICE_TOKEN = "This device's FCM token is unknown"
}
