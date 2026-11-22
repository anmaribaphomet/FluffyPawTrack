package com.example.fluffypawtrack.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.model.GeoPoint
import com.example.fluffypawtrack.domain.model.NewReport
import com.example.fluffypawtrack.domain.model.Priority
import com.example.fluffypawtrack.domain.model.Report
import com.example.fluffypawtrack.domain.model.ReportCategory
import com.example.fluffypawtrack.domain.model.ReportRules
import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.domain.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

/** Steps of the new-report flow, in order (Figma screens 10–16). */
enum class ReportStep { Category, Photo, Location, Details, Summary }

/** Whether the GPS position was obtained, is being read, or the user types the address. */
enum class LocationStatus { Idle, Locating, Found, Manual }

data class NewReportUiState(
    val step: ReportStep = ReportStep.Category,
    val category: ReportCategory? = null,
    /** Local reference to the photo (content URI or in-memory id); fake repository only. */
    val photoRef: String? = null,
    val locationStatus: LocationStatus = LocationStatus.Idle,
    val location: GeoPoint? = null,
    val address: String = "",
    val reference: String = "",
    val description: String = "",
    val priority: Priority? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val publishedReport: Report? = null,
) {
    val isPriorityLocked: Boolean get() = category?.let(ReportRules::isPriorityLocked) ?: false

    val isManualAddress: Boolean get() = locationStatus == LocationStatus.Manual

    val stepNumber: Int get() = step.ordinal + 1

    val canContinue: Boolean
        get() = when (step) {
            ReportStep.Category -> category != null
            // RF-01 allows reports without a photo in the domain (photoUrl is nullable).
            ReportStep.Photo -> true
            ReportStep.Location -> location != null && address.isNotBlank()
            ReportStep.Details -> description.isNotBlank() && priority != null
            ReportStep.Summary -> !isSubmitting && publishedReport == null
        }
}

class NewReportViewModel(
    private val reportRepository: ReportRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val state = MutableStateFlow(NewReportUiState())
    val uiState: StateFlow<NewReportUiState> = state.asStateFlow()

    fun selectCategory(category: ReportCategory) {
        state.update { it.copy(category = category, priority = ReportRules.suggestedPriority(category)) }
    }

    fun selectPriority(priority: Priority) {
        state.update { current ->
            val category = current.category ?: return@update current
            current.copy(priority = ReportRules.resolvePriority(category, priority))
        }
    }

    fun onPhotoSelected(ref: String) {
        state.update { it.copy(photoRef = ref) }
    }

    fun onLocating() {
        state.update { it.copy(locationStatus = LocationStatus.Locating) }
    }

    /** GPS result; null means permission denied or no fix, so the user types the address. */
    fun onLocationResult(point: GeoPoint?) {
        state.update { current ->
            if (point == null) {
                current.copy(
                    locationStatus = LocationStatus.Manual,
                    location = DefaultLocation,
                    address = if (current.locationStatus == LocationStatus.Manual) current.address else "",
                )
            } else {
                current.copy(locationStatus = LocationStatus.Found, location = point, address = point.describe())
            }
        }
    }

    fun onAddressChange(value: String) {
        state.update { it.copy(address = value) }
    }

    fun onReferenceChange(value: String) {
        state.update { it.copy(reference = value.take(MaxReferenceLength)) }
    }

    fun onDescriptionChange(value: String) {
        state.update { it.copy(description = value.take(MaxDescriptionLength)) }
    }

    fun next() {
        state.update { current ->
            if (!current.canContinue || current.step == ReportStep.Summary) return@update current
            current.copy(step = ReportStep.entries[current.step.ordinal + 1])
        }
    }

    /** Moves to the previous step; returns false on the first step so the caller can leave the flow. */
    fun back(): Boolean {
        val current = state.value
        if (current.step == ReportStep.Category) return false
        // Stay on the summary while the upload runs; the back press is still consumed.
        if (current.isSubmitting) return true
        state.update { it.copy(step = ReportStep.entries[it.step.ordinal - 1], errorMessage = null) }
        return true
    }

    fun submit() {
        val current = state.value
        val category = current.category ?: return
        val priority = current.priority ?: return
        val location = current.location ?: return
        if (current.step != ReportStep.Summary || !current.canContinue) return
        state.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            val report = NewReport(
                category = category,
                priority = priority,
                description = current.description.trim(),
                photoUrl = current.photoRef,
                location = location,
                address = current.address.trim(),
                reference = current.reference.trim().ifEmpty { null },
            )
            reportRepository.createReport(authRepository.currentUser.value?.id, report)
                .onSuccess { created -> state.update { it.copy(isSubmitting = false, publishedReport = created) } }
                .onFailure {
                    state.update { it.copy(isSubmitting = false, errorMessage = "No se pudo publicar el reporte. Intenta de nuevo.") }
                }
        }
    }

    companion object {
        /** Central Hermosillo; used when the GPS position is not available. */
        val DefaultLocation = GeoPoint(29.0729, -110.9559)
        const val MaxDescriptionLength = 280
        const val MaxReferenceLength = 140
    }
}

/** "29.07290, -110.95590" until a reverse geocoder exists. */
private fun GeoPoint.describe(): String = "${latitude.format5()}, ${longitude.format5()}"

private fun Double.format5(): String {
    val scaled = (this * 100_000).roundToLong()
    val sign = if (scaled < 0) "-" else ""
    val abs = kotlin.math.abs(scaled)
    return "$sign${abs / 100_000}.${(abs % 100_000).toString().padStart(5, '0')}"
}
