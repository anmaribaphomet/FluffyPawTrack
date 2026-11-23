package com.example.fluffypawtrack.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.model.Organization
import com.example.fluffypawtrack.domain.model.OrganizationType
import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.domain.repository.OrganizationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlacesDirectoryUiState(
    val loaded: Boolean = false,
    val blockedMessage: String? = null,
    /** Every place of the directory, before the filters. */
    val allPlaces: List<Organization> = emptyList(),
    /** Null shows every type. */
    val typeFilter: OrganizationType? = null,
    /** Null shows every status. */
    val statusFilter: PlaceStatus? = null,
    val confirmingDelete: Organization? = null,
    /** Id of the place an action is running on. */
    val busyId: String? = null,
    val message: String? = null,
    val notice: String? = null,
) {
    /** The places that match both filters, by name. */
    val places: List<Organization>
        get() = allPlaces.filter { place ->
            (typeFilter == null || place.type == typeFilter) &&
                (statusFilter == null || place.placeStatus == statusFilter)
        }
}

/** Admin → "Directorio de lugares" (RF-07): every place with filters, visibility toggle and delete. */
class PlacesDirectoryViewModel(
    authRepository: AuthRepository,
    private val organizationRepository: OrganizationRepository,
) : ViewModel() {

    private val state = MutableStateFlow(PlacesDirectoryUiState())
    val uiState: StateFlow<PlacesDirectoryUiState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.whileAdmin { organizationRepository.observeAllForAdmin() }.collect { places ->
                state.update {
                    if (places == null) {
                        it.copy(loaded = true, blockedMessage = ADMIN_ONLY_MESSAGE, allPlaces = emptyList(), confirmingDelete = null)
                    } else {
                        it.copy(loaded = true, blockedMessage = null, allPlaces = places)
                    }
                }
            }
        }
    }

    fun onTypeFilter(type: OrganizationType?) = state.update { it.copy(typeFilter = type) }

    fun onStatusFilter(status: PlaceStatus?) = state.update { it.copy(statusFilter = status) }

    /** Shows a place on the map when it is hidden, and hides it when it is shown. */
    fun toggleActive(organizationId: String) {
        val place = state.value.allPlaces.firstOrNull { it.id == organizationId } ?: return
        if (state.value.busyId != null) return
        val active = !place.active
        state.update { it.copy(busyId = organizationId, message = null, notice = null) }
        viewModelScope.launch {
            organizationRepository.setActive(organizationId, active)
                .onSuccess { updated ->
                    val notice = if (active) "${updated.name} ya aparece en el mapa." else "${updated.name} ya no aparece en el mapa."
                    state.update { it.copy(busyId = null, notice = notice) }
                }
                .onFailure { e -> state.update { it.copy(busyId = null, message = e.message ?: "No se pudo actualizar el lugar") } }
        }
    }

    fun requestDelete(organizationId: String) {
        val place = state.value.allPlaces.firstOrNull { it.id == organizationId } ?: return
        state.update { it.copy(confirmingDelete = place, notice = null) }
    }

    fun dismissDelete() = state.update { it.copy(confirmingDelete = null) }

    fun confirmDelete() {
        val place = state.value.confirmingDelete ?: return
        if (state.value.busyId != null) return
        state.update { it.copy(confirmingDelete = null, busyId = place.id, message = null, notice = null) }
        viewModelScope.launch {
            organizationRepository.deleteOrganization(place.id)
                .onSuccess { state.update { it.copy(busyId = null, notice = "Se eliminó ${place.name} del directorio.") } }
                .onFailure { e -> state.update { it.copy(busyId = null, message = e.message ?: "No se pudo eliminar el lugar") } }
        }
    }

    fun dismissMessage() = state.update { it.copy(message = null, notice = null) }
}
