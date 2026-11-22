package com.example.fluffypawtrack.data.fake

import com.example.fluffypawtrack.domain.model.AdoptionListing
import com.example.fluffypawtrack.domain.model.AdoptionRequest
import com.example.fluffypawtrack.domain.model.AdoptionRequestStatus
import com.example.fluffypawtrack.domain.model.AdoptionRequestTexts
import com.example.fluffypawtrack.domain.model.ListingStatus
import com.example.fluffypawtrack.domain.model.NewAdoptionRequest
import com.example.fluffypawtrack.domain.model.NotificationType
import com.example.fluffypawtrack.domain.model.Role
import com.example.fluffypawtrack.domain.model.UserProfile
import com.example.fluffypawtrack.domain.model.isActive
import com.example.fluffypawtrack.domain.repository.AdoptionRepository
import com.example.fluffypawtrack.domain.repository.AdoptionRequestRepository
import com.example.fluffypawtrack.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Enforces the RF-06 rules that Supabase will enforce with RLS, a partial unique index and an
 * RPC (see [AdoptionRequestRepository]). [findUser] stands in for the `profiles` table.
 * When [adoptionRepository] is the fake, marking an animal as adopted rejects its pending requests.
 */
@OptIn(ExperimentalTime::class)
class FakeAdoptionRequestRepository(
    private val adoptionRepository: AdoptionRepository = FakeAdoptionRepository(),
    private val notificationRepository: NotificationRepository = FakeNotificationRepository(),
    private val findUser: (String) -> UserProfile? = { id -> SampleData.users.firstOrNull { it.id == id } },
    private val clock: Clock = Clock.System,
    initialRequests: List<AdoptionRequest> = SampleData.adoptionRequests,
) : AdoptionRequestRepository {
    private val requests = MutableStateFlow(initialRequests)
    private var nextId = 1

    init {
        (adoptionRepository as? FakeAdoptionRepository)?.addAdoptedListener(::rejectPendingOfAdopted)
    }

    override fun observeByRequester(userId: String): Flow<List<AdoptionRequest>> =
        requests.map { list -> list.filter { it.requesterId == userId }.sortedByDescending { it.createdAt } }

    override fun observeByOrganization(organizationId: String): Flow<List<AdoptionRequest>> =
        requests.map { list -> list.filter { it.organizationId == organizationId }.sortedBy { it.createdAt } }

    override suspend fun submit(requesterId: String?, request: NewAdoptionRequest): Result<AdoptionRequest> {
        val requester = requesterId?.let(findUser)?.takeUnless { it.isAnonymous }
            ?: return failure(ACCOUNT_REQUIRED)
        if (requester.role != Role.User) return failure(ONLY_USERS)
        val listing = adoptionRepository.getListing(request.listingId)
        if (listing == null || listing.status != ListingStatus.Available) return failure(NOT_AVAILABLE)
        val hasActive = requests.value.any {
            it.listingId == listing.id && it.requesterId == requester.id && it.status.isActive
        }
        if (hasActive) return failure(ALREADY_ACTIVE)
        validate(request)?.let { return failure(it) }
        val created = AdoptionRequest(
            id = "adoption-request-new-${nextId++}",
            listingId = listing.id,
            organizationId = listing.organizationId,
            requesterId = requester.id,
            fullName = request.fullName.trim(),
            phone = request.phone.trim(),
            email = request.email.trim(),
            housingType = request.housingType,
            housingTenure = request.housingTenure,
            hasOtherPets = request.hasOtherPets,
            otherPets = request.otherPets?.trim()?.takeIf { request.hasOtherPets && it.isNotEmpty() },
            reason = request.reason.trim(),
            status = AdoptionRequestStatus.Pending,
            rejectionReason = null,
            createdAt = clock.now(),
            respondedAt = null,
        )
        requests.update { it + created }
        return Result.success(created)
    }

    override suspend fun cancel(requestId: String, userId: String): Result<AdoptionRequest> {
        val request = find(requestId) ?: return failure(NOT_FOUND)
        if (request.requesterId != userId) return failure("Solo quien envió la solicitud puede cancelarla")
        if (request.status != AdoptionRequestStatus.Pending) return failure("Solo puedes cancelar solicitudes pendientes")
        return Result.success(save(request.copy(status = AdoptionRequestStatus.Cancelled, respondedAt = clock.now())))
    }

    override suspend fun approve(requestId: String, affiliateUserId: String): Result<AdoptionRequest> {
        val request = answerable(requestId, affiliateUserId).getOrElse { return Result.failure(it) }
        val approved = save(request.copy(status = AdoptionRequestStatus.Approved, respondedAt = clock.now()))
        notificationRepository.notify(
            userId = approved.requesterId,
            title = AdoptionRequestTexts.APPROVED_TITLE,
            message = AdoptionRequestTexts.approvedMessage(animalName(approved)),
            type = NotificationType.Adoption,
        )
        return Result.success(approved)
    }

    override suspend fun reject(requestId: String, affiliateUserId: String, reason: String): Result<AdoptionRequest> {
        val request = answerable(requestId, affiliateUserId).getOrElse { return Result.failure(it) }
        if (reason.isBlank()) return failure(REASON_REQUIRED)
        return Result.success(rejectAndNotify(request, reason.trim(), animalName(request)))
    }

    /** Pending requests of an animal that was just adopted become rejected with the same notice. */
    private suspend fun rejectPendingOfAdopted(listing: AdoptionListing) {
        requests.value
            .filter { it.listingId == listing.id && it.status == AdoptionRequestStatus.Pending }
            .forEach { rejectAndNotify(it, AdoptionRequestTexts.ADOPTED_REASON, listing.name) }
    }

    private suspend fun rejectAndNotify(request: AdoptionRequest, reason: String, animalName: String): AdoptionRequest {
        val rejected = save(
            request.copy(status = AdoptionRequestStatus.Rejected, rejectionReason = reason, respondedAt = clock.now()),
        )
        notificationRepository.notify(
            userId = rejected.requesterId,
            title = AdoptionRequestTexts.REJECTED_TITLE,
            message = AdoptionRequestTexts.rejectedMessage(animalName, reason),
            type = NotificationType.Adoption,
        )
        return rejected
    }

    /** The request exists, is still pending and [affiliateUserId] belongs to its center. */
    private fun answerable(requestId: String, affiliateUserId: String): Result<AdoptionRequest> {
        val request = find(requestId) ?: return failure(NOT_FOUND)
        val affiliate = findUser(affiliateUserId)
        if (affiliate?.role != Role.Affiliate || affiliate.organizationId != request.organizationId) {
            return failure("Solo el centro de este animal puede responder la solicitud")
        }
        if (request.status != AdoptionRequestStatus.Pending) return failure("Esta solicitud ya fue respondida")
        return Result.success(request)
    }

    private suspend fun animalName(request: AdoptionRequest): String =
        adoptionRepository.getListing(request.listingId)?.name ?: "este animal"

    private fun find(requestId: String): AdoptionRequest? = requests.value.firstOrNull { it.id == requestId }

    private fun save(updated: AdoptionRequest): AdoptionRequest {
        requests.update { list -> list.map { if (it.id == updated.id) updated else it } }
        return updated
    }

    private fun <T> failure(message: String): Result<T> = Result.failure(IllegalStateException(message))

    companion object {
        const val ACCOUNT_REQUIRED = "Necesitas una cuenta para enviar una solicitud de adopción"
        const val ONLY_USERS = "Solo los usuarios pueden enviar solicitudes de adopción"
        const val NOT_AVAILABLE = "Este animal ya no está disponible para adopción"
        const val ALREADY_ACTIVE = "Ya tienes una solicitud activa para este animal"
        const val CONTACT_REQUIRED = "Completa tu nombre, teléfono y correo"
        const val OTHER_PETS_REQUIRED = "Indica cuáles otras mascotas tienes"
        const val ADOPTION_REASON_REQUIRED = "Cuéntanos por qué quieres adoptarlo"
        const val REASON_REQUIRED = "Escribe el motivo del rechazo"
        private const val NOT_FOUND = "La solicitud no existe"

        /** First broken form rule, or null when the request is complete. */
        fun validate(request: NewAdoptionRequest): String? = when {
            listOf(request.fullName, request.phone, request.email).any { it.isBlank() } -> CONTACT_REQUIRED
            request.hasOtherPets && request.otherPets.isNullOrBlank() -> OTHER_PETS_REQUIRED
            request.reason.isBlank() -> ADOPTION_REASON_REQUIRED
            else -> null
        }
    }
}
