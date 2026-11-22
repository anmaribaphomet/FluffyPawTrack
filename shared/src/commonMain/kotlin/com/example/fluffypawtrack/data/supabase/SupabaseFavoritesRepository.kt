package com.example.fluffypawtrack.data.supabase

import com.example.fluffypawtrack.domain.repository.FavoritesRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow

/** The `favorites` table; RLS limits each user to their own rows and blocks anonymous inserts. */
internal class SupabaseFavoritesRepository(
    private val client: SupabaseClient,
    private val queries: QueryRegistry,
) : FavoritesRepository {

    override fun observeFavoriteIds(userId: String): Flow<Set<String>> =
        queries.observe("favorites:$userId", emptySet()) { favoriteIds(userId) }

    /** The interface has no failure channel: a failed toggle leaves the saved state unchanged. */
    override suspend fun toggleFavorite(userId: String, listingId: String) {
        remote {
            if (listingId in favoriteIds(userId)) {
                client.from(TABLE).delete {
                    filter {
                        eq("user_id", userId)
                        eq("listing_id", listingId)
                    }
                }
            } else {
                client.from(TABLE).insert(FavoriteDto(userId = userId, listingId = listingId))
            }
        }.thenRefresh(queries)
    }

    private suspend fun favoriteIds(userId: String): Set<String> =
        client.from(TABLE).select { filter { eq("user_id", userId) } }
            .decodeList<FavoriteDto>()
            .mapTo(mutableSetOf()) { it.listingId }

    private companion object {
        const val TABLE = "favorites"
    }
}
