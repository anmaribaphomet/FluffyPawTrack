package com.example.fluffypawtrack.data.fake

import com.example.fluffypawtrack.domain.model.GeoPoint
import com.example.fluffypawtrack.domain.model.OrganizationDraft
import com.example.fluffypawtrack.domain.model.OrganizationProfile
import com.example.fluffypawtrack.domain.model.OrganizationRules
import com.example.fluffypawtrack.domain.model.OrganizationStatus
import com.example.fluffypawtrack.domain.model.OrganizationType
import com.example.fluffypawtrack.domain.model.Role
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FakeOrganizationAdminTest {

    private val admin = SampleData.demoUser.copy(id = "user-admin-test", email = "admin-test@correo.com", role = Role.Admin)
    private val auth = FakeAuthRepository(extraUsers = listOf(admin))
    private val notifications = FakeNotificationRepository()
    private val organizations = FakeOrganizationRepository(
        currentUser = { auth.currentUser.value },
        updateUsers = auth::updateUsers,
        notifications = notifications,
    )

    private val draft = OrganizationDraft(
        type = OrganizationType.SafePoint,
        name = "Punto Seguro Pitic",
        description = "Tienda que resguarda animales mientras llega un albergue.",
        location = GeoPoint(29.09, -110.96),
        address = "Blvd. Kino 300, Hermosillo",
        schedule = "Todos los días, 9:00 a 21:00",
        capacity = 3,
        phone = "6620001111",
        email = "pitic@seguro.example",
        social = null,
        photoUrl = "res:sample_place_shelter_kennels",
    )

    private fun Result<*>.message(): String? = exceptionOrNull()?.message

    private suspend fun requestAsDemoUser(): String {
        auth.signIn(SampleData.demoUser.email!!, SampleData.DEMO_PASSWORD).getOrThrow()
        return organizations.requestAffiliation(
            OrganizationType.Shelter, "Refugio Nuevo", "Calle 1, Hermosillo", "6620002222",
            "refugio@nuevo.example", "Rescatistas del poniente.", "res:sample_place_shelter_kennels",
        ).getOrThrow().id
    }

    private suspend fun signInAdmin() {
        auth.signIn(admin.email!!, SampleData.DEMO_PASSWORD).getOrThrow()
    }

    @Test
    fun approvingPromotesTheRequesterAndShowsTheOrganization() = runTest {
        val id = requestAsDemoUser()
        signInAdmin()
        assertTrue(organizations.observePendingAffiliations().first().any { it.id == id })

        val approved = organizations.reviewAffiliation(id, approve = true).getOrThrow()

        assertEquals(OrganizationStatus.Approved, approved.status)
        assertTrue(organizations.observeActiveOrganizations().first().any { it.id == id })
        assertFalse(organizations.observePendingAffiliations().first().any { it.id == id })
        val promoted = auth.findUser(SampleData.demoUser.id)
        assertEquals(Role.Affiliate, promoted?.role)
        assertEquals(id, promoted?.organizationId)
        val notice = notifications.observeNotifications(SampleData.demoUser.id).first().last()
        assertEquals("Afiliación aprobada", notice.title)
        assertEquals("Tu solicitud de afiliación para Refugio Nuevo fue aprobada.", notice.message)
    }

    @Test
    fun rejectingKeepsItOffTheMapAndTheRequesterAUser() = runTest {
        val id = requestAsDemoUser()
        signInAdmin()

        val rejected = organizations.reviewAffiliation(id, approve = false, reason = "  Faltan datos de contacto  ").getOrThrow()

        assertEquals(OrganizationStatus.Rejected, rejected.status)
        assertEquals("Faltan datos de contacto", rejected.rejectionReason)
        assertEquals("Faltan datos de contacto", organizations.getOrganization(id)?.rejectionReason)
        assertFalse(organizations.observeActiveOrganizations().first().any { it.id == id })
        assertEquals(Role.User, auth.findUser(SampleData.demoUser.id)?.role)
        val notice = notifications.observeNotifications(SampleData.demoUser.id).first().last()
        assertEquals("Afiliación no aprobada", notice.title)
        assertEquals(
            "Tu solicitud de afiliación para Refugio Nuevo no fue aprobada.\nMotivo: Faltan datos de contacto",
            notice.message,
        )
        assertEquals("Esta solicitud de afiliación ya fue revisada", organizations.reviewAffiliation(id, true).message())
    }

    @Test
    fun rejectingNeedsAReasonOfAtMost500Characters() = runTest {
        val id = requestAsDemoUser()
        signInAdmin()

        assertEquals("Escribe el motivo del rechazo", organizations.reviewAffiliation(id, approve = false).message())
        assertEquals("Escribe el motivo del rechazo", organizations.reviewAffiliation(id, false, "   ").message())
        assertEquals(
            "El motivo debe tener como máximo 500 caracteres",
            organizations.reviewAffiliation(id, false, "a".repeat(501)).message(),
        )
        assertEquals(OrganizationStatus.Pending, organizations.getOrganization(id)?.status)

        val rejected = organizations.reviewAffiliation(id, false, "a".repeat(500)).getOrThrow()
        assertEquals(500, rejected.rejectionReason?.length)
    }

    @Test
    fun approvingStoresNoReason() = runTest {
        val id = requestAsDemoUser()
        signInAdmin()

        val approved = organizations.reviewAffiliation(id, approve = true, reason = "Todo en orden").getOrThrow()

        assertNull(approved.rejectionReason)
        assertNull(organizations.getOrganization(id)?.rejectionReason)
    }

    @Test
    fun onlyTheAdminReviewsAffiliations() = runTest {
        val id = requestAsDemoUser()
        assertEquals("Solo el administrador puede revisar afiliaciones", organizations.reviewAffiliation(id, true).message())
        signInAdmin()
        assertEquals("La organización no existe", organizations.reviewAffiliation("missing", true).message())
    }

    @Test
    fun sampleDataHasAPendingAffiliation() = runTest {
        assertTrue(organizations.observePendingAffiliations().first().any { it.name == "Rescate Animal Norte" })
    }

    @Test
    fun adminCreatesEditsHidesAndDeletesPlaces() = runTest {
        signInAdmin()
        val created = organizations.createOrganization(draft).getOrThrow()
        assertEquals(OrganizationStatus.Approved, created.status)
        assertEquals(3, created.capacity)
        assertTrue(organizations.observeActiveOrganizations().first().any { it.id == created.id })

        val edited = organizations.updateOrganization(created.id, draft.copy(name = "Punto Seguro Pitic Norte")).getOrThrow()
        assertEquals("Punto Seguro Pitic Norte", edited.name)

        assertFalse(organizations.setActive(created.id, false).getOrThrow().active)
        assertFalse(organizations.observeActiveOrganizations().first().any { it.id == created.id })
        assertTrue(organizations.observeAllForAdmin().first().any { it.id == created.id })

        organizations.deleteOrganization(created.id).getOrThrow()
        assertNull(organizations.getOrganization(created.id))
        assertEquals("La organización no existe", organizations.deleteOrganization(created.id).message())
    }

    @Test
    fun directoryNeedsNameAddressAndPhoto() = runTest {
        signInAdmin()
        assertEquals(OrganizationRules.MISSING_NAME_OR_ADDRESS, organizations.createOrganization(draft.copy(name = " ")).message())
        assertEquals(OrganizationRules.MISSING_PHOTO, organizations.createOrganization(draft.copy(photoUrl = "")).message())
        assertEquals(OrganizationRules.NEGATIVE_CAPACITY, organizations.createOrganization(draft.copy(capacity = -1)).message())
    }

    @Test
    fun onlyTheAdminManagesTheDirectory() = runTest {
        auth.signIn(SampleData.demoAffiliate.email!!, SampleData.DEMO_PASSWORD).getOrThrow()
        val message = "Solo el administrador puede administrar el directorio"
        assertEquals(message, organizations.createOrganization(draft).message())
        assertEquals(message, organizations.setActive("org-imbpa", false).message())
        assertEquals(message, organizations.deleteOrganization("org-imbpa").message())
    }

    @Test
    fun deletingAnOrganizationReturnsItsAffiliatesToUsers() = runTest {
        signInAdmin()
        organizations.deleteOrganization("org-patitas").getOrThrow()
        val former = auth.findUser(SampleData.demoAffiliate.id)
        assertEquals(Role.User, former?.role)
        assertNull(former?.organizationId)
    }

    @Test
    fun affiliateEditsOnlyTheirOwnOrganization() = runTest {
        auth.signIn(SampleData.demoAffiliate.email!!, SampleData.DEMO_PASSWORD).getOrThrow()
        val profile = OrganizationProfile(
            description = "Rescatamos perros y gatos.",
            schedule = "Sábados 10:00 a 14:00",
            phone = "6620003002",
            email = "hola@patitas.example",
            social = "@patitashmo",
            photoUrl = "res:sample_place_shelter_kennels",
        )

        val updated = organizations.updateOwnOrganization("org-patitas", profile).getOrThrow()
        assertEquals("Sábados 10:00 a 14:00", updated.schedule)
        assertEquals("@patitashmo", updated.social)
        assertEquals(OrganizationStatus.Approved, updated.status)

        assertEquals(
            "Solo los afiliados de la organización pueden editar su perfil",
            organizations.updateOwnOrganization("org-imbpa", profile).message(),
        )
        assertEquals(OrganizationRules.MISSING_PHOTO, organizations.updateOwnOrganization("org-patitas", profile.copy(photoUrl = "")).message())
    }
}
