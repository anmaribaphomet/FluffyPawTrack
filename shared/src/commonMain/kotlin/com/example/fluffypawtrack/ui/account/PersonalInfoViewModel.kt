package com.example.fluffypawtrack.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.model.ProfileRules
import com.example.fluffypawtrack.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Confirmation shown after a successful save. */
const val PERSONAL_INFO_SAVED = "Cambios guardados"

/** The editable fields while "Editar" is open. The email is not one of them. */
data class PersonalInfoDraft(
    val firstName: String,
    val lastName: String,
    val phone: String,
    val photoRef: String?,
    val firstNameError: String? = null,
    val phoneError: String? = null,
)

data class PersonalInfoUiState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String? = null,
    val phone: String? = null,
    val avatarUrl: String? = null,
    /** Signed-in, non-anonymous accounts of every role may edit their data (RF-08). */
    val canEdit: Boolean = false,
    /** Non-null while editing. */
    val draft: PersonalInfoDraft? = null,
    val isSaving: Boolean = false,
    /** Readable repository failure. */
    val message: String? = null,
    /** True after the last save succeeded, until the next edit. */
    val saved: Boolean = false,
) {
    val isEditing: Boolean get() = draft != null
    val confirmation: String? get() = if (saved) PERSONAL_INFO_SAVED else null
}

private data class EditState(
    val draft: PersonalInfoDraft? = null,
    val isSaving: Boolean = false,
    val message: String? = null,
    val saved: Boolean = false,
)

class PersonalInfoViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val edit = MutableStateFlow(EditState())

    val uiState: StateFlow<PersonalInfoUiState> = combine(authRepository.currentUser, edit) { user, edit ->
        if (user == null) {
            PersonalInfoUiState()
        } else {
            PersonalInfoUiState(
                firstName = user.firstName,
                lastName = user.lastName,
                email = user.email,
                phone = user.phone,
                avatarUrl = user.avatarUrl,
                canEdit = !user.isAnonymous,
                draft = edit.draft,
                isSaving = edit.isSaving,
                message = edit.message,
                saved = edit.saved,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PersonalInfoUiState())

    fun onEdit() {
        val user = authRepository.currentUser.value?.takeUnless { it.isAnonymous } ?: return
        edit.value = EditState(
            draft = PersonalInfoDraft(user.firstName, user.lastName, user.phone.orEmpty(), user.avatarUrl),
        )
    }

    fun onFirstNameChange(value: String) = updateDraft { it.copy(firstName = value, firstNameError = null) }

    fun onLastNameChange(value: String) = updateDraft { it.copy(lastName = value) }

    fun onPhoneChange(value: String) = updateDraft { it.copy(phone = value, phoneError = null) }

    fun onPhotoSelected(ref: String) = updateDraft { it.copy(photoRef = ref) }

    fun onPhotoRemoved() = updateDraft { it.copy(photoRef = null) }

    fun onCancel() {
        if (!edit.value.isSaving) edit.value = EditState()
    }

    fun onSave() {
        val current = edit.value
        val draft = current.draft ?: return
        if (current.isSaving) return
        val checked = draft.copy(
            firstNameError = ProfileRules.firstNameError(draft.firstName),
            phoneError = ProfileRules.phoneError(draft.phone),
        )
        if (checked.firstNameError != null || checked.phoneError != null) {
            edit.update { it.copy(draft = checked) }
            return
        }
        edit.update { it.copy(isSaving = true, message = null) }
        viewModelScope.launch {
            authRepository.updateProfile(draft.firstName, draft.lastName, draft.phone, draft.photoRef)
                .onSuccess { edit.value = EditState(saved = true) }
                .onFailure { error ->
                    edit.update {
                        it.copy(isSaving = false, message = error.message ?: "No se pudieron guardar los cambios")
                    }
                }
        }
    }

    private fun updateDraft(transform: (PersonalInfoDraft) -> PersonalInfoDraft) =
        edit.update { state -> state.draft?.let { state.copy(draft = transform(it), saved = false, message = null) } ?: state }
}
