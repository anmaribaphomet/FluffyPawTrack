package com.example.fluffypawtrack.ui.admin

import androidx.compose.ui.graphics.Color
import com.example.fluffypawtrack.domain.model.Organization
import com.example.fluffypawtrack.domain.model.OrganizationStatus
import com.example.fluffypawtrack.domain.model.Role
import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.ui.theme.FluffyColors
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

// Admin-only labels and helpers. Shared names of domain values stay in ui/common/Labels.kt.

/** Shown instead of an admin screen to anyone who is not the admin. */
internal const val ADMIN_ONLY_MESSAGE = "Esta sección es solo para el administrador."

/**
 * [source] while the signed-in account is the admin; null otherwise, so the screen shows
 * [ADMIN_ONLY_MESSAGE] and never loads admin data for another role.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> AuthRepository.whileAdmin(source: () -> Flow<T>): Flow<T?> =
    currentUser.flatMapLatest { user -> if (user?.role == Role.Admin) source() else flowOf(null) }

/** How a place of the directory is shown: its review status, and for approved ones whether it is on the map. */
enum class PlaceStatus { Active, Inactive, Pending, Rejected }

val Organization.placeStatus: PlaceStatus
    get() = when (status) {
        OrganizationStatus.Pending -> PlaceStatus.Pending
        OrganizationStatus.Rejected -> PlaceStatus.Rejected
        OrganizationStatus.Approved -> if (active) PlaceStatus.Active else PlaceStatus.Inactive
    }

val PlaceStatus.label: String
    get() = when (this) {
        PlaceStatus.Active -> "Activo"
        PlaceStatus.Inactive -> "Inactivo"
        PlaceStatus.Pending -> "Pendiente"
        PlaceStatus.Rejected -> "Rechazado"
    }

/** Status chip color of a place; the text on it is brown-900. */
val PlaceStatus.color: Color
    get() = when (this) {
        PlaceStatus.Active -> FluffyColors.Sage400
        PlaceStatus.Inactive -> FluffyColors.Neutral300
        PlaceStatus.Pending -> FluffyColors.Peach300
        PlaceStatus.Rejected -> FluffyColors.Pink400
    }

/** Report supervision filters (RF-07). Category is filtered separately. */
enum class SupervisionFilter { All, Hidden, Closed }

val SupervisionFilter.label: String
    get() = when (this) {
        SupervisionFilter.All -> "Todos"
        SupervisionFilter.Hidden -> "Ocultos"
        SupervisionFilter.Closed -> "Cerrados"
    }
