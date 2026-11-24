package com.example.fluffypawtrack.data.supabase

import com.example.fluffypawtrack.domain.model.AdoptionRequestStatus
import com.example.fluffypawtrack.domain.model.ContactDetails
import com.example.fluffypawtrack.domain.model.ContactRequestStatus
import com.example.fluffypawtrack.domain.model.GeoPoint
import com.example.fluffypawtrack.domain.model.HousingTenure
import com.example.fluffypawtrack.domain.model.HousingType
import com.example.fluffypawtrack.domain.model.ListingStatus
import com.example.fluffypawtrack.domain.model.NewAdoptionRequest
import com.example.fluffypawtrack.domain.model.NewReport
import com.example.fluffypawtrack.domain.model.NotificationType
import com.example.fluffypawtrack.domain.model.OrganizationStatus
import com.example.fluffypawtrack.domain.model.OrganizationType
import com.example.fluffypawtrack.domain.model.Priority
import com.example.fluffypawtrack.domain.model.ReportCategory
import com.example.fluffypawtrack.domain.model.ReportStatus
import com.example.fluffypawtrack.domain.model.Role
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class MappersTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun reportDto(anonymous: Boolean, address: String? = "Blvd. Kino 123") = ReportDto(
        id = "r1",
        reporterId = "u1",
        reporterIsAnonymous = anonymous,
        category = "possibly_lost",
        priority = "medium",
        description = "Perrito café",
        photoUrl = "res:sample_report_labrador",
        lat = 29.07,
        lng = -110.95,
        address = address,
        reference = null,
        status = "on_the_way",
        assignedOrgId = "o1",
        createdAt = "2026-11-22T10:15:30.123456+00:00",
    )

    @Test
    fun reportMapsColumnsAndTimestamps() {
        val report = reportDto(anonymous = false).toDomain()

        assertEquals("u1", report.reporterId)
        assertEquals(ReportCategory.PossiblyLost, report.category)
        assertEquals(Priority.Medium, report.priority)
        assertEquals(ReportStatus.OnTheWay, report.status)
        assertEquals(GeoPoint(29.07, -110.95), report.location)
        assertEquals("Blvd. Kino 123", report.address)
        assertEquals("o1", report.assignedOrganizationId)
        assertEquals(Instant.parse("2026-11-22T10:15:30.123456Z"), report.createdAt)
    }

    @Test
    fun anonymousReporterMapsToNull() {
        assertNull(reportDto(anonymous = true).toDomain().reporterId)
    }

    @Test
    fun missingAddressFallsBackToCoordinates() {
        assertEquals("29.07, -110.95", reportDto(anonymous = false, address = null).toDomain().address)
    }

    @Test
    fun reportRowDecodesFromSnakeCaseJson() {
        val row = """
            {"id":"r1","reporter_id":null,"reporter_is_anonymous":false,"category":"critical","priority":"urgent",
             "description":"","photo_url":"res:x","lat":1.0,"lng":2.0,"address":null,"reference":"Junto al Oxxo",
             "status":"pending","assigned_org_id":null,"hidden":false,"created_at":"2026-11-22T10:15:30+00:00",
             "updated_at":"2026-11-22T10:15:30+00:00"}
        """.trimIndent()

        val report = json.decodeFromString<ReportDto>(row).toDomain()

        assertNull(report.reporterId)
        assertEquals(ReportCategory.Critical, report.category)
        assertEquals("Junto al Oxxo", report.reference)
    }

    @Test
    fun newReportInsertUsesSnakeCaseColumns() {
        val dto = NewReport(
            category = ReportCategory.Injured,
            priority = Priority.High,
            description = "Cojea",
            photoUrl = "https://x/p.jpg",
            location = GeoPoint(1.5, -2.5),
            address = "",
            reference = null,
        ).toInsertDto(reporterId = "u1", photoUrl = "https://x/uploaded.jpg")

        val row = json.encodeToJsonElement(NewReportDto.serializer(), dto).jsonObject

        assertEquals("u1", row["reporter_id"]?.jsonPrimitive?.content)
        assertEquals("injured", row["category"]?.jsonPrimitive?.content)
        assertEquals("high", row["priority"]?.jsonPrimitive?.content)
        assertEquals("https://x/uploaded.jpg", row["photo_url"]?.jsonPrimitive?.content)
        assertEquals(JsonNull, row["address"])
        assertEquals(1.5, row["lat"]?.jsonPrimitive?.content?.toDouble())
    }

    @Test
    fun profileMapsWithSessionEmailAndAnonymousFlag() {
        val profile = ProfileDto(
            id = "u1",
            firstName = "Ana",
            lastName = "López",
            phone = "6621234567",
            role = "affiliate",
            organizationId = "o1",
        )

        val user = profile.toDomain(email = "ana@correo.com", isAnonymous = false)

        assertEquals("ana@correo.com", user.email)
        assertEquals(Role.Affiliate, user.role)
        assertEquals("o1", user.organizationId)
        assertEquals(false, user.isAnonymous)
    }

    @Test
    fun profileUpdateWritesOnlyTheEditableColumns() {
        val row = profileUpdateOf(firstName = "Ana", lastName = "López", phone = null, avatarUrl = "https://x/a.jpg")

        assertEquals(setOf("first_name", "last_name", "phone", "avatar_url"), row.keys)
        assertEquals("Ana", row["first_name"]?.jsonPrimitive?.content)
        assertEquals(JsonNull, row["phone"])
        assertEquals("https://x/a.jpg", row["avatar_url"]?.jsonPrimitive?.content)
    }

    @Test
    fun profileMapsTheAvatarUrl() {
        val user = ProfileDto(id = "u1", avatarUrl = "https://x/a.jpg").toDomain(email = null, isAnonymous = false)

        assertEquals("https://x/a.jpg", user.avatarUrl)
    }

    @Test
    fun sessionWithoutProfileIsAPlainUser() {
        val user = userProfileOf(id = "anon", email = null, isAnonymous = true, profile = null)

        assertEquals(Role.User, user.role)
        assertEquals("", user.firstName)
        assertEquals(true, user.isAnonymous)
    }

    @Test
    fun organizationMaps() {
        val org = OrganizationDto(
            id = "o1",
            type = "adoption_center",
            name = "Patitas",
            description = "d",
            photoUrl = "res:p",
            lat = 1.0,
            lng = 2.0,
            address = "a",
            schedule = "s",
            phone = "p",
            email = "e",
            social = null,
            status = "approved",
            active = true,
        ).toDomain()

        assertEquals(OrganizationType.AdoptionCenter, org.type)
        assertEquals(OrganizationStatus.Approved, org.status)
        assertEquals(GeoPoint(1.0, 2.0), org.location)
    }

    @Test
    fun listingMapsPhotoArray() {
        val listing = json.decodeFromString<ListingDto>(
            """{"id":"l1","organization_id":"o1","name":"Toby","species":"Perro","breed":"Mestizo","age":"2 años",
                "vaccination":"Completa","description":"d","photo_urls":["res:a","res:b"],"status":"adopted",
                "created_at":"2026-11-22T10:15:30+00:00"}""",
        ).toDomain()

        assertEquals(listOf("res:a", "res:b"), listing.photoUrls)
        assertEquals(ListingStatus.Adopted, listing.status)
    }

    @Test
    fun adoptionRequestMapsEveryField() {
        val request = AdoptionRequestDto(
            id = "a1",
            listingId = "l1",
            organizationId = "o1",
            requesterId = "u1",
            fullName = "Ana López",
            phone = "662",
            email = "ana@correo.com",
            housingType = "apartment",
            housingTenure = "rented",
            hasOtherPets = true,
            otherPets = "Un gato",
            reason = "Me encantan",
            status = "rejected",
            rejectionReason = "El animal ya fue adoptado",
            createdAt = "2026-11-22T10:15:30+00:00",
            respondedAt = "2026-11-23T08:00:00+00:00",
        ).toDomain()

        assertEquals(HousingType.Apartment, request.housingType)
        assertEquals(HousingTenure.Rented, request.housingTenure)
        assertEquals("Un gato", request.otherPets)
        assertEquals(AdoptionRequestStatus.Rejected, request.status)
        assertEquals("El animal ya fue adoptado", request.rejectionReason)
        assertEquals(Instant.parse("2026-11-23T08:00:00Z"), request.respondedAt)
    }

    @Test
    fun adoptionRequestWithoutResponseHasNullRespondedAt() {
        val request = json.decodeFromString<AdoptionRequestDto>(
            """{"id":"a1","listing_id":"l1","organization_id":"o1","requester_id":"u1","full_name":"A","phone":"1",
                "email":"e","housing_type":"house","housing_tenure":"owned","has_other_pets":false,"other_pets":null,
                "reason":"r","status":"pending","rejection_reason":null,"created_at":"2026-11-22T10:15:30+00:00",
                "responded_at":null}""",
        ).toDomain()

        assertNull(request.respondedAt)
        assertEquals(HousingType.House, request.housingType)
    }

    @Test
    fun submitAdoptionParamsUseTheRpcNames() {
        val params = NewAdoptionRequest(
            listingId = "l1",
            fullName = "Ana",
            phone = "662",
            email = "ana@correo.com",
            housingType = HousingType.House,
            housingTenure = HousingTenure.Owned,
            hasOtherPets = false,
            otherPets = null,
            reason = "Quiero darle un hogar",
        ).toSubmitParams()

        assertEquals(
            setOf(
                "p_listing_id", "p_full_name", "p_phone", "p_email", "p_housing_type", "p_housing_tenure",
                "p_has_other_pets", "p_other_pets", "p_reason",
            ),
            params.keys,
        )
        assertEquals(JsonPrimitive("house"), params["p_housing_type"])
        assertEquals(JsonPrimitive("owned"), params["p_housing_tenure"])
        assertEquals(false, params["p_has_other_pets"]?.jsonPrimitive?.boolean)
        assertEquals(JsonNull, params["p_other_pets"])
    }

    @Test
    fun contactRequestMapsRecipientOrganization() {
        val request = ContactRequestDto(
            id = "c1",
            reportId = "r1",
            requesterId = "u2",
            requesterFirstName = "Luis",
            recipientUserId = null,
            recipientOrgId = "o1",
            message = "Es mi perro, tiene un collar azul",
            photoUrl = null,
            status = "accepted",
            statusNote = null,
            createdAt = "2026-11-22T10:15:30+00:00",
        ).toDomain()

        assertEquals("o1", request.recipientOrganizationId)
        assertNull(request.recipientUserId)
        assertEquals(ContactRequestStatus.Accepted, request.status)
        assertEquals("Luis", request.requesterFirstName)
    }

    @Test
    fun contactRequestWithDeletedReportKeepsAnEmptyReportId() {
        val request = json.decodeFromString<ContactRequestDto>(
            """{"id":"c1","report_id":null,"requester_id":"u2","requester_first_name":"Luis","recipient_user_id":"u1",
                "recipient_org_id":null,"message":"m","photo_url":null,"status":"rejected",
                "status_note":"El reporte ya no está disponible","created_at":"2026-11-22T10:15:30+00:00",
                "responded_at":null}""",
        ).toDomain()

        assertEquals("", request.reportId)
        assertEquals("El reporte ya no está disponible", request.statusNote)
    }

    @Test
    fun notificationMapsBodyReadAndCreatedAt() {
        val notification = NotificationDto(
            id = "n1",
            type = "adoption",
            title = "Solicitud de adopción aprobada",
            body = "¡Buenas noticias!",
            read = true,
            createdAt = "2026-11-22T10:15:30+00:00",
        ).toDomain()

        assertEquals("¡Buenas noticias!", notification.message)
        assertEquals(true, notification.isRead)
        assertEquals(NotificationType.Adoption, notification.type)
        assertEquals(Instant.parse("2026-11-22T10:15:30Z"), notification.receivedAt)
    }

    @Test
    fun contactDetailsMap() {
        assertEquals(
            ContactDetails("Patitas", "662", "p@correo.com"),
            ContactDetailsDto(fullName = "Patitas", phone = "662", email = "p@correo.com").toDomain(),
        )
    }
}
