package com.example.fluffypawtrack.data.fake

import com.example.fluffypawtrack.domain.model.AdoptionListing
import com.example.fluffypawtrack.domain.model.AppNotification
import com.example.fluffypawtrack.domain.model.ContactDetails
import com.example.fluffypawtrack.domain.model.ContactRecipient
import com.example.fluffypawtrack.domain.model.ContactRequest
import com.example.fluffypawtrack.domain.model.ContactRequestRules
import com.example.fluffypawtrack.domain.model.ContactRequestStatus
import com.example.fluffypawtrack.domain.model.InvalidAuthDataException
import com.example.fluffypawtrack.domain.model.ListingRules
import com.example.fluffypawtrack.domain.model.ListingStatus
import com.example.fluffypawtrack.domain.model.NewListing
import com.example.fluffypawtrack.domain.model.NewReport
import com.example.fluffypawtrack.domain.model.NotificationType
import com.example.fluffypawtrack.domain.model.Organization
import com.example.fluffypawtrack.domain.model.OrganizationDraft
import com.example.fluffypawtrack.domain.model.OrganizationProfile
import com.example.fluffypawtrack.domain.model.OrganizationRules
import com.example.fluffypawtrack.domain.model.OrganizationStatus
import com.example.fluffypawtrack.domain.model.OrganizationType
import com.example.fluffypawtrack.domain.model.Report
import com.example.fluffypawtrack.domain.model.ReportCategory
import com.example.fluffypawtrack.domain.model.ReportStatus
import com.example.fluffypawtrack.domain.model.ReportStatusRules
import com.example.fluffypawtrack.domain.model.StatusHistoryEntry
import com.example.fluffypawtrack.domain.model.contactRecipient
import com.example.fluffypawtrack.domain.model.isActive
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
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// In-memory implementations backed by SampleData. State lives only while the app runs.

/**
 * Accounts of [SampleData.users] plus [extraUsers], all with [SampleData.DEMO_PASSWORD]. It also
 * stands in for the `profiles` table of the other fakes ([findUser], [updateUsers]).
 */
