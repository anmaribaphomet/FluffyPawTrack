package com.example.fluffypawtrack.di

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.fluffypawtrack.data.fake.FakeAdoptionRepository
import com.example.fluffypawtrack.data.fake.FakeAuthRepository
import com.example.fluffypawtrack.data.fake.FakeContactRequestRepository
import com.example.fluffypawtrack.data.fake.FakeFavoritesRepository
import com.example.fluffypawtrack.data.fake.FakeNotificationRepository
import com.example.fluffypawtrack.data.fake.FakeOrganizationRepository
import com.example.fluffypawtrack.data.fake.FakeReportRepository
import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.domain.repository.AdoptionRepository
import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.domain.repository.ContactRequestRepository
import com.example.fluffypawtrack.domain.repository.FavoritesRepository
import com.example.fluffypawtrack.domain.repository.NotificationRepository
import com.example.fluffypawtrack.domain.repository.OrganizationRepository
import com.example.fluffypawtrack.domain.repository.ReportRepository

/**
 * Manual dependency wiring. Phase 2 swaps the fake implementations for the Supabase
 * ones here; screens and ViewModels only see the interfaces.
 */
class AppContainer(
    val authRepository: AuthRepository = FakeAuthRepository(),
    val reportRepository: ReportRepository = FakeReportRepository(),
    val organizationRepository: OrganizationRepository = FakeOrganizationRepository(),
    val adoptionRepository: AdoptionRepository = FakeAdoptionRepository(),
    val favoritesRepository: FavoritesRepository = FakeFavoritesRepository(),
    val notificationRepository: NotificationRepository = FakeNotificationRepository(),
    val contactRequestRepository: ContactRequestRepository = FakeContactRequestRepository(
        reportRepository = reportRepository,
        // The signed-in profile first, so accounts created at runtime can send requests too.
        findUser = { id ->
            authRepository.currentUser.value?.takeIf { it.id == id }
                ?: SampleData.users.firstOrNull { it.id == id }
        },
        findOrganization = organizationRepository::getOrganization,
    ),
)

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}
