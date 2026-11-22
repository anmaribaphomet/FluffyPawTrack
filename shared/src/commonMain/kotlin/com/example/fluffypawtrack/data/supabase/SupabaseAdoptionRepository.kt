package com.example.fluffypawtrack.data.supabase

import com.example.fluffypawtrack.domain.model.AdoptionListing
import com.example.fluffypawtrack.domain.model.ListingRules
import com.example.fluffypawtrack.domain.model.ListingStatus
import com.example.fluffypawtrack.domain.model.NewListing
import com.example.fluffypawtrack.domain.repository.AdoptionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
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
    private val photos: PhotoUploader,
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

    /** `adoption_listings_insert_center`: the caller's approved adoption center only. */
    override suspend fun createListing(listing: NewListing): Result<AdoptionListing> = remote {
        ListingRules.problemOf(listing)?.let { throw RuleViolation(it) }
        val organizationId = myOrganizationId() ?: throw RuleViolation(NOT_A_CENTER)
        val photoUrls = storePhotos(listing.photoUrls, organizationId)
        client.from(TABLE).insert(listing.toInsertDto(organizationId, photoUrls)) { select() }
            .decodeList<ListingDto>().firstOrNull()?.toDomain()
            ?: throw RuleViolation(NOT_A_CENTER)
    }.thenRefresh(queries)

    override suspend fun updateListing(listingId: String, listing: NewListing): Result<AdoptionListing> = remote {
        ListingRules.problemOf(listing)?.let { throw RuleViolation(it) }
        val current = getListing(listingId) ?: throw RuleViolation("El animal no existe")
        val photoUrls = storePhotos(listing.photoUrls, current.organizationId)
        client.from(TABLE).update(listing.toUpdateDto(photoUrls)) {
            select()
            filter { eq("id", listingId) }
        }.decodeList<ListingDto>().firstOrNull()?.toDomain()
            // RLS turns an edit by another center into "no rows".
            ?: throw RuleViolation("Solo el centro de este animal puede editarlo")
    }.thenRefresh(queries)

    /** The caller's organization from their own profile row (`profiles_select_own`). */
    private suspend fun myOrganizationId(): String? {
        val uid = client.auth.currentUserOrNull()?.id ?: return null
        return client.from("profiles").select { filter { eq("id", uid) } }
            .decodeList<ProfileDto>().firstOrNull()
            ?.takeIf { it.role == "affiliate" }?.organizationId
    }

    /** Uploads local photos to `adoption-photos/<organizationId>/`, keeping their order. */
    private suspend fun storePhotos(refs: List<String>, organizationId: String): List<String> =
        refs.filter { it.isNotBlank() }.map { photos.store(it, StoragePaths.ADOPTION_PHOTOS, folder = organizationId) }

    private suspend fun selectListings(filters: PostgrestFilterBuilder.() -> Unit): List<AdoptionListing> =
        client.from(TABLE).select {
            filter(filters)
            order("created_at", Order.ASCENDING)
        }.decodeList<ListingDto>()
            // The table constraint guarantees a photo; this keeps the interface promise even so.
            .filter { it.photoUrls.isNotEmpty() }
            .map { it.toDomain() }

    private companion object {
        const val TABLE = "adoption_listings"
        const val NOT_A_CENTER = "Solo los centros de adopción afiliados pueden publicar animales"
    }
}
