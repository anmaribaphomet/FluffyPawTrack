package com.example.fluffypawtrack.ui.affiliate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.model.AdoptionListing
import com.example.fluffypawtrack.domain.model.ListingRules
import com.example.fluffypawtrack.domain.model.NewListing
import com.example.fluffypawtrack.domain.repository.AdoptionRepository
import com.example.fluffypawtrack.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal const val REQUIRED_FIELD = "Este dato es obligatorio"

data class ListingFormErrors(
    val name: String? = null,
    val species: String? = null,
    val breed: String? = null,
    val age: String? = null,
    val vaccination: String? = null,
    val description: String? = null,
    val photos: String? = null,
) {
    val isEmpty: Boolean
        get() = listOf(name, species, breed, age, vaccination, description, photos).all { it == null }
}

data class ListingEditorUiState(
    val isEditing: Boolean = false,
    val name: String = "",
    val species: String = "",
    val breed: String = "",
    val age: String = "",
    val vaccination: String = "",
    val description: String = "",
    /** Photo references; the first one is the cover. */
    val photos: List<String> = emptyList(),
    val errors: ListingFormErrors = ListingFormErrors(),
    val isSaving: Boolean = false,
    /** Readable repository failure. */
    val message: String? = null,
    /** Set once the listing was created or updated. */
    val saved: AdoptionListing? = null,
)

/**
 * Creates ([listingId] null) or edits an adoption listing (RF-06): "fotos, tipo de animal, raza,
 * edad, vacunación y descripción", plus the name, with at least one photo.
 */
class ListingEditorViewModel(
    private val listingId: String?,
    authRepository: AuthRepository,
    private val adoptionRepository: AdoptionRepository,
) : ViewModel() {

    private val state = MutableStateFlow(ListingEditorUiState(isEditing = listingId != null))
    val uiState: StateFlow<ListingEditorUiState> = state.asStateFlow()

    init {
        if (listingId != null && authRepository.currentUser.value.affiliateOrganizationId != null) {
            viewModelScope.launch {
                val listing = adoptionRepository.getListing(listingId)
                if (listing == null) {
                    state.update { it.copy(message = "Este animal ya no está disponible.") }
                } else {
                    state.update {
                        it.copy(
                            name = listing.name,
                            species = listing.species,
                            breed = listing.breed,
                            age = listing.age,
                            vaccination = listing.vaccination,
                            description = listing.description,
                            photos = listing.photoUrls,
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) = state.update { it.copy(name = value, errors = it.errors.copy(name = null)) }

    fun onSpeciesChange(value: String) = state.update { it.copy(species = value, errors = it.errors.copy(species = null)) }

    fun onBreedChange(value: String) = state.update { it.copy(breed = value, errors = it.errors.copy(breed = null)) }

    fun onAgeChange(value: String) = state.update { it.copy(age = value, errors = it.errors.copy(age = null)) }

    fun onVaccinationChange(value: String) =
        state.update { it.copy(vaccination = value, errors = it.errors.copy(vaccination = null)) }

    fun onDescriptionChange(value: String) =
        state.update { it.copy(description = value, errors = it.errors.copy(description = null)) }

    fun onPhotoAdded(ref: String) = state.update {
        if (ref in it.photos) it else it.copy(photos = it.photos + ref, errors = it.errors.copy(photos = null))
    }

    fun onPhotoRemoved(ref: String) = state.update { it.copy(photos = it.photos - ref) }

    fun dismissMessage() = state.update { it.copy(message = null) }

    fun onSave() {
        val current = state.value
        if (current.isSaving) return
        val errors = validate(current)
        if (!errors.isEmpty) {
            state.update { it.copy(errors = errors) }
            return
        }
        val listing = NewListing(
            name = current.name.trim(),
            species = current.species.trim(),
            breed = current.breed.trim(),
            age = current.age.trim(),
            vaccination = current.vaccination.trim(),
            description = current.description.trim(),
            photoUrls = current.photos,
        )
        state.update { it.copy(isSaving = true, message = null) }
        viewModelScope.launch {
            val result = if (listingId == null) {
                adoptionRepository.createListing(listing)
            } else {
                adoptionRepository.updateListing(listingId, listing)
            }
            result
                .onSuccess { saved -> state.update { it.copy(isSaving = false, saved = saved) } }
                .onFailure { error ->
                    state.update { it.copy(isSaving = false, message = error.message ?: "No se pudo guardar el animal") }
                }
        }
    }

    private fun validate(form: ListingEditorUiState): ListingFormErrors {
        fun required(value: String) = REQUIRED_FIELD.takeIf { value.isBlank() }
        return ListingFormErrors(
            name = required(form.name),
            species = required(form.species),
            breed = required(form.breed),
            age = required(form.age),
            vaccination = required(form.vaccination),
            description = required(form.description),
            photos = ListingRules.MISSING_PHOTO.takeIf { form.photos.none { it.isNotBlank() } },
        )
    }
}
