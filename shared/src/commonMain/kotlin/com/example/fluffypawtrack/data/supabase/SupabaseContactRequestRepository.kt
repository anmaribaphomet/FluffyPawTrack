package com.example.fluffypawtrack.data.supabase

import com.example.fluffypawtrack.domain.model.ContactDetails
import com.example.fluffypawtrack.domain.model.ContactRequest
import com.example.fluffypawtrack.domain.repository.ContactRequestRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * RF-05 contact requests. Clients can only read `contact_requests`; every write goes through
 * the RPCs, which check the rules and raise the same Spanish messages as the fake. The user
 * ids of the interface are not sent: the server takes the caller from the session.
 */
internal class SupabaseContactRequestRepository(
    private val client: SupabaseClient,
    private val queries: QueryRegistry,
    private val photos: PhotoUploader,
) : ContactRequestRepository {

    override fun observeSent(userId: String): Flow<List<ContactRequest>> =
        queries.observe("contact:sent:$userId", emptyList()) { select { eq("requester_id", userId) } }

    /**
     * RLS only returns the rows the caller sent or receives (as the reporter or as an affiliate
     * of the recipient organization), so "not sent by me" is the received list.
     */
    override fun observeReceived(userId: String): Flow<List<ContactRequest>> =
        queries.observe("contact:received:$userId", emptyList()) { select { neq("requester_id", userId) } }

    override suspend fun send(
        reportId: String,
        requesterId: String,
        message: String,
        photoUrl: String?,
    ): Result<ContactRequest> = remote {
        // Proof photos share the report-photos bucket: its policy lets any session write its own folder.
        val storedPhoto = photoUrl?.let { photos.store(it, StoragePaths.REPORT_PHOTOS, folder = requesterId) }
        rpc(
            "send_contact_request",
            buildJsonObject {
                put("p_report_id", reportId)
                put("p_message", message)
                put("p_photo_url", storedPhoto)
            },
        )
    }.thenRefresh(queries)

    override suspend fun cancel(requestId: String, userId: String): Result<ContactRequest> =
        remote { rpc("cancel_contact_request", requestParams(requestId)) }.thenRefresh(queries)

    override suspend fun accept(requestId: String, userId: String): Result<ContactRequest> =
        remote { rpc("respond_contact_request", respondParams(requestId, accept = true)) }.thenRefresh(queries)

    override suspend fun reject(requestId: String, userId: String): Result<ContactRequest> =
        remote { rpc("respond_contact_request", respondParams(requestId, accept = false)) }.thenRefresh(queries)

    /** Also closes the report and rejects its other pending requests, in one transaction. */
    override suspend fun markDelivered(requestId: String, userId: String): Result<ContactRequest> =
        remote { rpc("mark_pet_delivered", requestParams(requestId)) }.thenRefresh(queries)

    override suspend fun getContactDetails(requestId: String, userId: String): ContactDetails? = remoteOrNull {
        client.postgrest.rpc("get_contact_details", buildJsonObject { put("request_id", requestId) })
            .decodeList<ContactDetailsDto>()
            .firstOrNull()
            ?.toDomain()
    }

    private suspend fun rpc(name: String, params: JsonObject): ContactRequest =
        client.postgrest.rpc(name, params).decodeAs<ContactRequestDto>().toDomain()

    private suspend fun select(filters: PostgrestFilterBuilder.() -> Unit): List<ContactRequest> =
        client.from("contact_requests").select { filter(filters) }
            .decodeList<ContactRequestDto>()
            .map { it.toDomain() }

    private fun requestParams(requestId: String) = buildJsonObject { put("p_request_id", requestId) }

    private fun respondParams(requestId: String, accept: Boolean) = buildJsonObject {
        put("p_request_id", requestId)
        put("p_accept", accept)
    }
}
