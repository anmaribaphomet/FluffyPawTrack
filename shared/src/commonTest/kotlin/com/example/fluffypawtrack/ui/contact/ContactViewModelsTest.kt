package com.example.fluffypawtrack.ui.contact

import com.example.fluffypawtrack.data.fake.FakeAuthRepository
import com.example.fluffypawtrack.data.fake.FakeContactRequestRepository
import com.example.fluffypawtrack.data.fake.FakeReportRepository
import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.domain.model.ContactRequestRules
import com.example.fluffypawtrack.domain.model.ContactRequestStatus
import com.example.fluffypawtrack.domain.model.ReportStatus
import com.example.fluffypawtrack.domain.model.Role
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ContactViewModelsTest {

    private val demo = SampleData.demoUser
    private val carlos = SampleData.users.first { it.id == "user-2" }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun <T> TestScope.collecting(state: StateFlow<T>): StateFlow<T> {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
        return state
    }

    private class Setup(val auth: FakeAuthRepository, val reports: FakeReportRepository, val requests: FakeContactRequestRepository)

    private fun setup(signedIn: Boolean = true): Setup {
        val auth = FakeAuthRepository(if (signedIn) demo else null)
        val reports = FakeReportRepository()
        return Setup(auth, reports, FakeContactRequestRepository(reports))
    }

    private fun Setup.form(reportId: String = "report-2") =
        ContactRequestFormViewModel(reportId, auth, requests, reports)

    private fun Setup.list() = ContactRequestsViewModel(auth, requests, reports)

    // --- "Es mi mascota" form ---

    @Test
    fun formLoadsTheReport() = runTest {
        assertEquals("report-2", setup().form().uiState.value.report?.id)
    }

    @Test
    fun messageNeedsBetween20And500CharactersToSubmit() = runTest {
        val vm = setup().form()

        vm.onMessageChange("a".repeat(19))
        assertFalse(vm.uiState.value.canSubmit)
        assertEquals("Escribe al menos 20 caracteres", vm.uiState.value.messageHint)

        vm.onMessageChange("a".repeat(20))
        assertTrue(vm.uiState.value.canSubmit)
        assertNull(vm.uiState.value.messageHint)

        vm.onMessageChange("a".repeat(500))
        assertTrue(vm.uiState.value.canSubmit)

        vm.onMessageChange("a".repeat(501))
        assertFalse(vm.uiState.value.canSubmit)
        assertEquals("El mensaje no puede pasar de 500 caracteres", vm.uiState.value.messageHint)
    }

    @Test
    fun counterShowsTrimmedLengthOutOf500() = runTest {
        val vm = setup().form()
        vm.onMessageChange("  Hola  ")
        assertEquals("4/500", vm.uiState.value.counter)
    }

    @Test
    fun submittingAShortMessageShowsTheHintAndSendsNothing() = runTest {
        val s = setup()
        val vm = s.form()
        vm.onMessageChange("muy corto")
        vm.onSubmit()

        assertEquals("Escribe al menos 20 caracteres", vm.uiState.value.error)
        assertFalse(vm.uiState.value.sent)
    }

    @Test
    fun validSubmitSendsThePendingRequestWithThePhoto() = runTest {
        val s = setup()
        // The demo user already has request-1 on report-2; report-5 is their own. Use Carlos.
        s.auth.signIn(carlos.email!!, SampleData.DEMO_PASSWORD).getOrThrow()
        val reportId = s.reports.createReport(demo.id, lostReport()).getOrThrow().id
        val vm = s.form(reportId)

        vm.onMessageChange("Es mi perro Max, tiene una mancha blanca en el pecho.")
        vm.onPhotoSelected("photo-1")
        vm.onSubmit()

        assertTrue(vm.uiState.value.sent)
        val sent = collecting(s.list().uiState).value.sent.first { it.request.reportId == reportId }
        assertEquals(ContactRequestStatus.Pending, sent.request.status)
        assertEquals("photo-1", sent.request.photoUrl)
    }

    @Test
    fun repositoryRefusalIsShownAsReadableError() = runTest {
        val vm = setup().form("report-2")
        vm.onMessageChange("Mi gata Nieve se perdió por la colonia Centenario.")
        vm.onSubmit()

        assertEquals("Ya tienes una solicitud activa en este reporte", vm.uiState.value.error)
        assertFalse(vm.uiState.value.sent)
    }

    @Test
    fun withoutSessionTheFormAsksForAnAccount() = runTest {
        val vm = setup(signedIn = false).form()
        vm.onMessageChange("Mi gata Nieve se perdió por la colonia Centenario.")
        vm.onSubmit()
        assertEquals("Necesitas una cuenta para enviar solicitudes de contacto", vm.uiState.value.error)
    }

    // --- "Solicitudes de contacto" ---

    @Test
    fun tabsListSentAndReceivedRequestsOfTheSignedInUser() = runTest {
        val vm = setup().list()
        val state = collecting(vm.uiState)

        assertEquals(ContactRequestsTab.Sent, state.value.tab)
        assertEquals(listOf("request-1"), state.value.visible.map { it.request.id })
        assertEquals("report-2", state.value.visible.single().report?.id)

        vm.selectTab(ContactRequestsTab.Received)
        assertEquals(listOf("request-2"), state.value.visible.map { it.request.id })
        assertEquals(listOf("Enviadas", "Recibidas"), ContactRequestsTab.entries.map { it.label })
    }

    @Test
    fun pendingCardsShowNoContactDetails() = runTest {
        val state = collecting(setup().list().uiState)
        assertTrue((state.value.sent + state.value.received).all { it.contact == null })
        assertTrue(state.value.sent.single().canCancel)
        assertTrue(state.value.received.single().canRespond)
    }

    @Test
    fun cancelFromTheSentTab() = runTest {
        val vm = setup().list()
        val state = collecting(vm.uiState)
        vm.cancel("request-1")
        assertEquals(ContactRequestStatus.Cancelled, state.value.sent.single().request.status)
        assertFalse(state.value.sent.single().canCancel)
    }

    @Test
    fun acceptingShowsTheRequesterDetailsAndDeliveredClosesTheCase() = runTest {
        val s = setup()
        val vm = s.list()
        val state = collecting(vm.uiState)
        vm.selectTab(ContactRequestsTab.Received)

        vm.accept("request-2")
        val accepted = state.value.received.single()
        assertEquals(ContactRequestStatus.Accepted, accepted.request.status)
        assertEquals("Carlos Moreno", accepted.contact?.fullName)
        assertEquals("6620000002", accepted.contact?.phone)
        assertTrue(accepted.canMarkDelivered)

        vm.markDelivered("request-2")
        assertEquals(ReportStatus.Closed, s.reports.getReport("report-5")?.status)
        assertTrue(state.value.received.single().isDelivered)
        assertFalse(state.value.received.single().canMarkDelivered)
    }

    @Test
    fun rejectFromTheReceivedTab() = runTest {
        val vm = setup().list()
        val state = collecting(vm.uiState)
        vm.reject("request-2")
        assertEquals(ContactRequestStatus.Rejected, state.value.received.single().request.status)
        assertNull(state.value.received.single().contact)
    }

    @Test
    fun failedActionShowsReadableMessage() = runTest {
        val vm = setup().list()
        val state = collecting(vm.uiState)
        vm.accept("request-1") // demo is the requester, not the recipient
        assertEquals("Solo el receptor puede responder esta solicitud", state.value.message)
        vm.dismissMessage()
        assertNull(state.value.message)
    }

    // --- "Es mi mascota" button state ---

    @Test
    fun buttonOnlyForPossiblyLostReportsOfSomeoneElse() {
        val report2 = SampleData.reports.first { it.id == "report-2" }
        val street = SampleData.reports.first { it.id == "report-1" }
        val own = SampleData.reports.first { it.id == "report-5" }

        assertEquals(ContactActionState.Enabled, contactActionState(report2, demo))
        assertEquals(ContactActionState.Hidden, contactActionState(street, carlos))
        assertEquals(ContactActionState.Hidden, contactActionState(own, demo))
        assertEquals(ContactActionState.Hidden, contactActionState(report2, demo.copy(role = Role.Affiliate)))
    }

    @Test
    fun anonymousReportWithoutShelterDisablesTheButtonWithTheExactText() = runTest {
        val anonymous = FakeReportRepository().createReport(null, lostReport()).getOrThrow()
        val state = contactActionState(anonymous, demo)
        assertEquals(
            ContactActionState.Disabled("Este reporte es anónimo. Podrás enviar una solicitud cuando un albergue lo tome."),
            state,
        )
        assertEquals(ContactRequestRules.ANONYMOUS_REPORT_NOTICE, (state as ContactActionState.Disabled).reason)
        assertNotNull(contactActionState(anonymous.copy(assignedOrganizationId = "org-imbpa"), demo) as? ContactActionState.Enabled)
    }

    @Test
    fun guestsSeeTheButtonDisabledAndAskedForAnAccount() {
        val report2 = SampleData.reports.first { it.id == "report-2" }
        assertEquals(
            ContactActionState.Disabled("Necesitas una cuenta para enviar solicitudes de contacto"),
            contactActionState(report2, null),
        )
    }

    private fun lostReport() = com.example.fluffypawtrack.domain.model.NewReport(
        category = com.example.fluffypawtrack.domain.model.ReportCategory.PossiblyLost,
        priority = com.example.fluffypawtrack.domain.model.Priority.Medium,
        description = "Perrita negra con suéter azul",
        photoUrl = null,
        location = com.example.fluffypawtrack.domain.model.GeoPoint(29.08, -110.96),
        address = "Calle Matamoros 120",
        reference = null,
    )
}
