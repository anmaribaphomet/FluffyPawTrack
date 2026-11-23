package com.example.fluffypawtrack.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.model.GeoPoint
import com.example.fluffypawtrack.domain.model.Organization
import com.example.fluffypawtrack.domain.model.OrganizationDraft
import com.example.fluffypawtrack.domain.model.OrganizationType
import com.example.fluffypawtrack.domain.model.Role
import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.domain.repository.OrganizationRepository
import com.example.fluffypawtrack.platform.ReverseGeocoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Per-field validation messages; null means the field is valid. */
data class PlaceEditorErrors(
    val name: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val address: String? = null,
    val schedule: String? = null,
    val capacity: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val description: String? = null,
    val photo: String? = null,
) {
    val hasErrors: Boolean
        get() = listOf(name, latitude, longitude, address, schedule, capacity, phone, email, description, photo)
            .any { it != null }
}

data class PlaceEditorUiState(
    /** True when editing an existing place, false when creating one. */
    val isEditing: Boolean = false,
    val loaded: Boolean = false,
    val blockedMessage: String? = null,
    val name: String = "",
    val type: OrganizationType = OrganizationType.Shelter,
    val latitude: String = "",
    val longitude: String = "",
    val address: String = "",
    val schedule: String = "",
    /** Optional whole number. */
    val capacity: String = "",
    val phone: String = "",
    val email: String = "",
    /** Optional social handle or URL. */
    val social: String = "",
    val description: String = "",
    /** Required photo or logo: a local reference from the picker or the stored URL. */
    val photoRef: String? = null,
    val errors: PlaceEditorErrors = PlaceEditorErrors(),
    /** Form-level error from the repository or the location request. */
    val message: String? = null,
    val isSaving: Boolean = false,
    /** The stored place once it was saved. */
    val saved: Organization? = null,
) {
    /** RF-07: albergues, centros de adopción, veterinarias aliadas y puntos seguros. */
    val typeOptions: List<OrganizationType> get() = OrganizationType.entries
}

private val EmailShape = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

private fun digitsOf(phone: String) = phone.filter { it.isDigit() }

private fun String.toCoordinate(limit: Double): Double? =
    trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= -limit && it <= limit }

/** Blank is valid (no capacity); otherwise a whole number of 0 or more. */
private fun String.toCapacity(): Result<Int?> {
    val text = trim()
    if (text.isEmpty()) return Result.success(null)
    val value = text.toIntOrNull()
    return if (value != null && value >= 0) Result.success(value) else Result.failure(IllegalArgumentException())
}

internal fun validatePlace(state: PlaceEditorUiState) = PlaceEditorErrors(
    name = if (state.name.isBlank()) "Ingresa el nombre del lugar" else null,
    latitude = if (state.latitude.toCoordinate(90.0) == null) "Ingresa una latitud entre -90 y 90" else null,
    longitude = if (state.longitude.toCoordinate(180.0) == null) "Ingresa una longitud entre -180 y 180" else null,
    address = if (state.address.isBlank()) "Ingresa la dirección" else null,
    schedule = if (state.schedule.isBlank()) "Ingresa el horario" else null,
    capacity = if (state.capacity.toCapacity().isFailure) "La capacidad debe ser un número entero de 0 o más" else null,
    phone = if (digitsOf(state.phone).length != 10) "Ingresa un teléfono de 10 dígitos" else null,
    email = when {
        state.email.isBlank() -> "Ingresa el correo"
        !EmailShape.matches(state.email.trim()) -> "Ingresa un correo válido"
        else -> null
    },
    description = if (state.description.isBlank()) "Agrega una descripción del lugar" else null,
    photo = if (state.photoRef.isNullOrBlank()) "Agrega una foto del lugar o su logo" else null,
)

/** Plain decimal text of a coordinate, as the admin would type it. */
private fun Double.coordinateText(): String = toString().removeSuffix(".0")