class FakeAuthRepository(
    initialUser: UserProfile? = null,
    extraUsers: List<UserProfile> = emptyList(),
) : AuthRepository {
    private val accounts = (SampleData.users + extraUsers)
        .mapNotNull { user -> user.email?.let { it to (user to SampleData.DEMO_PASSWORD) } }
        .toMap()
        .toMutableMap()
    private val session = MutableStateFlow(initialUser)
    private var nextId = 1

    override val currentUser: StateFlow<UserProfile?> = session.asStateFlow()

    /** The profile of [id]: a registered account, or the current (possibly anonymous) session. */
    internal fun findUser(id: String): UserProfile? =
        session.value?.takeIf { it.id == id } ?: accounts.values.firstOrNull { it.first.id == id }?.first

    /** Server-side profile changes (role promotion, organization removal) applied to every account. */
    internal fun updateUsers(transform: (UserProfile) -> UserProfile) {
        for (key in accounts.keys.toList()) {
            val (user, password) = accounts.getValue(key)
            accounts[key] = transform(user) to password
        }
        session.update { it?.let(transform) }
    }

    override suspend fun signIn(email: String, password: String): Result<UserProfile> {
        val (user, expected) = accounts[email.trim().lowercase()]
            ?: return Result.failure(InvalidAuthDataException())
        if (password != expected) return Result.failure(InvalidAuthDataException())
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
        if (key in accounts) return Result.failure(InvalidAuthDataException())
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

/**
 * Enforces the RF-04 and RF-07 rules of `take_report`, `set_report_status` and
 * `set_report_hidden`. [currentUser] stands in for the session (`auth.uid()`),
 * [findOrganizationName] for the `organizations` table, and [notifications] receives the
 * reporter notifications the server trigger inserts.
 */
@OptIn(ExperimentalTime::class)
class FakeReportRepository(
    private val clock: Clock = Clock.System,
    private val currentUser: () -> UserProfile? = { null },
    private val findOrganizationName: (String) -> String? = { id ->
        SampleData.organizations.firstOrNull { it.id == id }?.name
    },
    private val notifications: NotificationRepository? = null,
) : ReportRepository {
    private val reports = MutableStateFlow(SampleData.reports)
    private val history = MutableStateFlow(SampleData.reports.associate { it.id to sampleHistory(it) })
    private val changeListeners = mutableListOf<suspend (before: Report, after: Report) -> Unit>()
    private var nextId = 1

    /** One row of `report_status_history`; [changedBy] is a user id, null for seed data. */
    private data class HistoryRow(val status: ReportStatus, val at: Instant, val changedBy: String?)

    override fun observeActiveReports(): Flow<List<Report>> =
        reports.map { list -> list.filter { it.status != ReportStatus.Closed && !it.hidden } }

    override fun observeInbox(organizationId: String): Flow<List<Report>> = reports.map { list ->
        list.filter {
            it.status != ReportStatus.Closed && !it.hidden &&
                (it.assignedOrganizationId == null || it.assignedOrganizationId == organizationId)
        }.sortedByDescending { it.createdAt }
    }

    override fun observeCasesOf(organizationId: String): Flow<List<Report>> = reports.map { list ->
        // RLS hides hidden reports from affiliates, so a hidden case leaves the list too.
        list.filter { it.assignedOrganizationId == organizationId && it.status != ReportStatus.Closed && !it.hidden }
            .sortedByDescending { it.createdAt }
    }

    override fun observeAllForAdmin(): Flow<List<Report>> =
        reports.map { list -> list.sortedByDescending { it.createdAt } }

    override suspend fun takeReport(reportId: String): Result<Report> {
        val organizationId = currentUser()?.takeIf { it.role == Role.Affiliate }?.organizationId
            ?: return failure("Solo los albergues afiliados pueden tomar casos")
        val report = reports.value.firstOrNull { it.id == reportId }?.takeUnless { it.hidden }
            ?: return failure(NOT_FOUND)
        if (report.status == ReportStatus.Closed) return failure("Este caso ya está cerrado")
        if (report.status != ReportStatus.Pending || report.assignedOrganizationId != null) {
            return failure("Este caso ya fue tomado por otro albergue")
        }
        return Result.success(
            save(report, report.copy(assignedOrganizationId = organizationId, status = ReportStatus.OnTheWay)),
        )
    }

    override suspend fun setStatus(reportId: String, status: ReportStatus): Result<Report> {
        val report = reports.value.firstOrNull { it.id == reportId } ?: return failure(NOT_FOUND)
        val user = currentUser()
        if (user?.role != Role.Admin) {
            val isAssigned = user?.role == Role.Affiliate && user.organizationId != null &&
                user.organizationId == report.assignedOrganizationId
            if (!isAssigned) return failure("Solo el albergue asignado puede cambiar el estado del reporte")
            if (!ReportStatusRules.canAdvance(report.status, status)) {
                return failure("El estado del reporte solo puede avanzar")
            }
        }
        return Result.success(save(report, report.copy(status = status)))
    }

    override suspend fun setHidden(reportId: String, hidden: Boolean): Result<Report> {
        if (currentUser()?.role != Role.Admin) return failure("Solo el administrador puede ocultar reportes")
        val report = reports.value.firstOrNull { it.id == reportId } ?: return failure(NOT_FOUND)
        return Result.success(save(report, report.copy(hidden = hidden)))
    }

    override fun observeStatusHistory(reportId: String): Flow<List<StatusHistoryEntry>> =
        history.map { all ->
            val report = reports.value.firstOrNull { it.id == reportId }
            val organizationName = report?.assignedOrganizationId?.let(findOrganizationName)
            all[reportId].orEmpty().sortedBy { it.at }.map { row ->
                StatusHistoryEntry(
                    status = row.status,
                    changedAt = row.at,
                    responsibleName = ReportStatusRules.responsibleName(
                        row.status, row.changedBy, report?.reporterId, organizationName,
                    ),
                )
            }
        }

    /**
     * Lets [FakeContactRequestRepository] reject pending requests when a report is closed or
     * hidden, like the `reports_reject_on_close` and `reports_release_on_hide` triggers.
     */
    internal fun addChangeListener(listener: suspend (before: Report, after: Report) -> Unit) {
        changeListeners += listener
    }

    /** Stores [updated], records a status change in the history and notifies the reporter. */
    private suspend fun save(before: Report, updated: Report): Report {
        reports.update { list -> list.map { if (it.id == updated.id) updated else it } }
        if (updated.status != before.status) {
            val changedBy = currentUser()?.id
            history.update { all ->
                all + (updated.id to all[updated.id].orEmpty() + HistoryRow(updated.status, clock.now(), changedBy))
            }
            val reporterId = updated.reporterId
            if (reporterId != null && reporterId != changedBy) {
                notifications?.notify(
                    reporterId,
                    ReportStatusRules.STATUS_CHANGED_TITLE,
                    ReportStatusRules.statusChangedMessage(updated.status),
                    NotificationType.Report,
                )
            }
        }
        changeListeners.forEach { it(before, updated) }
        return updated
    }

    private fun <T> failure(message: String): Result<T> = Result.failure(IllegalStateException(message))

    private companion object {
        const val NOT_FOUND = "El reporte no existe"

        /** Every status up to the current one, an hour apart, the first one by the reporter. */
        fun sampleHistory(report: Report): List<HistoryRow> =
            ReportStatus.entries.filter { it <= report.status }.mapIndexed { index, status ->
                HistoryRow(
                    status = status,
                    at = report.createdAt + index.hours,
                    changedBy = report.reporterId.takeIf { status == ReportStatus.Pending },
                )
            }
    }

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
        history.update { all -> all + (created.id to listOf(HistoryRow(created.status, created.createdAt, reporterId))) }
        return Result.success(created)
    }

    override suspend fun deleteReport(id: String): Result<Unit> {
        if (reports.value.none { it.id == id }) {
            return Result.failure(NoSuchElementException("El reporte no existe"))
        }
        reports.update { list -> list.filterNot { it.id == id } }
        return Result.success(Unit)
    }

    override suspend fun updateReport(id: String, description: String, reference: String?): Result<Report> {
        val current = reports.value.firstOrNull { it.id == id }
            ?: return Result.failure(NoSuchElementException("El reporte no existe"))
        if (current.status != ReportStatus.Pending) {
            return Result.failure(IllegalStateException("Solo puedes editar reportes pendientes de atención"))
        }
        val updated = current.copy(description = description, reference = reference)
        reports.update { list -> list.map { if (it.id == id) updated else it } }
        return Result.success(updated)
    }

    override suspend fun closeReport(id: String): Result<Report> {
        val current = reports.value.firstOrNull { it.id == id }
            ?: return Result.failure(NoSuchElementException("El reporte no existe"))
        return Result.success(save(current, current.copy(status = ReportStatus.Closed)))
    }
}

/**
 * Enforces the RF-07 rules of `review_affiliation`, the admin policies and the affiliate's
 * profile guard. [currentUser] stands in for the session, [updateUsers] for the profile changes
 * the server makes (promotion on approval, demotion on delete), and [notifications] receives
 * the requester's notification.
 */
class FakeOrganizationRepository(
    private val currentUser: () -> UserProfile? = { null },
    private val updateUsers: ((UserProfile) -> UserProfile) -> Unit = {},
    private val notifications: NotificationRepository? = null,
) : OrganizationRepository {
    private val organizations = MutableStateFlow(SampleData.organizations)

    /** `organizations.requested_by`: who asked for each affiliation. */
    private val requesters = SampleData.affiliationRequesters.toMutableMap()

    override fun observeActiveOrganizations(): Flow<List<Organization>> =
        organizations.map { list -> list.filter { it.status == OrganizationStatus.Approved && it.active } }

    override suspend fun getOrganization(id: String): Organization? = findOrganization(id)

    /** Non-suspending lookup for the other fakes (stands in for a join on `organizations`). */
    internal fun findOrganization(id: String): Organization? = organizations.value.firstOrNull { it.id == id }

    override fun observePendingAffiliations(): Flow<List<Organization>> =
        organizations.map { list -> list.filter { it.status == OrganizationStatus.Pending } }

    override suspend fun reviewAffiliation(organizationId: String, approve: Boolean): Result<Organization> {
        if (!isAdmin()) return failure("Solo el administrador puede revisar afiliaciones")
        val organization = getOrganization(organizationId) ?: return failure(NOT_FOUND)
        if (organization.status != OrganizationStatus.Pending) {
            return failure("Esta solicitud de afiliación ya fue revisada")
        }
        val reviewed = save(
            organization.copy(status = if (approve) OrganizationStatus.Approved else OrganizationStatus.Rejected),
        )
        val requesterId = requesters[organizationId] ?: return Result.success(reviewed)
        if (approve) {
            updateUsers { user ->
                if (user.id == requesterId && user.role == Role.User) {
                    user.copy(role = Role.Affiliate, organizationId = organizationId)
                } else {
                    user
                }
            }
        }
        notifications?.notify(
            requesterId,
            if (approve) "Afiliación aprobada" else "Afiliación no aprobada",
            "Tu solicitud de afiliación para ${organization.name} ${if (approve) "fue aprobada" else "no fue aprobada"}.",
            NotificationType.General,
        )
        return Result.success(reviewed)
    }

    override fun observeAllForAdmin(): Flow<List<Organization>> =
        organizations.map { list -> list.sortedBy { it.name } }

    override suspend fun createOrganization(draft: OrganizationDraft): Result<Organization> {
        if (!isAdmin()) return failure(ADMIN_ONLY)
        OrganizationRules.problemOf(draft)?.let { return failure(it) }
        val created = draft.toOrganization(id = "org-admin-${nextAffiliationId++}")
            .copy(status = OrganizationStatus.Approved, active = true)
        organizations.update { it + created }
        return Result.success(created)
    }

    override suspend fun updateOrganization(organizationId: String, draft: OrganizationDraft): Result<Organization> {
        if (!isAdmin()) return failure(ADMIN_ONLY)
        val current = getOrganization(organizationId) ?: return failure(NOT_FOUND)
        OrganizationRules.problemOf(draft)?.let { return failure(it) }
        return Result.success(save(draft.toOrganization(organizationId).copy(status = current.status, active = current.active)))
    }

    override suspend fun setActive(organizationId: String, active: Boolean): Result<Organization> {
        if (!isAdmin()) return failure(ADMIN_ONLY)
        val current = getOrganization(organizationId) ?: return failure(NOT_FOUND)
        return Result.success(save(current.copy(active = active)))
    }

    override suspend fun deleteOrganization(organizationId: String): Result<Unit> {
        if (!isAdmin()) return failure(ADMIN_ONLY)
        if (getOrganization(organizationId) == null) return failure(NOT_FOUND)
        organizations.update { list -> list.filterNot { it.id == organizationId } }
        requesters.remove(organizationId)
        updateUsers { user ->
            if (user.role == Role.Affiliate && user.organizationId == organizationId) {
                user.copy(role = Role.User, organizationId = null)
            } else {
                user
            }
        }
        return Result.success(Unit)
    }

    override suspend fun updateOwnOrganization(
        organizationId: String,
        profile: OrganizationProfile,
    ): Result<Organization> {
        val user = currentUser()
        val current = getOrganization(organizationId)
        if (current == null || user?.role != Role.Affiliate || user.organizationId != organizationId) {
            return failure("Solo los afiliados de la organización pueden editar su perfil")
        }
        OrganizationRules.problemOf(profile)?.let { return failure(it) }
        return Result.success(
            save(
                current.copy(
                    description = profile.description.trim(),
                    schedule = profile.schedule.trim(),
                    phone = profile.phone.trim(),
                    email = profile.email.trim(),
                    social = profile.social?.trim()?.takeIf { it.isNotEmpty() },
                    photoUrl = profile.photoUrl,
                ),
            ),
        )
    }

    private fun isAdmin(): Boolean = currentUser()?.role == Role.Admin

    private fun save(updated: Organization): Organization {
        organizations.update { list -> list.map { if (it.id == updated.id) updated else it } }
        return updated
    }

    private fun OrganizationDraft.toOrganization(id: String): Organization = Organization(
        id = id,
        type = type,
        name = name.trim(),
        description = description.trim(),
        location = location,
        address = address.trim(),
        schedule = schedule.trim(),
        phone = phone.trim(),
        email = email.trim(),
        social = social?.trim()?.takeIf { it.isNotEmpty() },
        photoUrl = photoUrl,
        status = OrganizationStatus.Approved,
        active = true,
        capacity = capacity,
    )

    private fun <T> failure(message: String): Result<T> = Result.failure(IllegalStateException(message))

    private companion object {
        const val NOT_FOUND = "La organización no existe"
        const val ADMIN_ONLY = "Solo el administrador puede administrar el directorio"
    }

    private var nextAffiliationId = 1

    override suspend fun requestAffiliation(
        type: OrganizationType,
        name: String,
        address: String,
        phone: String,
        email: String,
        description: String,
        photoUrl: String,
    ): Result<Organization> {
        if (listOf(name, address, phone, email, description).any { it.isBlank() }) {
            return Result.failure(IllegalArgumentException("Completa todos los datos de la organización"))
        }
        if (photoUrl.isBlank()) {
            return Result.failure(IllegalArgumentException("Agrega una foto del lugar o su logo"))
        }
        val requested = Organization(
            id = "org-new-${nextAffiliationId++}",
            type = type,
            name = name.trim(),
            description = description.trim(),
            // The form has no map picker; the admin sets the exact spot when approving.
            location = SampleData.HERMOSILLO_CENTER,
            address = address.trim(),
            schedule = "Por confirmar",
            phone = phone.trim(),
            email = email.trim().lowercase(),
            social = null,
            photoUrl = photoUrl,
            status = OrganizationStatus.Pending,
            active = true,
        )
        organizations.update { it + requested }
        currentUser()?.let { requesters[requested.id] = it.id }
        return Result.success(requested)
    }
}

/**
 * Rejects listings without photos: every animal in adoption needs at least one.
 * [findUser] stands in for the `profiles` table when checking who marks an animal as adopted.
 */
class FakeAdoptionRepository(
    initialListings: List<AdoptionListing> = SampleData.adoptionListings,
    private val findUser: (String) -> UserProfile? = { id -> SampleData.users.firstOrNull { it.id == id } },
    /** The session, for the listing editor (`adoption_listings_*_center` policies). */
    private val currentUser: () -> UserProfile? = { null },
    private val findOrganization: (String) -> Organization? = { id -> SampleData.organizations.firstOrNull { it.id == id } },
) : AdoptionRepository {
    private var nextId = 1

    override suspend fun createListing(listing: NewListing): Result<AdoptionListing> {
        val user = currentUser()
        val organization = user?.takeIf { it.role == Role.Affiliate }?.organizationId?.let(findOrganization)
        if (organization?.type != OrganizationType.AdoptionCenter || organization.status != OrganizationStatus.Approved) {
            return Result.failure(IllegalStateException("Solo los centros de adopción afiliados pueden publicar animales"))
        }
        ListingRules.problemOf(listing)?.let { return Result.failure(IllegalStateException(it)) }
        val created = listing.toListing(id = "listing-new-${nextId++}", organizationId = organization.id)
        listings.update { it + created }
        return Result.success(created)
    }

    override suspend fun updateListing(listingId: String, listing: NewListing): Result<AdoptionListing> {
        val current = getListing(listingId) ?: return Result.failure(IllegalStateException("El animal no existe"))
        val user = currentUser()
        if (user?.role != Role.Affiliate || user.organizationId != current.organizationId) {
            return Result.failure(IllegalStateException("Solo el centro de este animal puede editarlo"))
        }
        ListingRules.problemOf(listing)?.let { return Result.failure(IllegalStateException(it)) }
        val updated = listing.toListing(id = listingId, organizationId = current.organizationId)
            .copy(status = current.status)
        listings.update { list -> list.map { if (it.id == listingId) updated else it } }
        return Result.success(updated)
    }

    private fun NewListing.toListing(id: String, organizationId: String): AdoptionListing = AdoptionListing(
        id = id,
        organizationId = organizationId,
        name = name.trim(),
        species = species.trim(),
        breed = breed.trim(),
        age = age.trim(),
        vaccination = vaccination.trim(),
        description = description.trim(),
        photoUrls = photoUrls.filter { it.isNotBlank() },
        status = ListingStatus.Available,
    )

    init {
        require(initialListings.all { it.photoUrls.isNotEmpty() }) { "Every adoption listing needs at least one photo" }
    }

    private val listings = MutableStateFlow(initialListings)
    private val adoptedListeners = mutableListOf<suspend (AdoptionListing) -> Unit>()

    override fun observeAvailableListings(): Flow<List<AdoptionListing>> =
        listings.map { list -> list.filter { it.status == ListingStatus.Available } }

    override fun observeListingsBy(organizationId: String): Flow<List<AdoptionListing>> =
        listings.map { list -> list.filter { it.organizationId == organizationId } }

    override suspend fun getListing(id: String): AdoptionListing? = listings.value.firstOrNull { it.id == id }

    override suspend fun markAdopted(listingId: String, affiliateUserId: String): Result<AdoptionListing> {
        val listing = getListing(listingId)
            ?: return Result.failure(IllegalStateException("El animal no existe"))
        val affiliate = findUser(affiliateUserId)
        if (affiliate?.role != Role.Affiliate || affiliate.organizationId != listing.organizationId) {
            return Result.failure(IllegalStateException("Solo el centro de este animal puede marcarlo como adoptado"))
        }
        if (listing.status == ListingStatus.Adopted) {
            return Result.failure(IllegalStateException("Este animal ya fue marcado como adoptado"))
        }
        val adopted = listing.copy(status = ListingStatus.Adopted)
        listings.update { list -> list.map { if (it.id == listingId) adopted else it } }
        adoptedListeners.forEach { it(adopted) }
        return Result.success(adopted)
    }

    /**
     * Lets [FakeAdoptionRequestRepository] reject the pending requests of an adopted animal.
     * Supabase does the same in one transaction (an RPC or trigger on `adoption_listings`).
     */
    internal fun addAdoptedListener(listener: suspend (AdoptionListing) -> Unit) {
        adoptedListeners += listener
    }
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

@OptIn(ExperimentalTime::class)
class FakeNotificationRepository(private val clock: Clock = Clock.System) : NotificationRepository {
    private val notifications = MutableStateFlow(SampleData.notifications)
    private var nextId = 1

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

    override suspend fun notify(
        userId: String,
        title: String,
        message: String,
        type: NotificationType,
    ): AppNotification {
        val created = AppNotification(
            id = "notification-new-${nextId++}",
            title = title,
            message = message,
            receivedAt = clock.now(),
            type = type,
        )
        notifications.update { all -> all + (userId to all[userId].orEmpty() + created) }
        return created
    }
}

/**
 * Enforces the RF-05 rules that Supabase will enforce with RLS and the
 * `get_contact_details` RPC. [findUser] stands in for the `profiles` table.
 */
@OptIn(ExperimentalTime::class)
class FakeContactRequestRepository(
    private val reportRepository: ReportRepository = FakeReportRepository(),
    private val findUser: (String) -> UserProfile? = { id -> SampleData.users.firstOrNull { it.id == id } },
    private val findOrganization: suspend (String) -> Organization? = { id ->
        SampleData.organizations.firstOrNull { it.id == id }
    },
    private val clock: Clock = Clock.System,
) : ContactRequestRepository {
    private val requests = MutableStateFlow(SampleData.contactRequests)
    private var nextId = 1

    init {
        // Server triggers: closing or hiding a report rejects its pending requests.
        (reportRepository as? FakeReportRepository)?.addChangeListener { before, after ->
            when {
                after.status == ReportStatus.Closed && before.status != ReportStatus.Closed ->
                    rejectPending(after.id, ContactRequestRules.CASE_CLOSED_NOTE)
                after.hidden && !before.hidden -> rejectPending(after.id, ReportStatusRules.REPORT_UNAVAILABLE_NOTE)
            }
        }
    }

    private fun rejectPending(reportId: String, note: String) {
        requests.update { list ->
            list.map {
                if (it.reportId == reportId && it.status == ContactRequestStatus.Pending) {
                    it.copy(status = ContactRequestStatus.Rejected, statusNote = note)
                } else {
                    it
                }
            }
        }
    }

    override fun observeSent(userId: String): Flow<List<ContactRequest>> =
        requests.map { list -> list.filter { it.requesterId == userId } }

    override fun observeReceived(userId: String): Flow<List<ContactRequest>> =
        requests.map { list -> list.filter { it.recipientUserId == userId } }

    override suspend fun send(
        reportId: String,
        requesterId: String,
        message: String,
        photoUrl: String?,
    ): Result<ContactRequest> {
        val requester = findUser(requesterId)?.takeUnless { it.isAnonymous }
            ?: return failure("Necesitas una cuenta para enviar solicitudes de contacto")
        if (requester.role != Role.User) return failure("Solo los usuarios pueden enviar solicitudes de contacto")
        val report = reportRepository.getReport(reportId) ?: return failure("El reporte ya no está disponible")
        if (report.category != ReportCategory.PossiblyLost) {
            return failure("Solo puedes enviar solicitudes en reportes de \"Posible mascota extraviada\"")
        }
        if (report.status == ReportStatus.Closed) return failure("Este caso ya está cerrado")
        if (report.reporterId == requester.id) return failure("No puedes enviar una solicitud a tu propio reporte")
        val recipient = report.contactRecipient ?: return failure(ContactRequestRules.ANONYMOUS_REPORT_NOTICE)
        val previous = requests.value.filter { it.reportId == reportId && it.requesterId == requester.id }
        if (previous.any { it.status.isActive }) return failure("Ya tienes una solicitud activa en este reporte")
        if (previous.any { it.status == ContactRequestStatus.Rejected }) {
            return failure("Tu solicitud en este reporte ya fue rechazada")
        }
        if (requester.phone.isNullOrBlank()) return failure(MISSING_PHONE)
        if (!ContactRequestRules.isValidMessage(message)) {
            return failure(
                "El mensaje debe tener entre ${ContactRequestRules.MIN_MESSAGE_LENGTH} " +
                    "y ${ContactRequestRules.MAX_MESSAGE_LENGTH} caracteres",
            )
        }
        val created = ContactRequest(
            id = "request-new-${nextId++}",
            reportId = reportId,
            requesterId = requester.id,
            requesterFirstName = requester.firstName,
            recipientUserId = (recipient as? ContactRecipient.User)?.userId,
            recipientOrganizationId = (recipient as? ContactRecipient.Organization)?.organizationId,
            message = message.trim(),
            photoUrl = photoUrl,
            status = ContactRequestStatus.Pending,
            statusNote = null,
            createdAt = clock.now(),
        )
        requests.update { it + created }
        return Result.success(created)
    }

    override suspend fun cancel(requestId: String, userId: String): Result<ContactRequest> {
        val request = find(requestId) ?: return failure(NOT_FOUND)
        if (request.requesterId != userId) return failure("Solo quien envió la solicitud puede cancelarla")
        if (request.status != ContactRequestStatus.Pending) return failure("Solo puedes cancelar solicitudes pendientes")
        return Result.success(save(request.copy(status = ContactRequestStatus.Cancelled)))
    }

    override suspend fun accept(requestId: String, userId: String): Result<ContactRequest> {
        val request = find(requestId) ?: return failure(NOT_FOUND)
        if (!isRecipient(request, userId)) return failure(NOT_RECIPIENT)
        if (request.status != ContactRequestStatus.Pending) return failure(ALREADY_ANSWERED)
        if (findUser(userId)?.phone.isNullOrBlank()) return failure(MISSING_PHONE)
        return Result.success(save(request.copy(status = ContactRequestStatus.Accepted)))
    }

    override suspend fun reject(requestId: String, userId: String): Result<ContactRequest> {
        val request = find(requestId) ?: return failure(NOT_FOUND)
        if (!isRecipient(request, userId)) return failure(NOT_RECIPIENT)
        if (request.status != ContactRequestStatus.Pending) return failure(ALREADY_ANSWERED)
        return Result.success(save(request.copy(status = ContactRequestStatus.Rejected)))
    }

    override suspend fun markDelivered(requestId: String, userId: String): Result<ContactRequest> {
        val request = find(requestId) ?: return failure(NOT_FOUND)
        if (!isRecipient(request, userId)) return failure(NOT_RECIPIENT)
        if (request.status != ContactRequestStatus.Accepted) {
            return failure("Solo puedes marcar como entregada una solicitud aceptada")
        }
        val report = reportRepository.getReport(request.reportId) ?: return failure("El reporte ya no está disponible")
        if (report.status == ReportStatus.Closed) return failure("Este caso ya está cerrado")
        reportRepository.closeReport(report.id).onFailure { return Result.failure(it) }
        // Also done by the change listener when the report repository is the fake one.
        rejectPending(report.id, ContactRequestRules.CASE_CLOSED_NOTE)
        return Result.success(request)
    }

    override suspend fun getContactDetails(requestId: String, userId: String): ContactDetails? {
        val request = find(requestId)?.takeIf { it.status == ContactRequestStatus.Accepted } ?: return null
        return when {
            request.requesterId == userId -> recipientDetails(request)
            isRecipient(request, userId) -> findUser(request.requesterId)?.toContactDetails()
            else -> null
        }
    }

    private suspend fun recipientDetails(request: ContactRequest): ContactDetails? {
        request.recipientOrganizationId?.let { orgId ->
            val organization = findOrganization(orgId) ?: return null
            return ContactDetails(organization.name, organization.phone, organization.email)
        }
        return request.recipientUserId?.let(findUser)?.toContactDetails()
    }

    private fun UserProfile.toContactDetails(): ContactDetails =
        ContactDetails("$firstName $lastName".trim(), phone.orEmpty(), email.orEmpty())

    /** The reporter, or any affiliate account of the recipient organization. */
    private fun isRecipient(request: ContactRequest, userId: String): Boolean {
        if (request.recipientUserId != null) return request.recipientUserId == userId
        val user = findUser(userId) ?: return false
        return user.role == Role.Affiliate && user.organizationId == request.recipientOrganizationId
    }

    private fun find(requestId: String): ContactRequest? = requests.value.firstOrNull { it.id == requestId }

    private fun save(updated: ContactRequest): ContactRequest {
        requests.update { list -> list.map { if (it.id == updated.id) updated else it } }
        return updated
    }

    private fun <T> failure(message: String): Result<T> = Result.failure(IllegalStateException(message))

    private companion object {
        const val NOT_FOUND = "La solicitud no existe"
        const val NOT_RECIPIENT = "Solo el receptor puede responder esta solicitud"
        const val ALREADY_ANSWERED = "Esta solicitud ya fue respondida"
        const val MISSING_PHONE = "Agrega un teléfono a tu perfil antes de continuar"
    }
}
