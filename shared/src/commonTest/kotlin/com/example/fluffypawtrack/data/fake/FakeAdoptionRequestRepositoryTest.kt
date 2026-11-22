package com.example.fluffypawtrack.data.fake

import com.example.fluffypawtrack.domain.model.AdoptionRequestStatus
import com.example.fluffypawtrack.domain.model.HousingTenure
import com.example.fluffypawtrack.domain.model.HousingType
import com.example.fluffypawtrack.domain.model.ListingStatus
import com.example.fluffypawtrack.domain.model.NewAdoptionRequest
import com.example.fluffypawtrack.domain.model.NotificationType
import com.example.fluffypawtrack.domain.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FakeAdoptionRequestRepositoryTest {

    private val demo = SampleData.demoUser
    private val affiliate = SampleData.demoAffiliate
    private val otherAffiliate = affiliate.copy(id = "user-other-org", organizationId = "org-imbpa")
    private val anonymous = demo.copy(id = "anonymous-1", email = null, isAnonymous = true)
    private val users = SampleData.users + otherAffiliate + anonymous
    private val findUser: (String) -> UserProfile? = { id -> users.firstOrNull { it.id == id } }

    private val listings = FakeAdoptionRepository(findUser = findUser)
    private val notifications = FakeNotificationRepository()
    private val repository = FakeAdoptionRequestRepository(
        adoptionRepository = listings,
        notificationRepository = notifications,
        findUser = findUser,
        initialRequests = emptyList(),
    )

    private fun request(listingId: String = "listing-1", reason: String = "Tengo patio y tiempo.") = NewAdoptionRequest(
        listingId = listingId,
        fullName = "María Valenzuela",
        phone = "6620000001",
        email = "demo@correo.com",
        housingType = HousingType.House,
        housingTenure = HousingTenure.Owned,
        hasOtherPets = false,
        otherPets = null,
        reason = reason,
    )

    private fun <T> Result<T>.message(): String? = exceptionOrNull()?.message

    @Test
    fun userSubmitsAPendingRequestLinkedToTheListingCenter() = runTest {
        val created = repository.submit(demo.id, request()).getOrThrow()

        assertEquals(AdoptionRequestStatus.Pending, created.status)
        assertEquals("org-patitas", created.organizationId)
        assertEquals(demo.id, created.requesterId)
        assertEquals(listOf(created), repository.observeByRequester(demo.id).first())
        assertEquals(listOf(created), repository.observeByOrganization("org-patitas").first())
    }

    @Test
    fun submittingRequiresAnAccount() = runTest {
        assertEquals(FakeAdoptionRequestRepository.ACCOUNT_REQUIRED, repository.submit(null, request()).message())
        assertEquals(FakeAdoptionRequestRepository.ACCOUNT_REQUIRED, repository.submit(anonymous.id, request()).message())
    }

    @Test
    fun onlyTheUserRoleCanSubmit() = runTest {
        assertEquals(FakeAdoptionRequestRepository.ONLY_USERS, repository.submit(affiliate.id, request()).message())
    }

    @Test
    fun atMostOneActiveRequestPerUserPerAnimal() = runTest {
        val first = repository.submit(demo.id, request()).getOrThrow()
        assertEquals(FakeAdoptionRequestRepository.ALREADY_ACTIVE, repository.submit(demo.id, request()).message())

        repository.approve(first.id, affiliate.id).getOrThrow()
        assertEquals(FakeAdoptionRequestRepository.ALREADY_ACTIVE, repository.submit(demo.id, request()).message())

        // Another animal is fine.
        assertTrue(repository.submit(demo.id, request("listing-2")).isSuccess)
    }

    @Test
    fun aCancelledRequestAllowsANewOne() = runTest {
        val first = repository.submit(demo.id, request()).getOrThrow()
        repository.cancel(first.id, demo.id).getOrThrow()
        assertTrue(repository.submit(demo.id, request()).isSuccess)
    }

    @Test
    fun onlyAvailableListingsAcceptRequests() = runTest {
        // listing-4 (Luna) is already adopted.
        assertEquals(FakeAdoptionRequestRepository.NOT_AVAILABLE, repository.submit(demo.id, request("listing-4")).message())
        assertEquals(FakeAdoptionRequestRepository.NOT_AVAILABLE, repository.submit(demo.id, request("missing")).message())
    }

    @Test
    fun theReasonIsRequired() = runTest {
        assertEquals(
            FakeAdoptionRequestRepository.ADOPTION_REASON_REQUIRED,
            repository.submit(demo.id, request(reason = "  ")).message(),
        )
    }

    @Test
    fun otherPetsMustBeNamedWhenTheUserHasThem() = runTest {
        val result = repository.submit(demo.id, request().copy(hasOtherPets = true, otherPets = ""))
        assertEquals(FakeAdoptionRequestRepository.OTHER_PETS_REQUIRED, result.message())
    }

    @Test
    fun onlyTheRequesterCancelsAndOnlyWhilePending() = runTest {
        val created = repository.submit(demo.id, request()).getOrThrow()

        assertEquals("Solo quien envió la solicitud puede cancelarla", repository.cancel(created.id, "user-2").message())
        assertEquals(AdoptionRequestStatus.Cancelled, repository.cancel(created.id, demo.id).getOrThrow().status)
        assertEquals("Solo puedes cancelar solicitudes pendientes", repository.cancel(created.id, demo.id).message())
    }

    @Test
    fun onlyAnAffiliateOfTheCenterAnswers() = runTest {
        val created = repository.submit(demo.id, request()).getOrThrow()
        val expected = "Solo el centro de este animal puede responder la solicitud"

        assertEquals(expected, repository.approve(created.id, otherAffiliate.id).message())
        assertEquals(expected, repository.approve(created.id, demo.id).message())
        assertEquals(expected, repository.reject(created.id, otherAffiliate.id, "No").message())
        assertEquals(AdoptionRequestStatus.Pending, repository.observeByRequester(demo.id).first().single().status)
    }

    @Test
    fun rejectingRequiresAReason() = runTest {
        val created = repository.submit(demo.id, request()).getOrThrow()
        assertEquals(FakeAdoptionRequestRepository.REASON_REQUIRED, repository.reject(created.id, affiliate.id, " ").message())
    }

    @Test
    fun finalStatesNeverChange() = runTest {
        val approved = repository.submit(demo.id, request()).getOrThrow()
        repository.approve(approved.id, affiliate.id).getOrThrow()
        val cancelled = repository.submit(demo.id, request("listing-2")).getOrThrow()
        repository.cancel(cancelled.id, demo.id).getOrThrow()
        val rejected = repository.submit(demo.id, request("listing-3")).getOrThrow()
        repository.reject(rejected.id, affiliate.id, "Necesita patio").getOrThrow()

        for (id in listOf(approved.id, cancelled.id, rejected.id)) {
            assertEquals("Esta solicitud ya fue respondida", repository.approve(id, affiliate.id).message())
            assertEquals("Esta solicitud ya fue respondida", repository.reject(id, affiliate.id, "Otro").message())
            assertEquals("Solo puedes cancelar solicitudes pendientes", repository.cancel(id, demo.id).message())
        }
        val statuses = repository.observeByRequester(demo.id).first().associate { it.id to it.status }
        assertEquals(AdoptionRequestStatus.Approved, statuses[approved.id])
        assertEquals(AdoptionRequestStatus.Cancelled, statuses[cancelled.id])
        assertEquals(AdoptionRequestStatus.Rejected, statuses[rejected.id])
    }

    @Test
    fun approvingNotifiesTheRequesterWithTheExactText() = runTest {
        val created = repository.submit(demo.id, request()).getOrThrow()
        val before = notifications.observeNotifications(demo.id).first().size

        val approved = repository.approve(created.id, affiliate.id).getOrThrow()

        assertEquals(AdoptionRequestStatus.Approved, approved.status)
        val notification = notifications.observeNotifications(demo.id).first().last()
        assertEquals(before + 1, notifications.observeNotifications(demo.id).first().size)
        assertEquals(
            "¡Buenas noticias! El centro aprobó tu solicitud para adoptar a Canela. Pronto se pondrán en contacto contigo.",
            notification.message,
        )
        assertEquals(NotificationType.Adoption, notification.type)
    }

    @Test
    fun rejectingNotifiesTheRequesterWithTheTextAndTheReason() = runTest {
        val created = repository.submit(demo.id, request()).getOrThrow()

        val rejected = repository.reject(created.id, affiliate.id, "Buscamos un hogar con patio").getOrThrow()

        assertEquals(AdoptionRequestStatus.Rejected, rejected.status)
        assertEquals("Buscamos un hogar con patio", rejected.rejectionReason)
        assertEquals(
            "Tu solicitud para adoptar a Canela no fue aprobada esta vez. ¡Hay muchos más esperando un hogar!\n" +
                "Motivo: Buscamos un hogar con patio",
            notifications.observeNotifications(demo.id).first().last().message,
        )
    }

    @Test
    fun markingAdoptedRejectsPendingRequestsWithTheSameNotice() = runTest {
        val pending = repository.submit(demo.id, request()).getOrThrow()
        val other = repository.submit("user-2", request()).getOrThrow()
        repository.approve(other.id, affiliate.id).getOrThrow()

        val listing = listings.markAdopted("listing-1", affiliate.id).getOrThrow()

        assertEquals(ListingStatus.Adopted, listing.status)
        val updated = repository.observeByRequester(demo.id).first().single { it.id == pending.id }
        assertEquals(AdoptionRequestStatus.Rejected, updated.status)
        assertEquals("El animal ya fue adoptado", updated.rejectionReason)
        assertEquals(
            "Tu solicitud para adoptar a Canela no fue aprobada esta vez. ¡Hay muchos más esperando un hogar!\n" +
                "Motivo: El animal ya fue adoptado",
            notifications.observeNotifications(demo.id).first().last().message,
        )
        // The approved request keeps its state.
        assertEquals(AdoptionRequestStatus.Approved, repository.observeByRequester("user-2").first().single().status)
    }

    @Test
    fun onlyAnAffiliateOfTheCenterMarksAnAnimalAdoptedAndOnlyOnce() = runTest {
        val expected = "Solo el centro de este animal puede marcarlo como adoptado"
        assertEquals(expected, listings.markAdopted("listing-1", otherAffiliate.id).message())
        assertEquals(expected, listings.markAdopted("listing-1", demo.id).message())
        assertEquals("Este animal ya fue marcado como adoptado", listings.markAdopted("listing-4", affiliate.id).message())
        assertNull(listings.markAdopted("listing-1", affiliate.id).exceptionOrNull())
    }

    @Test
    fun sampleDataGivesTheDemoUserPendingRequestsAtPatitas() = runTest {
        val sample = FakeAdoptionRequestRepository()
        val mine = sample.observeByRequester(demo.id).first()
        assertTrue(mine.isNotEmpty())
        assertTrue(mine.all { it.status == AdoptionRequestStatus.Pending && it.organizationId == "org-patitas" })
        assertEquals(mine.size, sample.observeByOrganization(affiliate.organizationId!!).first().size)
    }
}
