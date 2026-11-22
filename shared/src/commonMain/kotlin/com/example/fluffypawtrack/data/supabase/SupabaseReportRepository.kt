package com.example.fluffypawtrack.data.supabase

import com.example.fluffypawtrack.domain.model.NewReport
import com.example.fluffypawtrack.domain.model.Report
import com.example.fluffypawtrack.domain.model.ReportStatus
import com.example.fluffypawtrack.domain.model.StatusHistoryEntry
import com.example.fluffypawtrack.domain.repository.ReportRepository
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
 * Reports through PostgREST with RLS; edits and status changes through the `update_report` and
 * `set_report_status` RPCs. New photos go to `report-photos/<uid>/`.
 */
internal class SupabaseReportRepository(
    private val client: SupabaseClient,
    private val queries: QueryRegistry,
    private val photos: PhotoUploader,
) : ReportRepository {

    override fun observeActiveReports(): Flow<List<Report>> = queries.observe("reports:active", emptyList()) {
        selectReports {
            neq("status", ReportStatus.Closed.dbValue)
            // RLS lets the admin and the reporter read hidden reports too; the map never shows them.
            eq("hidden", false)
        }
    }

    override fun observeReportsBy(reporterId: String): Flow<List<Report>> =
        queries.observe("reports:by:$reporterId", emptyList()) {
            selectReports { eq("reporter_id", reporterId) }
        }

    override suspend fun getReport(id: String): Report? = remoteOrNull {
        selectReports { eq("id", id) }.firstOrNull()
    }

    /**
     * RLS requires `reporter_id = auth.uid()`, so the row always belongs to the current session
     * (an anonymous one included); [reporterId] only has to match it.
     */
    override suspend fun createReport(reporterId: String?, report: NewReport): Result<Report> = remote {
        val uid = client.auth.currentUserOrNull()?.id
            ?: throw RuleViolation("Inicia sesión o continúa sin cuenta para publicar el reporte")
        val photoUrl = photos.store(report.photoUrl, StoragePaths.REPORT_PHOTOS, folder = uid)
        client.from(TABLE)
            .insert(report.toInsertDto(reporterId = uid, photoUrl = photoUrl)) { select() }
            .decodeList<ReportDto>()
            .first()
            .toDomain()
    }.thenRefresh(queries)

    override suspend fun deleteReport(id: String): Result<Unit> = remote {
        val deleted = client.from(TABLE).delete {
            select()
            filter { eq("id", id) }
        }.decodeList<ReportDto>()
        // RLS turns a forbidden delete into "no rows", like an unknown id.
        if (deleted.isEmpty()) throw RuleViolation("El reporte no existe")
    }.thenRefresh(queries)

    override suspend fun updateReport(id: String, description: String, reference: String?): Result<Report> = remote {
        client.postgrest.rpc(
            "update_report",
            buildJsonObject {
                put("p_report_id", id)
                put("p_description", description)
                put("p_reference", reference)
            },
        ).decodeAs<ReportDto>().toDomain()
    }.thenRefresh(queries)

    /** Allowed to the assigned organization and the admin (`set_report_status`). */
    override suspend fun closeReport(id: String): Result<Report> = setStatus(id, ReportStatus.Closed)

    override fun observeInbox(organizationId: String): Flow<List<Report>> =
        queries.observe("reports:inbox:$organizationId", emptyList()) {
            selectReports(newestFirst = true) {
                neq("status", ReportStatus.Closed.dbValue)
                eq("hidden", false)
            }.filter { it.assignedOrganizationId == null || it.assignedOrganizationId == organizationId }
        }

    override fun observeCasesOf(organizationId: String): Flow<List<Report>> =
        queries.observe("reports:cases:$organizationId", emptyList()) {
            selectReports(newestFirst = true) {
                eq("assigned_org_id", organizationId)
                neq("status", ReportStatus.Closed.dbValue)
                eq("hidden", false)
            }
        }

    /** RLS returns every row, hidden ones included, only to the admin. */
    override fun observeAllForAdmin(): Flow<List<Report>> =
        queries.observe("reports:admin", emptyList()) { selectReports(newestFirst = true) {} }

    override suspend fun takeReport(reportId: String): Result<Report> = remote {
        client.postgrest.rpc("take_report", buildJsonObject { put("p_report_id", reportId) })
            .decodeAs<ReportDto>().toDomain()
    }.thenRefresh(queries)

    override suspend fun setStatus(reportId: String, status: ReportStatus): Result<Report> = remote {
        client.postgrest.rpc(
            "set_report_status",
            buildJsonObject {
                put("p_report_id", reportId)
                put("p_status", status.dbValue)
            },
        ).decodeAs<ReportDto>().toDomain()
    }.thenRefresh(queries)

    override suspend fun setHidden(reportId: String, hidden: Boolean): Result<Report> = remote {
        client.postgrest.rpc(
            "set_report_hidden",
            buildJsonObject {
                put("p_report_id", reportId)
                put("p_hidden", hidden)
            },
        ).decodeAs<ReportDto>().toDomain()
    }.thenRefresh(queries)

    override fun observeStatusHistory(reportId: String): Flow<List<StatusHistoryEntry>> =
        queries.observe("reports:history:$reportId", emptyList()) {
            val report = client.from(TABLE).select { filter { eq("id", reportId) } }
                .decodeList<ReportDto>().firstOrNull() ?: return@observe emptyList()
            val rows = client.from("report_status_history").select {
                filter { eq("report_id", reportId) }
                order("created_at", Order.ASCENDING)
            }.decodeList<StatusHistoryDto>()
            val organizationName = report.assignedOrgId?.let { orgId ->
                client.from("organizations").select { filter { eq("id", orgId) } }
                    .decodeList<OrganizationDto>().firstOrNull()?.name
            }
            statusHistoryOf(rows, report, organizationName)
        }

    private suspend fun selectReports(
        newestFirst: Boolean = false,
        filters: PostgrestFilterBuilder.() -> Unit,
    ): List<Report> = client.from(TABLE).select {
        filter(filters)
        order("created_at", if (newestFirst) Order.DESCENDING else Order.ASCENDING)
    }.decodeList<ReportDto>().map { it.toDomain() }

    private companion object {
        const val TABLE = "reports"
    }
}
