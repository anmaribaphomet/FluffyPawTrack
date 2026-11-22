package com.example.fluffypawtrack.di

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.fluffypawtrack.data.fake.FakeAdoptionRepository
import com.example.fluffypawtrack.data.fake.FakeAdoptionRequestRepository
import com.example.fluffypawtrack.data.fake.FakeAuthRepository
import com.example.fluffypawtrack.data.fake.FakeContactRequestRepository
import com.example.fluffypawtrack.data.fake.FakeFavoritesRepository
import com.example.fluffypawtrack.data.fake.FakeNotificationRepository
import com.example.fluffypawtrack.data.fake.FakeOrganizationRepository
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
import com.example.fluffypawtrack.domain.repository.NotificationRepository
import com.example.fluffypawtrack.domain.repository.OrganizationRepository
import com.example.fluffypawtrack.domain.repository.ReportRepository
import io.github.jan.supabase.SupabaseClient

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
    val reportRepository: ReportRepository = supabase?.reports ?: FakeReportRepository()
    val organizationRepository: OrganizationRepository = supabase?.organizations ?: FakeOrganizationRepository()
    val adoptionRepository: AdoptionRepository =
        supabase?.adoptions ?: FakeAdoptionRepository(findUser = profileLookup(authRepository))
    val favoritesRepository: FavoritesRepository = supabase?.favorites ?: FakeFavoritesRepository()
    val notificationRepository: NotificationRepository = supabase?.notifications ?: FakeNotificationRepository()
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
}

/** The signed-in profile first, so accounts created at runtime pass the fake rules too. */
private fun profileLookup(authRepository: AuthRepository): (String) -> UserProfile? = { id ->
    authRepository.currentUser.value?.takeIf { it.id == id }
        ?: SampleData.users.firstOrNull { it.id == id }
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}
