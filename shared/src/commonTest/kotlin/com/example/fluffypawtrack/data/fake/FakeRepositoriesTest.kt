package com.example.fluffypawtrack.data.fake

import com.example.fluffypawtrack.domain.model.GeoPoint
import com.example.fluffypawtrack.domain.model.ListingStatus
import com.example.fluffypawtrack.domain.model.NewReport
import com.example.fluffypawtrack.domain.model.OrganizationStatus
import com.example.fluffypawtrack.domain.model.Priority
import com.example.fluffypawtrack.domain.model.ReportCategory
import com.example.fluffypawtrack.domain.model.ReportStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FakeRepositoriesTest {

    private val newReport = NewReport(
        category = ReportCategory.Injured,
        priority = Priority.High,
        description = "Perro con una pata lastimada",
        photoUrl = null,
        location = GeoPoint(29.0892, -110.9613),
        address = "Blvd. Kino 100, Hermosillo",
        reference = null,
    )

    @Test
    fun sampleDataCoversReportsSheltersAndAdoptionsInHermosillo() {
        assertTrue(SampleData.reports.isNotEmpty())
        assertTrue(SampleData.organizations.isNotEmpty())
        assertTrue(SampleData.adoptionListings.isNotEmpty())
        val points = SampleData.reports.map { it.location } + SampleData.organizations.map { it.location }
        points.forEach { point ->
            assertTrue(point.latitude in 28.95..29.20, "latitude out of Hermosillo: $point")
            assertTrue(point.longitude in -111.10..-110.85, "longitude out of Hermosillo: $point")
        }
    }

    @Test
    fun activeReportsExcludeClosedOnes() = runTest {
        val active = FakeReportRepository().observeActiveReports().first()
        assertTrue(active.isNotEmpty())
        assertTrue(active.none { it.status == ReportStatus.Closed })
    }

    @Test
    fun createdReportIsPendingAndVisibleToItsReporter() = runTest {
        val repository = FakeReportRepository()
        val created = repository.createReport("user-1", newReport).getOrThrow()

        assertEquals(ReportStatus.Pending, created.status)
        assertEquals(created, repository.getReport(created.id))
        assertTrue(repository.observeActiveReports().first().contains(created))
        assertTrue(repository.observeReportsBy("user-1").first().contains(created))
    }

    @Test
    fun anonymousReportHasNoReporter() = runTest {
        val created = FakeReportRepository().createReport(null, newReport).getOrThrow()
        assertNull(created.reporterId)
    }

    @Test
    fun deletedReportDisappears() = runTest {
        val repository = FakeReportRepository()
        val created = repository.createReport("user-1", newReport).getOrThrow()

        assertTrue(repository.deleteReport(created.id).isSuccess)
        assertNull(repository.getReport(created.id))
        assertFalse(repository.observeActiveReports().first().contains(created))
    }

    @Test
    fun deletingUnknownReportFails() = runTest {
        assertTrue(FakeReportRepository().deleteReport("missing").isFailure)
    }

    @Test
    fun onlyApprovedActiveOrganizationsAreShown() = runTest {
        val shown = FakeOrganizationRepository().observeActiveOrganizations().first()
        assertTrue(shown.isNotEmpty())
        assertTrue(shown.all { it.status == OrganizationStatus.Approved && it.active })
        assertTrue(shown.size < SampleData.organizations.size, "sample data should include hidden places")
    }

    @Test
    fun availableListingsExcludeAdoptedAnimals() = runTest {
        val available = FakeAdoptionRepository().observeAvailableListings().first()
        assertTrue(available.isNotEmpty())
        assertTrue(available.all { it.status == ListingStatus.Available })
    }

    @Test
    fun toggleFavoriteAddsThenRemoves() = runTest {
        val repository = FakeFavoritesRepository()
        repository.toggleFavorite("user-1", "listing-1")
        assertEquals(setOf("listing-1"), repository.observeFavoriteIds("user-1").first())
        repository.toggleFavorite("user-1", "listing-1")
        assertEquals(emptySet(), repository.observeFavoriteIds("user-1").first())
    }

    @Test
    fun markAllAsReadLeavesNoUnreadNotifications() = runTest {
        val repository = FakeNotificationRepository()
        val userId = SampleData.demoUser.id
        assertTrue(repository.observeNotifications(userId).first().any { !it.isRead })

        repository.markAllAsRead(userId)

        assertTrue(repository.observeNotifications(userId).first().all { it.isRead })
    }

    @Test
    fun signInWithDemoCredentialsSucceedsAndWrongPasswordFails() = runTest {
        val repository = FakeAuthRepository()
        assertTrue(repository.signIn(SampleData.demoUser.email!!, "wrong").isFailure)
        assertNull(repository.currentUser.value)

        val user = repository.signIn(SampleData.demoUser.email!!, SampleData.DEMO_PASSWORD).getOrThrow()

        assertEquals(SampleData.demoUser, user)
        assertEquals(user, repository.currentUser.value)
    }

    @Test
    fun signUpRejectsAnExistingEmail() = runTest {
        val result = FakeAuthRepository().signUp(SampleData.demoUser.email!!, "secret123", "Ana", "López", null)
        assertTrue(result.isFailure)
    }

    @Test
    fun signUpCreatesASession() = runTest {
        val repository = FakeAuthRepository()
        val user = repository.signUp("nueva@correo.com", "secret123", "Ana", "López", "6620000000").getOrThrow()

        assertEquals("nueva@correo.com", user.email)
        assertFalse(user.isAnonymous)
        assertEquals(user, repository.currentUser.value)
    }

    @Test
    fun anonymousSignInAndSignOut() = runTest {
        val repository = FakeAuthRepository()
        val user = repository.signInAnonymously().getOrThrow()
        assertTrue(user.isAnonymous)
        assertNotNull(repository.currentUser.value)

        repository.signOut()

        assertNull(repository.currentUser.value)
    }

    @Test
    fun contactRequestsAreSplitIntoSentAndReceived() = runTest {
        val repository = FakeContactRequestRepository()
        val userId = SampleData.demoUser.id
        assertEquals(listOf("request-1"), repository.observeSent(userId).first().map { it.id })
        assertEquals(listOf("request-2"), repository.observeReceived(userId).first().map { it.id })
    }

    @Test
    fun markAsReadOnlyChangesThatNotification() = runTest {
        val repository = FakeNotificationRepository()
        val userId = SampleData.demoUser.id

        repository.markAsRead("notification-2")

        val byId = repository.observeNotifications(userId).first().associateBy { it.id }
        assertTrue(byId.getValue("notification-2").isRead)
        assertFalse(byId.getValue("notification-3").isRead)
    }

    @Test
    fun signInIgnoresEmailCaseAndSpaces() = runTest {
        val user = FakeAuthRepository().signIn("  DEMO@Correo.com ", SampleData.DEMO_PASSWORD).getOrThrow()
        assertEquals(SampleData.demoUser, user)
    }
}
