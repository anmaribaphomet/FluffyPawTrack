package com.example.fluffypawtrack.data.fake

import com.example.fluffypawtrack.domain.model.AdoptionListing
import com.example.fluffypawtrack.domain.model.AppNotification
import com.example.fluffypawtrack.domain.model.ContactRequest
import com.example.fluffypawtrack.domain.model.ListingStatus
import com.example.fluffypawtrack.domain.model.NewReport
import com.example.fluffypawtrack.domain.model.Organization
import com.example.fluffypawtrack.domain.model.OrganizationStatus
import com.example.fluffypawtrack.domain.model.Report
import com.example.fluffypawtrack.domain.model.ReportStatus
import com.example.fluffypawtrack.domain.model.Role
import com.example.fluffypawtrack.domain.model.UserProfile
import com.example.fluffypawtrack.domain.repository.AdoptionRepository
import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.domain.repository.ContactRequestRepository
import com.example.fluffypawtrack.domain.repository.FavoritesRepository
import com.example.fluffypawtrack.domain.repository.NotificationRepository
import com.example.fluffypawtrack.domain.repository.OrganizationRepository
import com.example.fluffypawtrack.domain.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

// In-memory implementations backed by SampleData. State lives only while the app runs.

class FakeAuthRepository : AuthRepository {
    private val accounts = SampleData.users
        .mapNotNull { user -> user.email?.let { it to (user to SampleData.DEMO_PASSWORD) } }
        .toMap()
        .toMutableMap()
    private val session = MutableStateFlow<UserProfile?>(null)
    private var nextId = 1

    override val currentUser: StateFlow<UserProfile?> = session.asStateFlow()

    override suspend fun signIn(email: String, password: String): Result<UserProfile> {
        val (user, expected) = accounts[email.trim().lowercase()]
            ?: return Result.failure(IllegalArgumentException("Correo o contraseña incorrectos"))
        if (password != expected) return Result.failure(IllegalArgumentException("Correo o contraseña incorrectos"))
        session.value = user
        return Result.success(user)
    }

    override suspend fun signUp(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phone: String?,
    ): Result<UserProfile> {
        val key = email.trim().lowercase()
        if (key in accounts) return Result.failure(IllegalArgumentException("Ya existe una cuenta con ese correo"))
        val user = UserProfile(
            id = "user-new-${nextId++}",
            email = key,
            firstName = firstName,
            lastName = lastName,
            phone = phone,
            role = Role.User,
            organizationId = null,
            isAnonymous = false,
        )
        accounts[key] = user to password
        session.value = user
        return Result.success(user)
    }

    override suspend fun signInAnonymously(): Result<UserProfile> {
        val user = UserProfile(
            id = "anonymous-${nextId++}",
            email = null,
            firstName = "",
            lastName = "",
            phone = null,
            role = Role.User,
            organizationId = null,
            isAnonymous = true,
        )
        session.value = user
        return Result.success(user)
    }

    override suspend fun signOut() {
        session.value = null
    }
}

@OptIn(ExperimentalTime::class)
class FakeReportRepository(private val clock: Clock = Clock.System) : ReportRepository {
    private val reports = MutableStateFlow(SampleData.reports)
    private var nextId = 1

    override fun observeActiveReports(): Flow<List<Report>> =
        reports.map { list -> list.filter { it.status != ReportStatus.Closed } }

    override fun observeReportsBy(reporterId: String): Flow<List<Report>> =
        reports.map { list -> list.filter { it.reporterId == reporterId } }

    override suspend fun getReport(id: String): Report? = reports.value.firstOrNull { it.id == id }

    override suspend fun createReport(reporterId: String?, report: NewReport): Result<Report> {
        val created = Report(
            id = "report-new-${nextId++}",
            reporterId = reporterId,
            category = report.category,
            priority = report.priority,
            description = report.description,
            photoUrl = report.photoUrl,
            location = report.location,
            address = report.address,
            reference = report.reference,
            status = ReportStatus.Pending,
            assignedOrganizationId = null,
            createdAt = clock.now(),
        )
        reports.update { it + created }
        return Result.success(created)
    }

    override suspend fun deleteReport(id: String): Result<Unit> {
        if (reports.value.none { it.id == id }) {
            return Result.failure(NoSuchElementException("El reporte no existe"))
        }
        reports.update { list -> list.filterNot { it.id == id } }
        return Result.success(Unit)
    }
}

class FakeOrganizationRepository : OrganizationRepository {
    private val organizations = MutableStateFlow(SampleData.organizations)

    override fun observeActiveOrganizations(): Flow<List<Organization>> =
        organizations.map { list -> list.filter { it.status == OrganizationStatus.Approved && it.active } }

    override suspend fun getOrganization(id: String): Organization? =
        organizations.value.firstOrNull { it.id == id }
}

class FakeAdoptionRepository : AdoptionRepository {
    private val listings = MutableStateFlow(SampleData.adoptionListings)

    override fun observeAvailableListings(): Flow<List<AdoptionListing>> =
        listings.map { list -> list.filter { it.status == ListingStatus.Available } }

    override fun observeListingsBy(organizationId: String): Flow<List<AdoptionListing>> =
        listings.map { list -> list.filter { it.organizationId == organizationId } }

    override suspend fun getListing(id: String): AdoptionListing? = listings.value.firstOrNull { it.id == id }
}

class FakeFavoritesRepository : FavoritesRepository {
    private val favorites = MutableStateFlow<Map<String, Set<String>>>(emptyMap())

    override fun observeFavoriteIds(userId: String): Flow<Set<String>> =
        favorites.map { it[userId].orEmpty() }

    override suspend fun toggleFavorite(userId: String, listingId: String) {
        favorites.update { all ->
            val current = all[userId].orEmpty()
            val updated = if (listingId in current) current - listingId else current + listingId
            all + (userId to updated)
        }
    }
}

class FakeNotificationRepository : NotificationRepository {
    private val notifications = MutableStateFlow(SampleData.notifications)

    override fun observeNotifications(userId: String): Flow<List<AppNotification>> =
        notifications.map { it[userId].orEmpty() }

    override suspend fun markAsRead(notificationId: String) {
        notifications.update { all ->
            all.mapValues { (_, list) -> list.map { if (it.id == notificationId) it.copy(isRead = true) else it } }
        }
    }

    override suspend fun markAllAsRead(userId: String) {
        notifications.update { all ->
            all + (userId to all[userId].orEmpty().map { it.copy(isRead = true) })
        }
    }
}

class FakeContactRequestRepository : ContactRequestRepository {
    private val requests = MutableStateFlow(SampleData.contactRequests)

    override fun observeSent(userId: String): Flow<List<ContactRequest>> =
        requests.map { list -> list.filter { it.requesterId == userId } }

    override fun observeReceived(userId: String): Flow<List<ContactRequest>> =
        requests.map { list -> list.filter { it.recipientUserId == userId } }
}
