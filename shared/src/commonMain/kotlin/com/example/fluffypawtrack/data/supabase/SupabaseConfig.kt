package com.example.fluffypawtrack.data.supabase

/**
 * Supabase project settings. The Android app fills them from `local.properties` through
 * `BuildConfig`; they are never written to versioned files.
 */
data class SupabaseConfig(
    val url: String,
    val publishableKey: String,
) {
    /** False when either value is missing, so the app keeps using the fake repositories. */
    val isConfigured: Boolean
        get() = url.isNotBlank() && publishableKey.isNotBlank()

    /** Never prints the key, so the config is safe to log. */
    override fun toString(): String = "SupabaseConfig(url=$url, publishableKey=***)"
}
