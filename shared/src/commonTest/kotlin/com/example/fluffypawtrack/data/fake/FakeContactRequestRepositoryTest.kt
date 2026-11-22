package com.example.fluffypawtrack.data.fake

import com.example.fluffypawtrack.domain.model.ContactDetails
import com.example.fluffypawtrack.domain.model.ContactRequestRules
import com.example.fluffypawtrack.domain.model.ContactRequestStatus
import com.example.fluffypawtrack.domain.model.GeoPoint
import com.example.fluffypawtrack.domain.model.NewReport
import com.example.fluffypawtrack.domain.model.Priority
import com.example.fluffypawtrack.domain.model.Report
import com.example.fluffypawtrack.domain.model.ReportCategory
import com.example.fluffypawtrack.domain.model.ReportStatus
import com.example.fluffypawtrack.domain.model.Role
import com.example.fluffypawtrack.domain.model.UserProfile
import com.example.fluffypawtrack.domain.repository.ReportRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FakeContactRequestRepositoryTest {

    private val demo = SampleData.demoUser
    private val carlos = SampleData.users.first { it.id == "user-2" }
    private val ana = UserProfile(
        id = "user-ana",
        email = "ana@correo.com",
        firstName = "Ana",
        lastName = "Ruiz",
        phone = "6620000003",
        role = Role.User,
        organizationId = null,
        isAnonymous = false,
    )
    private val shelterStaff = ana.copy(
        id = "user-staff",
        email = "staff@imbpa.example",
        firstName = "Luis",
        lastName = "Peña",
        phone = "6620000009",
        role = Role.Affiliate,
        organizationId = "org-imbpa",
    )
    private val noPhone = ana.copy(id = "user-nophone", phone = null)
    private val anonymous = ana.copy(id = "anonymous-1", email = null, phone = null, isAnonymous = true)

    private val people = listOf(demo, carlos, ana, shelterStaff, noPhone, anonymous)
    private val message = "Es mi gata Nieve, tiene un collar rojo con cascabel."

    // report-2: Carlos's possibly-lost report. report-5: demo's. report-6: closed.
    private fun repositories(reports: FakeReportRepository = FakeReportRepository()) =
        reports to FakeContactRequestRepository(
            reportRepository = reports,
            findUser = { id -> people.firstOrNull { it.id == id } },
        )

    private fun lostReport(reporterId: String?) = NewReport(
        category = ReportCategory.PossiblyLost,
        priority = Priority.Medium,
        description = "Perrita negra con suéter azul",
        photoUrl = "memory://photo/1",
        location = GeoPoint(29.08, -110.96),
        address = "Calle Matamoros 120",
        reference = null,
    )

    private fun failureMessage(result: Result<*>): String? = result.exceptionOrNull()?.message

    // --- Existing behavior ---

    @Test
    fun observeSentAndReceivedKeepFilteringByRequesterAndRecipientUser() = runTest {
        val repository = FakeContactRequestRepository()
        assertEquals(listOf("request-1"), repository.observeSent(demo.id).first().map { it.id })
        assertEquals(listOf("request-2"), repository.observeReceived(demo.id).first().map { it.id })
        assertEquals(listOf("request-2"), repository.observeSent(carlos.id).first().map { it.id })
        assertEquals(listOf("request-1"), repository.observeReceived(carlos.id).first().map { it.id })
    }

    // --- Sending ---

    @Test
    fun sendCreatesPendingRequestForTheReporterWithOnlyTheFirstName() = runTest {
        val (_, repository) = repositories()
        val sent = repository.send("report-2", ana.id, message, "photo-ref").getOrThrow()

        assertEquals(ContactRequestStatus.Pending, sent.status)
        assertEquals(carlos.id, sent.recipientUserId)
        assertNull(sent.recipientOrganizationId)
        assertEquals("Ana", sent.requesterFirstName)
        assertEquals("photo-ref", sent.photoUrl)
        assertTrue(repository.observeSent(ana.id).first().contains(sent))
        assertTrue(repository.observeReceived(carlos.id).first().contains(sent))
    }

    @Test
    fun recipientIsTheAssignedOrganizationWhenThereIsOne() = runTest {
        val (reportId, repository) = shelteredLostReport()
        val sent = repository.send(reportId, ana.id, message, null).getOrThrow()

        assertEquals("org-imbpa", sent.recipientOrganizationId)
        assertNull(sent.recipientUserId)
    }

    @Test
    fun anonymousReportWithoutOrganizationHasNoRecipient() = runTest {
        val reports = FakeReportRepository()
        val report = reports.createReport(null, lostReport(null)).getOrThrow()
        val (_, repository) = repositories(reports)

        val result = repository.send(report.id, ana.id, message, null)
        assertEquals(ContactRequestRules.ANONYMOUS_REPORT_NOTICE, failureMessage(result))
    }

    @Test
    fun onlyPossiblyLostReportsAcceptRequests() = runTest {
        val (_, repository) = repositories()
        // report-1 is "Situación de calle".
        val result = repository.send("report-1", ana.id, message, null)
        assertTrue(result.isFailure)
        assertEquals(
            "Solo puedes enviar solicitudes en reportes de \"Posible mascota extraviada\"",
            failureMessage(result),
        )
    }

    @Test
    fun closedReportsDoNotAcceptRequests() = runTest {
        val reports = FakeReportRepository()
        val report = reports.createReport(carlos.id, lostReport(carlos.id)).getOrThrow()
        reports.closeReport(report.id).getOrThrow()
        val (_, repository) = repositories(reports)

        assertEquals("Este caso ya está cerrado", failureMessage(repository.send(report.id, ana.id, message, null)))
    }

    @Test
    fun cannotSendToOwnReport() = runTest {
        val (_, repository) = repositories()
        assertEquals(
            "No puedes enviar una solicitud a tu propio reporte",
            failureMessage(repository.send("report-2", carlos.id, message, null)),
        )
    }

    @Test
    fun anonymousSessionsAndUnknownUsersCannotSend() = runTest {
        val (_, repository) = repositories()
        val expected = "Necesitas una cuenta para enviar solicitudes de contacto"
        assertEquals(expected, failureMessage(repository.send("report-2", anonymous.id, message, null)))
        assertEquals(expected, failureMessage(repository.send("report-2", "missing", message, null)))
    }

    @Test
    fun affiliatesAndAdminsCannotSend() = runTest {
        val admin = ana.copy(id = "user-admin", role = Role.Admin)
        val reports = FakeReportRepository()
        val repository = FakeContactRequestRepository(reports, findUser = { id -> (people + admin).firstOrNull { it.id == id } })
        val expected = "Solo los usuarios pueden enviar solicitudes de contacto"
        assertEquals(expected, failureMessage(repository.send("report-2", shelterStaff.id, message, null)))
        assertEquals(expected, failureMessage(repository.send("report-2", admin.id, message, null)))
    }

    @Test
    fun onlyOneActiveRequestPerUserAndReport() = runTest {
        val (_, repository) = repositories()
        repository.send("report-2", ana.id, message, null).getOrThrow()
        assertEquals(
            "Ya tienes una solicitud activa en este reporte",
            failureMessage(repository.send("report-2", ana.id, message, null)),
        )
    }

    @Test
    fun acceptedRequestAlsoCountsAsActive() = runTest {
        val (_, repository) = repositories()
        val sent = repository.send("report-2", ana.id, message, null).getOrThrow()
        repository.accept(sent.id, carlos.id).getOrThrow()
        assertTrue(repository.send("report-2", ana.id, message, null).isFailure)
    }

    @Test
    fun cancelledRequestCanBeSentAgain() = runTest {
        val (_, repository) = repositories()
        val sent = repository.send("report-2", ana.id, message, null).getOrThrow()
        repository.cancel(sent.id, ana.id).getOrThrow()
        assertTrue(repository.send("report-2", ana.id, message, null).isSuccess)
    }

    @Test
    fun cannotResendAfterRejection() = runTest {
        val (_, repository) = repositories()
        val sent = repository.send("report-2", ana.id, message, null).getOrThrow()
        repository.reject(sent.id, carlos.id).getOrThrow()
        assertEquals(
            "Tu solicitud en este reporte ya fue rechazada",
            failureMessage(repository.send("report-2", ana.id, message, null)),
        )
    }

    @Test
    fun phoneIsRequiredToSend() = runTest {
        val (_, repository) = repositories()
        assertEquals(
            "Agrega un teléfono a tu perfil antes de continuar",
            failureMessage(repository.send("report-2", noPhone.id, message, null)),
        )
    }

    @Test
    fun phoneIsRequiredToAccept() = runTest {
        val reports = FakeReportRepository()
        val report = reports.createReport(noPhone.id, lostReport(noPhone.id)).getOrThrow()
        val (_, repository) = repositories(reports)
        val sent = repository.send(report.id, ana.id, message, null).getOrThrow()

        assertEquals(
            "Agrega un teléfono a tu perfil antes de continuar",
            failureMessage(repository.accept(sent.id, noPhone.id)),
        )
    }

    @Test
    fun messageMustHaveBetween20And500Characters() = runTest {
        val (_, repository) = repositories()
        val expected = "El mensaje debe tener entre 20 y 500 caracteres"
        assertEquals(expected, failureMessage(repository.send("report-2", ana.id, "a".repeat(19), null)))
        assertEquals(expected, failureMessage(repository.send("report-2", ana.id, "a".repeat(501), null)))
        assertTrue(repository.send("report-2", ana.id, "a".repeat(20), null).isSuccess)
    }

    // --- Transitions ---

    @Test
    fun requesterCancelsOnlyOwnPendingRequests() = runTest {
        val (_, repository) = repositories()
        val sent = repository.send("report-2", ana.id, message, null).getOrThrow()

        assertEquals("Solo quien envió la solicitud puede cancelarla", failureMessage(repository.cancel(sent.id, carlos.id)))
        assertEquals(ContactRequestStatus.Cancelled, repository.cancel(sent.id, ana.id).getOrThrow().status)
    }

    @Test
    fun cancelIsOnlyAllowedWhilePending() = runTest {
        val (_, repository) = repositories()
        val sent = repository.send("report-2", ana.id, message, null).getOrThrow()
        repository.accept(sent.id, carlos.id).getOrThrow()
        assertEquals("Solo puedes cancelar solicitudes pendientes", failureMessage(repository.cancel(sent.id, ana.id)))
    }

    @Test
    fun onlyTheRecipientAcceptsOrRejects() = runTest {
        val (_, repository) = repositories()
        val sent = repository.send("report-2", ana.id, message, null).getOrThrow()
        val expected = "Solo el receptor puede responder esta solicitud"
        assertEquals(expected, failureMessage(repository.accept(sent.id, ana.id)))
        assertEquals(expected, failureMessage(repository.reject(sent.id, demo.id)))
        assertEquals(ContactRequestStatus.Accepted, repository.accept(sent.id, carlos.id).getOrThrow().status)
    }

    @Test
    fun rejectMovesToRejected() = runTest {
        val (_, repository) = repositories()
        val sent = repository.send("report-2", ana.id, message, null).getOrThrow()
        assertEquals(ContactRequestStatus.Rejected, repository.reject(sent.id, carlos.id).getOrThrow().status)
    }

    @Test
    fun recipientCanAcceptMoreThanOneRequestOfTheSameReport() = runTest {
        val (_, repository) = repositories()
        val first = repository.send("report-2", ana.id, message, null).getOrThrow()
        // request-1 from the demo user is already pending on report-2.
        repository.accept(first.id, carlos.id).getOrThrow()
        assertEquals(ContactRequestStatus.Accepted, repository.accept("request-1", carlos.id).getOrThrow().status)
    }

    @Test
    fun finalStatesNeverChange() = runTest {
        val (_, repository) = repositories()
        val accepted = repository.send("report-2", ana.id, message, null).getOrThrow()
        repository.accept(accepted.id, carlos.id).getOrThrow()
        val rejected = repository.reject("request-1", carlos.id).getOrThrow()

        val expected = "Esta solicitud ya fue respondida"
        assertEquals(expected, failureMessage(repository.reject(accepted.id, carlos.id)))
        assertEquals(expected, failureMessage(repository.accept(rejected.id, carlos.id)))
        val sent = repository.send("report-5", ana.id, message, null).getOrThrow()
        repository.cancel(sent.id, ana.id).getOrThrow()
        assertEquals(expected, failureMessage(repository.accept(sent.id, demo.id)))
        assertEquals(
            listOf(ContactRequestStatus.Accepted, ContactRequestStatus.Rejected, ContactRequestStatus.Cancelled),
            listOf(accepted.id, rejected.id, sent.id).map { id ->
                repository.observeSent(ana.id).first().plus(repository.observeSent(demo.id).first())
                    .first { it.id == id }.status
            },
        )
    }

    @Test
    fun deliveredClosesTheReportAndRejectsOtherPendingRequests() = runTest {
        val (reports, repository) = repositories()
        val accepted = repository.send("report-2", ana.id, message, null).getOrThrow()
        repository.accept(accepted.id, carlos.id).getOrThrow()

        repository.markDelivered(accepted.id, carlos.id).getOrThrow()

        assertEquals(ReportStatus.Closed, reports.getReport("report-2")?.status)
        val other = repository.observeSent(demo.id).first().first { it.id == "request-1" }
        assertEquals(ContactRequestStatus.Rejected, other.status)
        assertEquals(ContactRequestRules.CASE_CLOSED_NOTE, other.statusNote)
        assertEquals(ContactRequestStatus.Accepted, repository.observeSent(ana.id).first().single().status)
    }

    @Test
    fun deliveredNeedsAnAcceptedRequestAndTheRecipient() = runTest {
        val (_, repository) = repositories()
        val sent = repository.send("report-2", ana.id, message, null).getOrThrow()
        assertEquals(
            "Solo puedes marcar como entregada una solicitud aceptada",
            failureMessage(repository.markDelivered(sent.id, carlos.id)),
        )
        repository.accept(sent.id, carlos.id).getOrThrow()
        assertEquals(
            "Solo el receptor puede responder esta solicitud",
            failureMessage(repository.markDelivered(sent.id, ana.id)),
        )
    }

    // --- Contact details ---

    @Test
    fun contactDetailsAreHiddenUntilAccepted() = runTest {
        val (_, repository) = repositories()
        val sent = repository.send("report-2", ana.id, message, null).getOrThrow()
        assertNull(repository.getContactDetails(sent.id, ana.id))
        assertNull(repository.getContactDetails(sent.id, carlos.id))
    }

    @Test
    fun acceptedPartiesSeeEachOthersFullContactDetails() = runTest {
        val (_, repository) = repositories()
        val sent = repository.send("report-2", ana.id, message, null).getOrThrow()
        repository.accept(sent.id, carlos.id).getOrThrow()

        assertEquals(ContactDetails("Carlos Moreno", "6620000002", "carlos@correo.com"), repository.getContactDetails(sent.id, ana.id))
        assertEquals(ContactDetails("Ana Ruiz", "6620000003", "ana@correo.com"), repository.getContactDetails(sent.id, carlos.id))
        assertNull(repository.getContactDetails(sent.id, demo.id), "a third person never gets details")
    }

    @Test
    fun organizationRecipientSharesTheOrganizationContactAndAnyAffiliateCanRespond() = runTest {
        val (reportId, repository) = shelteredLostReport()
        val sent = repository.send(reportId, ana.id, message, null).getOrThrow()

        // Carlos reported it, but the shelter decides now.
        assertEquals("Solo el receptor puede responder esta solicitud", failureMessage(repository.accept(sent.id, carlos.id)))
        repository.accept(sent.id, shelterStaff.id).getOrThrow()

        val imbpa = SampleData.organizations.first { it.id == "org-imbpa" }
        assertEquals(ContactDetails(imbpa.name, imbpa.phone, imbpa.email), repository.getContactDetails(sent.id, ana.id))
        assertEquals(ContactDetails("Ana Ruiz", "6620000003", "ana@correo.com"), repository.getContactDetails(sent.id, shelterStaff.id))
    }

    @Test
    fun organizationRequestsAreReceivedByItsAffiliatesOnly() = runTest {
        val (reportId, repository) = shelteredLostReport()
        val sent = repository.send(reportId, ana.id, message, null).getOrThrow()

        assertTrue(repository.observeReceived(shelterStaff.id).first().any { it.id == sent.id })
        assertTrue(repository.observeReceived(carlos.id).first().none { it.id == sent.id }, "the reporter no longer decides")
        assertTrue(repository.observeReceived(ana.id).first().none { it.id == sent.id })
        // Requests addressed to a person keep reaching that person.
        assertEquals(listOf("request-2"), repository.observeReceived(demo.id).first().map { it.id })
    }

    /**
     * A possibly-lost report by Carlos that org-imbpa has sheltered. ReportRepository has
     * no "take case" yet, so the assignment is applied by a delegating test repository.
     */
    private suspend fun shelteredLostReport(): Pair<String, FakeContactRequestRepository> {
        val base = FakeReportRepository()
        val created = base.createReport(carlos.id, lostReport(carlos.id)).getOrThrow()
        val sheltered = created.copy(status = ReportStatus.Sheltered, assignedOrganizationId = "org-imbpa")
        val reports = object : ReportRepository by base {
            override suspend fun getReport(id: String): Report? =
                if (id == created.id) sheltered else base.getReport(id)
        }
        return created.id to FakeContactRequestRepository(reports, findUser = { id -> people.firstOrNull { it.id == id } })
    }
}
