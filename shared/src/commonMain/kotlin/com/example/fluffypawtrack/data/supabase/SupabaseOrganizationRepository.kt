package com.example.fluffypawtrack.data.supabase

import com.example.fluffypawtrack.domain.model.Organization
import com.example.fluffypawtrack.domain.model.OrganizationDraft
import com.example.fluffypawtrack.domain.model.OrganizationProfile
import com.example.fluffypawtrack.domain.model.OrganizationRules
import com.example.fluffypawtrack.domain.model.OrganizationStatus
import com.example.fluffypawtrack.domain.model.OrganizationType
import com.example.fluffypawtrack.domain.repository.OrganizationRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder
import io.github.jan.supabase.postgrest.query.PostgrestRequestBuilder
import io.github.jan.supabase.postgrest.result.PostgrestResult
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Map places through PostgREST; affiliation requests through the `request_affiliation` RPC. */
internal class SupabaseOrganizationRepository(
    private val client: SupabaseClient,
    private val queries: QueryRegistry,
    private val photos: PhotoUploader,
) : OrganizationRepository {

    override fun observeActiveOrganizations(): Flow<List<Organization>> =
        queries.observe("organizations:active", emptyList()) {
            client.from(TABLE).select {
                filter {
                    eq("status", OrganizationStatus.Approved.dbValue)
                    eq("active", true)
                }
            }.decodeList<OrganizationDto>().map { it.toDomain() }
        }

    override suspend fun getOrganization(id: String): Organization? = remoteOrNull {
        client.from(TABLE).select { filter { eq("id", id) } }
            .decodeList<OrganizationDto>().firstOrNull()?.toDomain()
    }

    override suspend fun requestAffiliation(
        type: OrganizationType,
        name: String,
        address: String,
        phone: String,
        email: String,
        description: String,
        photoUrl: String,
    ): Result<Organization> = remote {
        // Same checks as the RPC, before uploading a photo for a form that would be rejected.
        if (listOf(name, address, phone, email, description).any { it.isBlank() }) {
            throw RuleViolation("Completa todos los datos de la organización")
        }
        if (photoUrl.isBlank()) throw RuleViolation("Agrega una foto del lugar o su logo")
        val uid = client.auth.currentUserOrNull()?.id
            ?: throw RuleViolation("Necesitas una cuenta para solicitar la afiliación")
        val storedPhoto = photos.store(photoUrl, StoragePaths.PLACE_PHOTOS, folder = uid)
        client.postgrest.rpc(
            "request_affiliation",
            buildJsonObject {
                put("p_type", type.dbValue)
                put("p_name", name)
                put("p_address", address)
                put("p_phone", phone)
                put("p_email", email)
                put("p_description", description)
                put("p_photo_url", storedPhoto)
            },
        ).decodeAs<OrganizationDto>().toDomain()
    }.thenRefresh(queries)

    /** RLS shows pending organizations only to the admin and to their requester. */
    override fun observePendingAffiliations(): Flow<List<Organization>> =
        queries.observe("organizations:pending", emptyList()) {
            client.from(TABLE).select {
                filter { eq("status", OrganizationStatus.Pending.dbValue) }
                order("created_at", Order.ASCENDING)
            }.decodeList<OrganizationDto>().map { it.toDomain() }
        }

    override suspend fun reviewAffiliation(organizationId: String, approve: Boolean, reason: String?): Result<Organization> = remote {
        client.postgrest.rpc(
            "review_affiliation",
            buildJsonObject {
                put("p_organization_id", organizationId)
                put("p_approve", approve)
                put("p_reason", reason)
            },
        ).decodeAs<OrganizationDto>().toDomain()
    }.thenRefresh(queries)

    override fun observeAllForAdmin(): Flow<List<Organization>> =
        queries.observe("organizations:admin", emptyList()) {
            client.from(TABLE).select { order("name", Order.ASCENDING) }
                .decodeList<OrganizationDto>().map { it.toDomain() }
        }

    /** The insert policy for admins is `organizations_insert_admin`; others get a generic failure. */
    override suspend fun createOrganization(draft: OrganizationDraft): Result<Organization> = remote {
        OrganizationRules.problemOf(draft)?.let { throw RuleViolation(it) }
        val photo = storePlacePhoto(draft.photoUrl)
        client.from(TABLE).insert(draft.toWriteDto(photo).asNewApproved()) { select() }
            .decodeList<OrganizationDto>().first().toDomain()
    }.thenRefresh(queries)

    override suspend fun updateOrganization(organizationId: String, draft: OrganizationDraft): Result<Organization> =
        remote {
            OrganizationRules.problemOf(draft)?.let { throw RuleViolation(it) }
            val photo = storePlacePhoto(draft.photoUrl)
            updated(NOT_FOUND) { update(draft.toWriteDto(photo)) { selectOne(organizationId) } }
        }.thenRefresh(queries)

    override suspend fun setActive(organizationId: String, active: Boolean): Result<Organization> = remote {
        updated(NOT_FOUND) {
            update(buildJsonObject { put("active", active) }) { selectOne(organizationId) }
        }
    }.thenRefresh(queries)

    /** `20261122000600_review_fixes.sql` returns the organization's affiliates to `user` first. */
    override suspend fun deleteOrganization(organizationId: String): Result<Unit> = remote {
        val deleted = client.from(TABLE).delete { selectOne(organizationId) }.decodeList<OrganizationDto>()
        // RLS turns a forbidden delete into "no rows", like an unknown id.
        if (deleted.isEmpty()) throw RuleViolation(NOT_FOUND)
    }.thenRefresh(queries)

    /** `organizations_update_member` plus its guard trigger: only the profile columns change. */
    override suspend fun updateOwnOrganization(
        organizationId: String,
        profile: OrganizationProfile,
    ): Result<Organization> = remote {
        OrganizationRules.problemOf(profile)?.let { throw RuleViolation(it) }
        val photo = storePlacePhoto(profile.photoUrl)
        updated("Solo los afiliados de la organización pueden editar su perfil") {
            update(profile.toDto(photo)) { selectOne(organizationId) }
        }
    }.thenRefresh(queries)

    private suspend fun storePlacePhoto(ref: String): String {
        val uid = client.auth.currentUserOrNull()?.id ?: throw RuleViolation("Inicia sesión para continuar")
        return photos.store(ref, StoragePaths.PLACE_PHOTOS, folder = uid)
    }

    private fun PostgrestRequestBuilder.selectOne(organizationId: String) {
        select()
        filter { eq("id", organizationId) }
    }

    /** Runs an update on [TABLE]; no returned row means RLS refused it or the id is unknown. */
    private suspend fun updated(
        refusal: String,
        request: suspend PostgrestQueryBuilder.() -> PostgrestResult,
    ): Organization = client.from(TABLE).request().decodeList<OrganizationDto>().firstOrNull()?.toDomain()
        ?: throw RuleViolation(refusal)

    private companion object {
        const val TABLE = "organizations"
        const val NOT_FOUND = "La organización no existe"
    }
}
