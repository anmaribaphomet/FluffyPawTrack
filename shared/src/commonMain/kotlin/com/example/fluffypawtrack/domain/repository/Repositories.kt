package com.example.fluffypawtrack.domain.repository

import com.example.fluffypawtrack.domain.model.AdoptionListing
import com.example.fluffypawtrack.domain.model.AppNotification
import com.example.fluffypawtrack.domain.model.ContactRequest
import com.example.fluffypawtrack.domain.model.NewReport
import com.example.fluffypawtrack.domain.model.Organization
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
}

interface OrganizationRepository {
    /** Approved and active places, shown permanently on the map. */
    fun observeActiveOrganizations(): Flow<List<Organization>>

    suspend fun getOrganization(id: String): Organization?
}

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
}
