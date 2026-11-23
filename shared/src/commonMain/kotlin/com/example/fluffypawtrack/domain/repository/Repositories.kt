package com.example.fluffypawtrack.domain.repository

import com.example.fluffypawtrack.domain.model.AdoptionListing
import com.example.fluffypawtrack.domain.model.AdoptionRequest
import com.example.fluffypawtrack.domain.model.AppNotification
import com.example.fluffypawtrack.domain.model.ContactDetails
import com.example.fluffypawtrack.domain.model.ContactRequest
import com.example.fluffypawtrack.domain.model.NewAdoptionRequest
import com.example.fluffypawtrack.domain.model.NewListing
import com.example.fluffypawtrack.domain.model.NotificationPrefs
import com.example.fluffypawtrack.domain.model.NotificationType
import com.example.fluffypawtrack.domain.model.Priority
import com.example.fluffypawtrack.domain.model.NewReport
import com.example.fluffypawtrack.domain.model.Organization
import com.example.fluffypawtrack.domain.model.OrganizationDraft
import com.example.fluffypawtrack.domain.model.OrganizationProfile
import com.example.fluffypawtrack.domain.model.OrganizationType
import com.example.fluffypawtrack.domain.model.Report
import com.example.fluffypawtrack.domain.model.ReportStatus
import com.example.fluffypawtrack.domain.model.StatusHistoryEntry
import com.example.fluffypawtrack.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

// Each interface has a fake (data/fake) and a Supabase (data/supabase) implementation.

interface AuthRepository {
    /** The signed-in user, or null when there is no session. */
    val currentUser: StateFlow<UserProfile?>

    suspend fun signIn(email: String, password: String): Result<UserProfile>

    suspend fun signUp(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phone: String?,
    ): Result<UserProfile>

    /** Session without an account, used only to submit reports. */
    suspend fun signInAnonymously(): Result<UserProfile>

    suspend fun signOut()
}

interface ReportRepository {
    /** Reports visible on the map (not closed). */
    fun observeActiveReports(): Flow<List<Report>>

    fun observeReportsBy(reporterId: String): Flow<List<Report>>

    suspend fun getReport(id: String): Report?

    suspend fun createReport(reporterId: String?, report: NewReport): Result<Report>

    suspend fun deleteReport(id: String): Result<Unit>

    /** Edits the reporter's text fields. Fails for unknown reports or ones no longer pending. */
    suspend fun updateReport(id: String, description: String, reference: String?): Result<Report>

    /** Moves the report to "Caso cerrado", for example when the lost pet was delivered. */
    suspend fun closeReport(id: String): Result<Report>

    /**
     * Affiliate inbox: open (not closed), non-hidden reports that no other organization took,
     * newest first. Radius filtering (RF-03) arrives in Phase 4; filter by [Report.priority].
     */
    fun observeInbox(organizationId: String): Flow<List<Report>>

    /** Non-hidden cases assigned to the organization that are not closed yet, newest first. */
    fun observeCasesOf(organizationId: String): Flow<List<Report>>

    /** Every report, hidden and closed ones included, newest first (RF-07 supervision). */
    fun observeAllForAdmin(): Flow<List<Report>>

    /**
     * The signed-in affiliate's organization takes a pending case (`take_report`); it becomes
     * "En camino". Only one organization can take it.
     */
    suspend fun takeReport(reportId: String): Result<Report>

    /**
     * The assigned organization moves the case forward only; the admin may set any status
     * (`set_report_status`). The reporter is notified of each change.
     */
    suspend fun setStatus(reportId: String, status: ReportStatus): Result<Report>

    /** Admin only (`set_report_hidden`). Hiding rejects the report's pending contact requests. */
    suspend fun setHidden(reportId: String, hidden: Boolean): Result<Report>

    /** RF-04 history, oldest first: every status with its date and responsible. */
    fun observeStatusHistory(reportId: String): Flow<List<StatusHistoryEntry>>
}

interface OrganizationRepository {
    /** Approved and active places, shown permanently on the map. */
    fun observeActiveOrganizations(): Flow<List<Organization>>

    suspend fun getOrganization(id: String): Organization?

    /**
     * A shelter, rescuer or adoption center asks to join. The organization is created as
     * pending, so it stays off the map until the admin approves it.
     */
    suspend fun requestAffiliation(
        type: OrganizationType,
        name: String,
        address: String,
        phone: String,
        email: String,
        description: String,
        /** Required photo of the place or its logo. */
        photoUrl: String,
    ): Result<Organization>

    /** Admin: affiliation requests waiting for review, oldest first. */
    fun observePendingAffiliations(): Flow<List<Organization>>

    /**
     * Admin approves (the requester becomes an affiliate of it and it appears on the map) or
     * rejects (it stays off the map) a pending affiliation (`review_affiliation`). Rejecting
     * needs a [reason] of at most 500 characters; it is stored and sent to the requester in
     * the notification. Approving ignores [reason].
     */
    suspend fun reviewAffiliation(organizationId: String, approve: Boolean, reason: String? = null): Result<Organization>

    /** Admin directory: every organization, pending, rejected and inactive ones included, by name. */
    fun observeAllForAdmin(): Flow<List<Organization>>

    /** Admin adds a place; it is approved and active, so it appears on the map right away. */
    suspend fun createOrganization(draft: OrganizationDraft): Result<Organization>

    /** Admin edits a place; its status and visibility do not change. */
    suspend fun updateOrganization(organizationId: String, draft: OrganizationDraft): Result<Organization>

    /** Admin shows or hides a place on the map. */
    suspend fun setActive(organizationId: String, active: Boolean): Result<Organization>

