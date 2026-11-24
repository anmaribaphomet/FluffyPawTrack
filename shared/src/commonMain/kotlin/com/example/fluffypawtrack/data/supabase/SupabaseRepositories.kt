package com.example.fluffypawtrack.data.supabase

import com.example.fluffypawtrack.domain.repository.AdoptionRepository
import com.example.fluffypawtrack.domain.repository.AdoptionRequestRepository
import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.domain.repository.ContactRequestRepository
import com.example.fluffypawtrack.domain.repository.FavoritesRepository
import com.example.fluffypawtrack.domain.repository.NotificationPrefsRepository
import com.example.fluffypawtrack.domain.repository.NotificationRepository
import com.example.fluffypawtrack.domain.repository.OrganizationRepository
import com.example.fluffypawtrack.domain.repository.PushTokenRepository
import com.example.fluffypawtrack.domain.repository.ReportRepository
import com.example.fluffypawtrack.platform.currentPushToken
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Every Supabase repository on one [client], sharing one [QueryRegistry] so a mutation in any
 * of them refreshes the lists the others show.
 */
class SupabaseRepositories(
    client: SupabaseClient,
    deviceToken: suspend () -> String? = ::currentPushToken,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val queries = QueryRegistry()
    private val photos = PhotoUploader(client)

    val auth: AuthRepository = SupabaseAuthRepository(client, scope, queries, photos)
    val reports: ReportRepository = SupabaseReportRepository(client, queries, photos)
    val organizations: OrganizationRepository = SupabaseOrganizationRepository(client, queries, photos)
    val adoptions: AdoptionRepository = SupabaseAdoptionRepository(client, queries, photos)
    val favorites: FavoritesRepository = SupabaseFavoritesRepository(client, queries)
    val notifications: NotificationRepository = SupabaseNotificationRepository(client, queries)
    val notificationPrefs: NotificationPrefsRepository = SupabaseNotificationPrefsRepository(client, queries)
    val contactRequests: ContactRequestRepository = SupabaseContactRequestRepository(client, queries, photos)
    val adoptionRequests: AdoptionRequestRepository = SupabaseAdoptionRequestRepository(client, queries)
    val pushTokens: PushTokenRepository = SupabasePushTokenRepository(client, deviceToken)
}
