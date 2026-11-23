package com.example.fluffypawtrack.di

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.fluffypawtrack.data.fake.FakeAdoptionRepository
import com.example.fluffypawtrack.data.fake.FakeAdoptionRequestRepository
import com.example.fluffypawtrack.data.fake.FakeAuthRepository
import com.example.fluffypawtrack.data.fake.FakeContactRequestRepository
import com.example.fluffypawtrack.data.fake.FakeFavoritesRepository
import com.example.fluffypawtrack.data.fake.FakeNotificationPrefsRepository
import com.example.fluffypawtrack.data.fake.FakeNotificationRepository
import com.example.fluffypawtrack.data.fake.FakeOrganizationRepository
import com.example.fluffypawtrack.data.fake.FakePushTokenRepository
import com.example.fluffypawtrack.data.fake.FakeReportRepository
import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.data.supabase.SupabaseClientProvider
import com.example.fluffypawtrack.data.supabase.SupabaseConfig
import com.example.fluffypawtrack.data.supabase.SupabaseRepositories
import com.example.fluffypawtrack.domain.model.UserProfile
import com.example.fluffypawtrack.domain.repository.AdoptionRepository
import com.example.fluffypawtrack.domain.repository.AdoptionRequestRepository
import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.domain.repository.ContactRequestRepository
import com.example.fluffypawtrack.domain.repository.FavoritesRepository
import com.example.fluffypawtrack.domain.repository.NotificationPrefsRepository
import com.example.fluffypawtrack.domain.repository.NotificationRepository
import com.example.fluffypawtrack.domain.push.PushTokenSync
import com.example.fluffypawtrack.domain.repository.OrganizationRepository
import com.example.fluffypawtrack.domain.repository.PushTokenRepository
import com.example.fluffypawtrack.domain.repository.ReportRepository
import com.example.fluffypawtrack.platform.PushTokenRefreshes
import com.example.fluffypawtrack.platform.currentPushToken
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency wiring. Screens and ViewModels only see the repository interfaces.
 *
 * With a complete [supabaseConfig] every repository is the Supabase one. Without it (missing or
 * blank values) the app runs on the fakes, which keeps tests and previews working offline.
 */
class AppContainer(private val supabaseConfig: SupabaseConfig? = null) {
    /** True when both Supabase settings are present. */
    val usesSupabase: Boolean = supabaseConfig?.isConfigured == true

    /** The single Supabase client; null while running on the fakes. */
    val supabaseClient: SupabaseClient? =
        supabaseConfig?.takeIf { it.isConfigured }?.let(SupabaseClientProvider::create)

    private val supabase: SupabaseRepositories? = supabaseClient?.let(::SupabaseRepositories)

    val authRepository: AuthRepository = supabase?.auth ?: FakeAuthRepository()
    val notificationRepository: NotificationRepository = supabase?.notifications ?: FakeNotificationRepository()
    val notificationPrefsRepository: NotificationPrefsRepository =
        supabase?.notificationPrefs ?: FakeNotificationPrefsRepository()
    val reportRepository: ReportRepository = supabase?.reports
        ?: FakeReportRepository(
            currentUser = { authRepository.currentUser.value },
            notifications = notificationRepository,
        )
    val organizationRepository: OrganizationRepository = supabase?.organizations
        ?: FakeOrganizationRepository(
            currentUser = { authRepository.currentUser.value },
            updateUsers = { transform -> (authRepository as? FakeAuthRepository)?.updateUsers(transform) },
            notifications = notificationRepository,
        )
    val adoptionRepository: AdoptionRepository =
        supabase?.adoptions ?: FakeAdoptionRepository(
            findUser = profileLookup(authRepository),
            currentUser = { authRepository.currentUser.value },
            findOrganization = { id ->
                (organizationRepository as? FakeOrganizationRepository)?.findOrganization(id)
                    ?: SampleData.organizations.firstOrNull { it.id == id }
            },
        )
    val favoritesRepository: FavoritesRepository = supabase?.favorites ?: FakeFavoritesRepository()
    val contactRequestRepository: ContactRequestRepository = supabase?.contactRequests
        ?: FakeContactRequestRepository(
            reportRepository = reportRepository,
            findUser = profileLookup(authRepository),
            findOrganization = organizationRepository::getOrganization,
        )
    val adoptionRequestRepository: AdoptionRequestRepository = supabase?.adoptionRequests
        ?: FakeAdoptionRequestRepository(
            adoptionRepository = adoptionRepository,
            notificationRepository = notificationRepository,
            findUser = profileLookup(authRepository),
        )
    val pushTokenRepository: PushTokenRepository = supabase?.pushTokens
        ?: FakePushTokenRepository(currentUser = { authRepository.currentUser.value })

    private var pushTokenSyncJob: Job? = null

    /**
     * Starts registering this device's FCM token for signed-in accounts (RF-03). `App` calls it
     * once per process; tests and previews leave it off.
     */
    fun startPushTokenSync() {
        if (pushTokenSyncJob != null) return
        pushTokenSyncJob = PushTokenSync(
            auth = authRepository,
            tokens = pushTokenRepository,
            currentToken = ::currentPushToken,
            refreshedTokens = PushTokenRefreshes.refreshed,
        ).start(CoroutineScope(SupervisorJob() + Dispatchers.Default))
    }
}

/**
 * The fake accounts (with runtime sign-ups and role changes such as an approved affiliation),
 * so every fake applies its rules to the current profiles.
 */
private fun profileLookup(authRepository: AuthRepository): (String) -> UserProfile? = { id ->
    (authRepository as? FakeAuthRepository)?.findUser(id)
        ?: authRepository.currentUser.value?.takeIf { it.id == id }
        ?: SampleData.users.firstOrNull { it.id == id }
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}
