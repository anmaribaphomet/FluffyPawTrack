package com.example.fluffypawtrack.data.supabase

import com.example.fluffypawtrack.domain.model.AdoptionRequest
import com.example.fluffypawtrack.domain.model.NewAdoptionRequest
import com.example.fluffypawtrack.domain.repository.AdoptionRequestRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * RF-06 adoption requests through the `submit_adoption_request`, `respond_adoption_request` and
 * `cancel_adoption_request` RPCs, which enforce the rules, insert the RF-06 notifications and
 * raise the same Spanish messages as the fake. The server takes the caller from the session.
 */
internal class SupabaseAdoptionRequestRepository(
    private val client: SupabaseClient,
    private val queries: QueryRegistry,
) : AdoptionRequestRepository {

    override suspend fun submit(requesterId: String?, request: NewAdoptionRequest): Result<AdoptionRequest> {
        if (requesterId == null) return Result.failure(RuleViolation(ACCOUNT_REQUIRED))
        return remote { rpc("submit_adoption_request", request.toSubmitParams()) }.thenRefresh(queries)
    }

    override suspend fun cancel(requestId: String, userId: String): Result<AdoptionRequest> =
        remote { rpc("cancel_adoption_request", buildJsonObject { put("p_request_id", requestId) }) }
            .thenRefresh(queries)

    override suspend fun approve(requestId: String, affiliateUserId: String): Result<AdoptionRequest> =
        remote { rpc("respond_adoption_request", respondParams(requestId, approve = true, reason = null)) }
            .thenRefresh(queries)

    override suspend fun reject(requestId: String, affiliateUserId: String, reason: String): Result<AdoptionRequest> =
        remote { rpc("respond_adoption_request", respondParams(requestId, approve = false, reason = reason)) }
            .thenRefresh(queries)

    override fun observeByRequester(userId: String): Flow<List<AdoptionRequest>> =
        queries.observe("adoption-requests:requester:$userId", emptyList()) {
            select(column = "requester_id", value = userId, order = Order.DESCENDING)
        }

    override fun observeByOrganization(organizationId: String): Flow<List<AdoptionRequest>> =
        queries.observe("adoption-requests:org:$organizationId", emptyList()) {
            select(column = "organization_id", value = organizationId, order = Order.ASCENDING)
        }

    private suspend fun rpc(name: String, params: JsonObject): AdoptionRequest =
        client.postgrest.rpc(name, params).decodeAs<AdoptionRequestDto>().toDomain()

    /** Same order as the fake: newest first for the requester, oldest first for the center. */
    private suspend fun select(column: String, value: String, order: Order): List<AdoptionRequest> =
        client.from("adoption_requests").select {
            filter { eq(column, value) }
            order("created_at", order)
        }.decodeList<AdoptionRequestDto>().map { it.toDomain() }

    private fun respondParams(requestId: String, approve: Boolean, reason: String?) = buildJsonObject {
        put("p_request_id", requestId)
        put("p_approve", approve)
        put("p_reason", reason)
    }

    private companion object {
        /** Same text as the RPC and the fake; checked here because there is no session to send. */
        const val ACCOUNT_REQUIRED = "Necesitas una cuenta para enviar una solicitud de adopción"
    }
}
