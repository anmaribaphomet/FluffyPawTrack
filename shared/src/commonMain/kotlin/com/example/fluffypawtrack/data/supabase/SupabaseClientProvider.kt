package com.example.fluffypawtrack.data.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json

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
            // Rows carry columns the DTOs do not map (hidden, updated_at, requested_by, ...).
            defaultSerializer = KotlinXSerializer(Json { ignoreUnknownKeys = true })
            install(Auth)
            install(Postgrest)
            install(Storage)
        }
    }
}
