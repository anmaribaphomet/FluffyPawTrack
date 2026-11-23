package com.example.fluffypawtrack.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.domain.repository.OrganizationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class AdminHomeUiState(
    /** Affiliation requests waiting for review, shown on the shortcut. */
    val pendingCount: Int = 0,
    val blockedMessage: String? = null,
)

/** Admin home: shortcuts to the RF-07 sections with the pending affiliation count. */
class AdminHomeViewModel(
    authRepository: AuthRepository,
    organizationRepository: OrganizationRepository,
) : ViewModel() {

    private val state = MutableStateFlow(AdminHomeUiState())
    val uiState: StateFlow<AdminHomeUiState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.whileAdmin { organizationRepository.observePendingAffiliations().map { it.size } }
                .collect { count ->
                    state.value = if (count == null) {
                        AdminHomeUiState(blockedMessage = ADMIN_ONLY_MESSAGE)
                    } else {
                        AdminHomeUiState(pendingCount = count)
                    }
                }
        }
    }
}
