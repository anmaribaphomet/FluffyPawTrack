package com.example.fluffypawtrack.data.supabase

import com.example.fluffypawtrack.domain.model.Organization
import com.example.fluffypawtrack.domain.model.OrganizationStatus
import com.example.fluffypawtrack.domain.model.OrganizationType
import com.example.fluffypawtrack.domain.repository.OrganizationRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
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

    private companion object {
        const val TABLE = "organizations"
    }
}
