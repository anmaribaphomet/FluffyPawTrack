package com.example.fluffypawtrack.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.model.ReportStatus
import com.example.fluffypawtrack.domain.model.Role
import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.domain.repository.FavoritesRepository
import com.example.fluffypawtrack.domain.repository.ReportRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/** Account summary: who is signed in and their counters. */
data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val reports: Int = 0,
    val attended: Int = 0,
    val favorites: Int = 0,
    /** Affiliates see their center's "Solicitudes de adopción" instead of the user-only rows. */
    val isAffiliate: Boolean = false,
    val avatarUrl: String? = null,
) {
    fun toProfileUser() = ProfileUser(name, email, reports, attended, favorites, avatarUrl)
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    authRepository: AuthRepository,
    reportRepository: ReportRepository,
    favoritesRepository: FavoritesRepository,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(ProfileUiState())
            } else {
                combine(
                    reportRepository.observeReportsBy(user.id),
                    favoritesRepository.observeFavoriteIds(user.id),
                ) { reports, favorites ->
                    ProfileUiState(
                        name = "${user.firstName} ${user.lastName}".trim(),
                        email = user.email.orEmpty(),
                        reports = reports.size,
                        // "Atendido" means a shelter already took the case.
                        attended = reports.count { it.status != ReportStatus.Pending },
                        favorites = favorites.size,
                        isAffiliate = user.role == Role.Affiliate,
                        avatarUrl = user.avatarUrl,
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())
}