/** Admin → create or edit a place of the directory (RF-07). [organizationId] null creates one. */
class PlaceEditorViewModel(
    authRepository: AuthRepository,
    private val organizationRepository: OrganizationRepository,
    private val organizationId: String?,
    private val reverseGeocoder: ReverseGeocoder = ReverseGeocoder { null },
) : ViewModel() {

    private val state = MutableStateFlow(PlaceEditorUiState(isEditing = organizationId != null))
    val uiState: StateFlow<PlaceEditorUiState> = state.asStateFlow()

    init {
        if (authRepository.currentUser.value?.role != Role.Admin) {
            state.update { it.copy(loaded = true, blockedMessage = ADMIN_ONLY_MESSAGE) }
        } else if (organizationId == null) {
            state.update { it.copy(loaded = true) }
        } else {
            viewModelScope.launch {
                val place = organizationRepository.getOrganization(organizationId)
                state.update { if (place == null) it.copy(loaded = true, blockedMessage = "El lugar no existe") else it.from(place) }
            }
        }
    }

    private fun PlaceEditorUiState.from(place: Organization) = copy(
        loaded = true,
        name = place.name,
        type = place.type,
        latitude = place.location.latitude.coordinateText(),
        longitude = place.location.longitude.coordinateText(),
        address = place.address,
        schedule = place.schedule,
        capacity = place.capacity?.toString().orEmpty(),
        phone = place.phone,
        email = place.email,
        social = place.social.orEmpty(),
        description = place.description,
        photoRef = place.photoUrl.takeIf { it.isNotBlank() },
    )

    private inline fun edit(crossinline change: (PlaceEditorUiState) -> PlaceEditorUiState) =
        state.update { change(it).copy(saved = null) }

    fun onNameChange(value: String) = edit { it.copy(name = value, errors = it.errors.copy(name = null)) }

    fun onTypeChange(value: OrganizationType) = edit { it.copy(type = value) }

    fun onLatitudeChange(value: String) = edit { it.copy(latitude = value, errors = it.errors.copy(latitude = null)) }

    fun onLongitudeChange(value: String) = edit { it.copy(longitude = value, errors = it.errors.copy(longitude = null)) }

    fun onAddressChange(value: String) = edit { it.copy(address = value, errors = it.errors.copy(address = null)) }

    fun onScheduleChange(value: String) = edit { it.copy(schedule = value, errors = it.errors.copy(schedule = null)) }

    fun onCapacityChange(value: String) = edit { it.copy(capacity = value, errors = it.errors.copy(capacity = null)) }

    fun onPhoneChange(value: String) = edit { it.copy(phone = value, errors = it.errors.copy(phone = null)) }

    fun onEmailChange(value: String) = edit { it.copy(email = value, errors = it.errors.copy(email = null)) }

    fun onSocialChange(value: String) = edit { it.copy(social = value) }

    fun onDescriptionChange(value: String) =
        edit { it.copy(description = value, errors = it.errors.copy(description = null)) }

    fun onPhotoSelected(ref: String) = edit { it.copy(photoRef = ref, errors = it.errors.copy(photo = null)) }

    fun onPhotoRemoved() = edit { it.copy(photoRef = null) }

    /** Result of "Usar mi ubicación": fills the coordinates, and the address when it is empty. */
    fun onLocationResult(point: GeoPoint?) {
        if (point == null) {
            state.update { it.copy(message = "No pudimos obtener tu ubicación. Ingresa las coordenadas a mano.") }
            return
        }
        edit {
            it.copy(
                latitude = point.latitude.coordinateText(),
                longitude = point.longitude.coordinateText(),
                errors = it.errors.copy(latitude = null, longitude = null),
                message = null,
            )
        }
        if (state.value.address.isNotBlank()) return
        viewModelScope.launch {
            val address = try {
                reverseGeocoder.addressFor(point)?.trim()?.takeIf { it.isNotEmpty() }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            } ?: return@launch
            state.update { if (it.address.isBlank()) it.copy(address = address, errors = it.errors.copy(address = null)) else it }
        }
    }

    fun onSave() {
        val current = state.value
        if (current.isSaving || current.blockedMessage != null) return
        val errors = validatePlace(current)
        val latitude = current.latitude.toCoordinate(90.0)
        val longitude = current.longitude.toCoordinate(180.0)
        val photo = current.photoRef
        if (errors.hasErrors || latitude == null || longitude == null || photo == null) {
            state.update { it.copy(errors = errors) }
            return
        }
        val draft = OrganizationDraft(
            type = current.type,
            name = current.name.trim(),
            description = current.description.trim(),
            location = GeoPoint(latitude, longitude),
            address = current.address.trim(),
            schedule = current.schedule.trim(),
            capacity = current.capacity.toCapacity().getOrNull(),
            phone = digitsOf(current.phone),
            email = current.email.trim(),
            social = current.social.trim().takeIf { it.isNotEmpty() },
            photoUrl = photo,
        )
        state.update { it.copy(isSaving = true, message = null) }
        viewModelScope.launch {
            val result = if (organizationId == null) {
                organizationRepository.createOrganization(draft)
            } else {
                organizationRepository.updateOrganization(organizationId, draft)
            }
            result
                .onSuccess { place -> state.update { it.copy(isSaving = false, saved = place) } }
                .onFailure { e -> state.update { it.copy(isSaving = false, message = e.message ?: "No se pudo guardar el lugar") } }
        }
    }
}
