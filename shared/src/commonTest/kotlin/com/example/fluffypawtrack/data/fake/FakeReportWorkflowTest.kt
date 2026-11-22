package com.example.fluffypawtrack.data.fake

import com.example.fluffypawtrack.domain.model.ContactRequestRules
import com.example.fluffypawtrack.domain.model.ContactRequestStatus
import com.example.fluffypawtrack.domain.model.NotificationType
import com.example.fluffypawtrack.domain.model.ReportStatus
import com.example.fluffypawtrack.domain.model.ReportStatusRules
import com.example.fluffypawtrack.domain.model.Role
import com.example.fluffypawtrack.domain.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FakeReportWorkflowTest {

    private val patitas = SampleData.demoAffiliate
    private val huellitas = patitas.copy(id = "user-huellitas", organizationId = "org-huellitas")
    private val admin = patitas.copy(id = "user-admin", role = Role.Admin, organizationId = null)
    private val user = SampleData.demoUser

    private var signedIn: UserProfile? = null
    private val notifications = FakeNotificationRepository()
    private val reports = FakeReportRepository(currentUser = { signedIn }, notifications = notifications)

    private fun Result<*>.message(): String? = exceptionOrNull()?.message

    @Test
    fun onlyOneOrganizationCanTakeACase() = runTest {
        signedIn = patitas
        val taken = reports.takeReport("report-1").getOrThrow()
        assertEquals(ReportStatus.OnTheWay, taken.status)
        assertEquals("org-patitas", taken.assignedOrganizationId)

        signedIn = huellitas
        assertEquals("Este caso ya fue tomado por otro albergue", reports.takeReport("report-1").message())
        assertEquals("org-patitas", reports.getReport("report-1")?.assignedOrganizationId)
    }

    @Test
    fun takeReportFailsForClosedAndUnknownReports() = runTest {
        signedIn = patitas
        assertEquals("Este caso ya está cerrado", reports.takeReport("report-6").message())
        assertEquals("El reporte no existe", reports.takeReport("missing").message())
    }

    @Test
    fun nonAffiliateCannotTakeACase() = runTest {
        signedIn = user
        assertEquals("Solo los albergues afiliados pueden tomar casos", reports.takeReport("report-1").message())
        signedIn = null
        assertEquals("Solo los albergues afiliados pueden tomar casos", reports.takeReport("report-1").message())
        assertEquals(ReportStatus.Pending, reports.getReport("report-1")?.status)
    }

    @Test
    fun statusOnlyMovesForwardAndOnlyForTheAssignedOrganization() = runTest {
        signedIn = patitas
        reports.takeReport("report-1").getOrThrow()
        assertEquals(ReportStatus.Sheltered, reports.setStatus("report-1", ReportStatus.Sheltered).getOrThrow().status)
        assertEquals("El estado del reporte solo puede avanzar", reports.setStatus("report-1", ReportStatus.OnTheWay).message())
        assertEquals("El estado del reporte solo puede avanzar", reports.setStatus("report-1", ReportStatus.Sheltered).message())

        signedIn = huellitas
        assertEquals(
            "Solo el albergue asignado puede cambiar el estado del reporte",
            reports.setStatus("report-1", ReportStatus.Closed).message(),
        )
        assertEquals(ReportStatus.Sheltered, reports.getReport("report-1")?.status)
        assertEquals("El reporte no existe", reports.setStatus("missing", ReportStatus.Closed).message())
    }

    @Test
    fun adminMaySetAnyStatus() = runTest {
        signedIn = admin
        assertEquals(ReportStatus.Pending, reports.setStatus("report-4", ReportStatus.Pending).getOrThrow().status)
    }

    @Test
    fun statusChangeNotifiesTheReporter() = runTest {
        signedIn = patitas
        reports.takeReport("report-1").getOrThrow()
        val received = notifications.observeNotifications(user.id).first().last()
        assertEquals("Actualización de tu reporte", received.title)
        assertEquals("Tu reporte ahora está en \"En camino\".", received.message)
        assertEquals(NotificationType.Report, received.type)
    }

    @Test
    fun hiddenReportsLeaveUserListsButStayForTheAdmin() = runTest {
        signedIn = patitas
        assertEquals("Solo el administrador puede ocultar reportes", reports.setHidden("report-1", true).message())

        signedIn = admin
        assertTrue(reports.setHidden("report-1", true).getOrThrow().hidden)
        assertFalse(reports.observeActiveReports().first().any { it.id == "report-1" })
        assertFalse(reports.observeInbox("org-patitas").first().any { it.id == "report-1" })
        assertTrue(reports.observeAllForAdmin().first().any { it.id == "report-1" && it.hidden })
        assertEquals("El reporte no existe", reports.setHidden("missing", true).message())

        signedIn = patitas
        assertEquals("El reporte no existe", reports.takeReport("report-1").message())
    }

    @Test
    fun inboxShowsOpenReportsNotTakenByOthersNewestFirst() = runTest {
        val inbox = reports.observeInbox("org-patitas").first()
        assertTrue(inbox.none { it.status == ReportStatus.Closed })
        assertTrue(inbox.all { it.assignedOrganizationId == null || it.assignedOrganizationId == "org-patitas" })
        assertFalse(inbox.any { it.id == "report-3" }, "taken by Huellitas")
        assertEquals(inbox.sortedByDescending { it.createdAt }, inbox)
    }

    @Test
    fun casesAreTheOrganizationsOpenAssignedReports() = runTest {
        signedIn = patitas
        reports.takeReport("report-1").getOrThrow()
        val cases = reports.observeCasesOf("org-patitas").first()
        assertTrue(cases.any { it.id == "report-1" })
        assertTrue(cases.all { it.assignedOrganizationId == "org-patitas" && it.status != ReportStatus.Closed })
        assertEquals(cases.sortedByDescending { it.createdAt }, cases)
    }

    @Test
    fun adminListIncludesClosedReportsNewestFirst() = runTest {
        val all = reports.observeAllForAdmin().first()
        assertEquals(SampleData.reports.size, all.size)
        assertEquals(all.sortedByDescending { it.createdAt }, all)
    }

    @Test
    fun historyRecordsEveryStatusWithItsResponsible() = runTest {
        signedIn = patitas
        reports.takeReport("report-1").getOrThrow()
        reports.setStatus("report-1", ReportStatus.Sheltered).getOrThrow()

        val history = reports.observeStatusHistory("report-1").first()
        assertEquals(listOf(ReportStatus.Pending, ReportStatus.OnTheWay, ReportStatus.Sheltered), history.map { it.status })
        assertEquals(ReportStatusRules.REPORTER_RESPONSIBLE, history[0].responsibleName)
        assertEquals("Patitas Callejeras HMO", history[1].responsibleName)
        assertEquals(history.sortedBy { it.changedAt }, history)
    }

    @Test
    fun sampleReportsStartWithAHistory() = runTest {
        val history = reports.observeStatusHistory("report-4").first()
        assertEquals(listOf(ReportStatus.Pending, ReportStatus.OnTheWay, ReportStatus.Sheltered), history.map { it.status })
    }

    @Test
    fun closingOrHidingRejectsPendingContactRequests() = runTest {
        val contacts = FakeContactRequestRepository(reportRepository = reports)
        signedIn = admin
        reports.setStatus("report-5", ReportStatus.Closed).getOrThrow()
        val closed = contacts.observeReceived(user.id).first().first { it.id == "request-2" }
        assertEquals(ContactRequestStatus.Rejected, closed.status)
        assertEquals(ContactRequestRules.CASE_CLOSED_NOTE, closed.statusNote)

        reports.setHidden("report-2", true).getOrThrow()
        val hidden = contacts.observeSent(user.id).first().first { it.id == "request-1" }
        assertEquals(ContactRequestStatus.Rejected, hidden.status)
        assertEquals("El reporte ya no está disponible", hidden.statusNote)
    }
}
