package com.example.fluffypawtrack.data.supabase

import com.example.fluffypawtrack.domain.model.ReportStatus
import com.example.fluffypawtrack.domain.model.ReportStatusRules
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
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

    @Test
    fun historyWithoutOrganizationIsAttributedToTheAdmin() {
        val rows = listOf(StatusHistoryDto(status = "closed", changedBy = "admin", createdAt = "2026-11-22T10:00:00+00:00"))

        val history = statusHistoryOf(rows, reportRow(hidden = false), organizationName = null)

        assertEquals(ReportStatusRules.ADMIN_RESPONSIBLE, history.single().responsibleName)
    }
}
