package com.example.fluffypawtrack.data.supabase

import com.example.fluffypawtrack.domain.model.AdoptionListing
import com.example.fluffypawtrack.domain.model.ListingStatus
import com.example.fluffypawtrack.domain.repository.AdoptionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Listings through PostgREST. `mark_listing_adopted` checks the affiliate server-side, and its
 * trigger rejects the pending requests with "El animal ya fue adoptado" and notifies them.
 */
internal class SupabaseAdoptionRepository(
    private val client: SupabaseClient,
    private val queries: QueryRegistry,
) : AdoptionRepository {

    override fun observeAvailableListings(): Flow<List<AdoptionListing>> =
        queries.observe("listings:available", emptyList()) {
            selectListings { eq("status", ListingStatus.Available.dbValue) }
        }

    override fun observeListingsBy(organizationId: String): Flow<List<AdoptionListing>> =
        queries.observe("listings:by:$organizationId", emptyList()) {
            selectListings { eq("organization_id", organizationId) }
        }

    override suspend fun getListing(id: String): AdoptionListing? = remoteOrNull {
        selectListings { eq("id", id) }.firstOrNull()
    }

    /** The server identifies the affiliate from the session; [affiliateUserId] is not sent. */
    override suspend fun markAdopted(listingId: String, affiliateUserId: String): Result<AdoptionListing> = remote {
        client.postgrest.rpc("mark_listing_adopted", buildJsonObject { put("p_listing_id", listingId) })
            .decodeAs<ListingDto>()
            .toDomain()
    }.thenRefresh(queries)

    private suspend fun selectListings(filters: PostgrestFilterBuilder.() -> Unit): List<AdoptionListing> =
        client.from("adoption_listings").select {
            filter(filters)
            order("created_at", Order.ASCENDING)
        }.decodeList<ListingDto>()
            // The table constraint guarantees a photo; this keeps the interface promise even so.
            .filter { it.photoUrls.isNotEmpty() }
            .map { it.toDomain() }
}
