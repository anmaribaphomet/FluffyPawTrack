package com.example.fluffypawtrack.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class PersonalInfoUiState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String? = null,
    val phone: String? = null,
)

class PersonalInfoViewModel(authRepository: AuthRepository) : ViewModel() {

    val uiState: StateFlow<PersonalInfoUiState> = authRepository.currentUser
        .map { user ->
            if (user == null) {
                PersonalInfoUiState()
            } else {
                PersonalInfoUiState(user.firstName, user.lastName, user.email, user.phone)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PersonalInfoUiState())
}
