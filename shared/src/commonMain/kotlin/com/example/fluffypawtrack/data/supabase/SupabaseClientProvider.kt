package com.example.fluffypawtrack.data.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

/**
 * Builds the single [SupabaseClient] of the app. Auth keeps the session with the
 * library's default storage, so it survives app restarts.
 */
object SupabaseClientProvider {
    fun create(config: SupabaseConfig): SupabaseClient {
        require(config.isConfigured) { "Supabase settings are missing" }
        return createSupabaseClient(
            supabaseUrl = config.url.trim(),
            supabaseKey = config.publishableKey.trim(),
        ) {
            install(Auth)
            install(Postgrest)
            install(Storage)
        }
    }
}
