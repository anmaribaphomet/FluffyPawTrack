package com.example.fluffypawtrack.domain.repository

import com.example.fluffypawtrack.domain.model.AdoptionListing
import com.example.fluffypawtrack.domain.model.AppNotification
import com.example.fluffypawtrack.domain.model.ContactDetails
import com.example.fluffypawtrack.domain.model.ContactRequest
import com.example.fluffypawtrack.domain.model.NewReport
import com.example.fluffypawtrack.domain.model.Organization
import com.example.fluffypawtrack.domain.model.OrganizationType
import com.example.fluffypawtrack.domain.model.Report
import com.example.fluffypawtrack.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

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
}

/** Every listing it returns has at least one photo. */
interface AdoptionRepository {
    fun observeAvailableListings(): Flow<List<AdoptionListing>>

    fun observeListingsBy(organizationId: String): Flow<List<AdoptionListing>>

    suspend fun getListing(id: String): AdoptionListing?
}

interface FavoritesRepository {
    /** Ids of the adoption listings the user saved. */
    fun observeFavoriteIds(userId: String): Flow<Set<String>>

    suspend fun toggleFavorite(userId: String, listingId: String)
}

interface NotificationRepository {
    fun observeNotifications(userId: String): Flow<List<AppNotification>>

    suspend fun markAsRead(notificationId: String)

    suspend fun markAllAsRead(userId: String)
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