    /** Admin deletes a place; its affiliates go back to the `user` role. */
    suspend fun deleteOrganization(organizationId: String): Result<Unit>

    /** An affiliate edits the profile of their own organization. */
    suspend fun updateOwnOrganization(organizationId: String, profile: OrganizationProfile): Result<Organization>
}

/** Every listing it returns has at least one photo. */
interface AdoptionRepository {
    fun observeAvailableListings(): Flow<List<AdoptionListing>>

    fun observeListingsBy(organizationId: String): Flow<List<AdoptionListing>>

    suspend fun getListing(id: String): AdoptionListing?

    /**
     * An affiliate of the listing's center marks the animal as adopted. Its pending adoption
     * requests become rejected with "El animal ya fue adoptado" and their requesters are notified.
     */
    suspend fun markAdopted(listingId: String, affiliateUserId: String): Result<AdoptionListing>

    /**
     * The signed-in affiliate publishes an available animal for their adoption center. Needs
     * every field and at least one photo; local photos go to `adoption-photos/<organization>/`.
     */
    suspend fun createListing(listing: NewListing): Result<AdoptionListing>

    /** An affiliate of the listing's center edits it; its status does not change. */
    suspend fun updateListing(listingId: String, listing: NewListing): Result<AdoptionListing>
}

/**
 * RF-06 adoption requests (`adoption_requests` table). Failures carry a readable Spanish message.
 *
 * RLS intent for Phase 2: a user reads, inserts and cancels only their own rows
 * (`requester_id = auth.uid()`, role `user`, not anonymous); affiliates read and answer only the
 * rows of their organization (`organization_id` = their profile's organization). A partial unique
 * index on (listing_id, requester_id) where status in ('pending', 'approved') keeps one active
 * request per animal.
 */
interface AdoptionRequestRepository {
    /** Requires a non-anonymous `user` account and an available listing; at most one active request each. */
    suspend fun submit(requesterId: String?, request: NewAdoptionRequest): Result<AdoptionRequest>

    /** The requester withdraws a pending request. */
    suspend fun cancel(requestId: String, userId: String): Result<AdoptionRequest>

    /** An affiliate of the center approves a pending request and the requester is notified. */
    suspend fun approve(requestId: String, affiliateUserId: String): Result<AdoptionRequest>

    /** An affiliate of the center rejects a pending request with a required [reason]. */
    suspend fun reject(requestId: String, affiliateUserId: String, reason: String): Result<AdoptionRequest>

    fun observeByRequester(userId: String): Flow<List<AdoptionRequest>>

    fun observeByOrganization(organizationId: String): Flow<List<AdoptionRequest>>
}

interface FavoritesRepository {
    /** Ids of the adoption listings the user saved. */
    fun observeFavoriteIds(userId: String): Flow<Set<String>>

    suspend fun toggleFavorite(userId: String, listingId: String)
}

interface NotificationRepository {
    fun observeNotifications(userId: String): Flow<List<AppNotification>>

    /**
     * True while the last load of [userId]'s notifications failed (the list then keeps its last
     * rows, or is empty). In-memory repositories never fail.
     */
    fun observeLoadFailed(userId: String): Flow<Boolean> = flowOf(false)

    /** Loads [userId]'s notifications again, for example after a failure. */
    suspend fun reload(userId: String) {}

    suspend fun markAsRead(notificationId: String)

    suspend fun markAllAsRead(userId: String)

    /** Creates an in-app notification for [userId]. Phase 4 also sends it as an FCM push. */
    suspend fun notify(userId: String, title: String, message: String, type: NotificationType): AppNotification
}

/** RF-03 preferences of one account: radius, priorities and push (`notification_prefs`). */
interface NotificationPrefsRepository {
    /** The saved preferences, or the defaults (5 km, every priority, push on) when none are saved. */
    fun observe(userId: String): Flow<NotificationPrefs>

    /** Saves the preferences of [userId] (the signed-in account). [radiusKm] must be above 0. */
    suspend fun update(
        userId: String,
        radiusKm: Double,
        priorities: Set<Priority>,
        pushEnabled: Boolean,
    ): Result<NotificationPrefs>
}

interface ContactRequestRepository {
    fun observeSent(userId: String): Flow<List<ContactRequest>>

    fun observeReceived(userId: String): Flow<List<ContactRequest>>

    /** Creates a pending "Es mi mascota" request. Fails with a readable message when RF-05 forbids it. */
    suspend fun send(reportId: String, requesterId: String, message: String, photoUrl: String?): Result<ContactRequest>

    /** The requester withdraws a pending request. */
    suspend fun cancel(requestId: String, userId: String): Result<ContactRequest>

    suspend fun accept(requestId: String, userId: String): Result<ContactRequest>

    suspend fun reject(requestId: String, userId: String): Result<ContactRequest>

    /** Closes the report and rejects its other pending requests with "El caso se cerró". */
    suspend fun markDelivered(requestId: String, userId: String): Result<ContactRequest>

    /**
     * The other party's contact data, only when the request is accepted and [userId] is
     * one of its two parties; null otherwise. Mirrors the `get_contact_details` RPC.
     */
    suspend fun getContactDetails(requestId: String, userId: String): ContactDetails?
}

/** FCM tokens of the signed-in account's devices (`device_tokens`, RF-03). */
interface PushTokenRepository {
    /**
     * Saves [token] for the signed-in account. Fails for anonymous sessions or without a
     * session: only accounts receive push notifications.
     */
    suspend fun register(token: String): Result<Unit>

    /** Deletes this device's token. Call it before signing out, while RLS still allows it. */
    suspend fun unregister(): Result<Unit>
}
