package com.example.fluffypawtrack.data.supabase

import com.example.fluffypawtrack.domain.model.GeoPoint
import com.example.fluffypawtrack.domain.model.OrganizationDraft
import com.example.fluffypawtrack.domain.model.OrganizationProfile
import com.example.fluffypawtrack.domain.model.OrganizationType
import com.example.fluffypawtrack.domain.model.ReportStatus
import com.example.fluffypawtrack.domain.model.ReportStatusRules
import kotlinx.serialization.json.Json
import com.example.fluffypawtrack.domain.model.NewListing
import com.example.fluffypawtrack.domain.model.NotificationPrefs
import com.example.fluffypawtrack.domain.model.Priority
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** DTO mappings of the affiliate and admin repositories (Phase 3). */
@OptIn(ExperimentalTime::class)
class RoleMappersTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun reportRow(hidden: Boolean, anonymous: Boolean = false) = json.decodeFromString<ReportDto>(
        """{"id":"r1","reporter_id":"u1","reporter_is_anonymous":$anonymous,"category":"street","priority":"low",
            "description":"","photo_url":"res:x","lat":1.0,"lng":2.0,"status":"sheltered",
            "assigned_org_id":"o1","hidden":$hidden,"created_at":"2026-11-22T10:00:00+00:00"}""",
    )

    @Test
    fun reportMapsHiddenFlag() {
        assertTrue(reportRow(hidden = true).toDomain().hidden)
        assertEquals(false, reportRow(hidden = false).toDomain().hidden)
    }

    @Test
    fun historyMapsStatusDateAndResponsibleOldestFirst() {
        val rows = json.decodeFromString<List<StatusHistoryDto>>(
            """[
                {"id":"h3","report_id":"r1","status":"sheltered","changed_by":"u1","created_at":"2026-11-22T12:00:00+00:00"},
                {"id":"h1","report_id":"r1","status":"pending","changed_by":"u1","created_at":"2026-11-22T10:00:00+00:00"},
                {"id":"h2","report_id":"r1","status":"on_the_way","changed_by":"staff","created_at":"2026-11-22T11:00:00+00:00"}
            ]""",
        )

        val history = statusHistoryOf(rows, reportRow(hidden = false), organizationName = "Patitas")

        assertEquals(listOf(ReportStatus.Pending, ReportStatus.OnTheWay, ReportStatus.Sheltered), history.map { it.status })
        assertEquals(Instant.parse("2026-11-22T11:00:00Z"), history[1].changedAt)
        assertEquals(
            listOf(ReportStatusRules.REPORTER_RESPONSIBLE, "Patitas", ReportStatusRules.REPORTER_RESPONSIBLE),
            history.map { it.responsibleName },
        )
    }

    private val draft = OrganizationDraft(
        type = OrganizationType.AdoptionCenter,
        name = " Centro Pitic ",
        description = "Centro de adopción.",
        location = GeoPoint(29.1, -110.9),
        address = "Blvd. Kino 1",
        schedule = "Lunes a viernes",
        capacity = 12,
        phone = "662",
        email = "c@p.example",
        social = " ",
        photoUrl = "content://local",
    )

    @Test
    fun organizationMapsCapacity() {
        val org = json.decodeFromString<OrganizationDto>(
            """{"id":"o1","type":"shelter","name":"N","photo_url":"res:p","lat":1.0,"lng":2.0,
                "status":"approved","active":true,"capacity":40}""",
        ).toDomain()
        assertEquals(40, org.capacity)
        assertNull(org.rejectionReason)
    }

    @Test
    fun organizationMapsRejectionReason() {
        val org = json.decodeFromString<OrganizationDto>(
            """{"id":"o1","type":"shelter","name":"N","photo_url":"res:p","lat":1.0,"lng":2.0,
                "status":"rejected","active":true,"rejection_reason":"Faltan datos"}""",
        ).toDomain()
        assertEquals("Faltan datos", org.rejectionReason)
    }

    @Test
    fun adminDraftUsesSnakeCaseColumnsAndIsApproved() {
        val row = json.encodeToJsonElement(NewOrganizationDto.serializer(), draft.toWriteDto("https://x/p.jpg").asNewApproved())
            .jsonObject

        assertEquals("adoption_center", row["type"]?.jsonPrimitive?.content)
        assertEquals("Centro Pitic", row["name"]?.jsonPrimitive?.content)
        assertEquals("https://x/p.jpg", row["photo_url"]?.jsonPrimitive?.content)
        assertEquals("12", row["capacity"]?.jsonPrimitive?.content)
        assertEquals(JsonNull, row["social"])
        assertEquals("approved", row["status"]?.jsonPrimitive?.content)
        assertEquals("true", row["active"]?.jsonPrimitive?.content)
    }

    @Test
    fun editDraftLeavesStatusAndVisibilityOut() {
        val row = json.encodeToJsonElement(OrganizationWriteDto.serializer(), draft.toWriteDto("p")).jsonObject
        assertTrue("status" !in row && "active" !in row)
    }

    @Test
    fun affiliateProfileSendsOnlyGuardedColumns() {
        val profile = OrganizationProfile("d", "s", "p", "e", "@x", "content://local")
        val row = json.encodeToJsonElement(OrganizationProfileDto.serializer(), profile.toDto("https://x/p.jpg")).jsonObject
        assertEquals(setOf("description", "schedule", "phone", "email", "social", "photo_url"), row.keys)
        assertEquals("https://x/p.jpg", row["photo_url"]?.jsonPrimitive?.content)
    }

    private val listing = NewListing(" Luna ", "Gato", "Mestizo", "1 año", "Completa", "Tranquila", listOf("content://a"))

    @Test
    fun newListingInsertUsesSnakeCaseColumnsAndStoredPhotos() {
        val row = json.encodeToJsonElement(NewListingDto.serializer(), listing.toInsertDto("o1", listOf("https://x/a.jpg")))
            .jsonObject

        assertEquals("o1", row["organization_id"]?.jsonPrimitive?.content)
        assertEquals("Luna", row["name"]?.jsonPrimitive?.content)
        assertEquals(listOf("https://x/a.jpg"), row["photo_urls"]?.jsonArray?.map { it.jsonPrimitive.content })
        assertTrue("status" !in row)
    }

    @Test
    fun listingUpdateLeavesOrganizationAndStatusOut() {
        val row = json.encodeToJsonElement(ListingUpdateDto.serializer(), listing.toUpdateDto(listOf("https://x/a.jpg")))
            .jsonObject
        assertTrue("organization_id" !in row && "status" !in row)
        assertEquals("https://x/a.jpg", row["photo_urls"]?.jsonArray?.single()?.jsonPrimitive?.content)
    }

    @Test
    fun notificationPrefsRowMapsToDomain() {
        val prefs = json.decodeFromString<NotificationPrefsDto>(
            """{"user_id":"u1","priorities":["high","urgent"],"radius_km":7.5,"push_enabled":false,
                "updated_at":"2026-11-22T10:00:00+00:00"}""",
        ).toDomain()

        assertEquals(NotificationPrefs(7.5, setOf(Priority.High, Priority.Urgent), false), prefs)
    }

    @Test
    fun notificationPrefsUpsertUsesColumnNamesAndEnumOrder() {
        val dto = NotificationPrefs(3.0, setOf(Priority.Urgent, Priority.Low), true).toDto("u1")
        val row = json.encodeToJsonElement(NotificationPrefsDto.serializer(), dto).jsonObject

        assertEquals("u1", row["user_id"]?.jsonPrimitive?.content)
        assertEquals(listOf("low", "urgent"), row["priorities"]?.jsonArray?.map { it.jsonPrimitive.content })
        assertEquals("3.0", row["radius_km"]?.jsonPrimitive?.content)
        assertEquals("true", row["push_enabled"]?.jsonPrimitive?.content)
    }

    @Test
    fun historyWithoutOrganizationIsAttributedToTheAdmin() {
        val rows = listOf(StatusHistoryDto(status = "closed", changedBy = "admin", createdAt = "2026-11-22T10:00:00+00:00"))

        val history = statusHistoryOf(rows, reportRow(hidden = false), organizationName = null)

        assertEquals(ReportStatusRules.ADMIN_RESPONSIBLE, history.single().responsibleName)
    }
}
