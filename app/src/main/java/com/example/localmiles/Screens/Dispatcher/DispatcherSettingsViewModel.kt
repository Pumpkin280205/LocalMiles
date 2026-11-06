package com.example.localmiles.Screens.Dispatcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.localmiles.Core.Data.Repository.DepotRepository
import com.example.localmiles.Core.Data.Repository.UserRepository
import com.example.localmiles.Core.Network.Dto.DepotResponse
import com.example.localmiles.Core.Network.Dto.UserAccountResponse
import com.example.localmiles.Core.Utils.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DispatcherSettingsUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val user: UserAccountResponse? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    // Active depots for the "default depot" preference.
    val depots: List<DepotResponse> = emptyList(),
)

class DispatcherSettingsViewModel(
    private val userRepository: UserRepository,
    private val depotRepository: DepotRepository? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DispatcherSettingsUiState()
    )

    val uiState: StateFlow<DispatcherSettingsUiState> =
        _uiState.asStateFlow()

    fun loadCurrentUser() {

        if (_uiState.value.isLoading) {
            return
        }

        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            when (val result = userRepository.getCurrentUser()) {

                is Result.Success -> {

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = result.data,
                        errorMessage = null
                    )
                }

                is Result.Error -> {

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun updateProfile(
        firstName: String,
        lastName: String
    ) {

        val trimmedFirstName = firstName.trim()
        val trimmedLastName = lastName.trim()

        if (trimmedFirstName.isBlank()) {

            _uiState.value = _uiState.value.copy(
                errorMessage = "First name cannot be empty.",
                successMessage = null
            )

            return
        }

        if (trimmedLastName.isBlank()) {

            _uiState.value = _uiState.value.copy(
                errorMessage = "Last name cannot be empty.",
                successMessage = null
            )

            return
        }

        if (_uiState.value.isSaving) {
            return
        }

        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(
                isSaving = true,
                errorMessage = null,
                successMessage = null
            )

            when (
                val result = userRepository.updateCurrentUser(
                    firstName = trimmedFirstName,
                    lastName = trimmedLastName
                )
            ) {

                is Result.Success -> {

                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        user = result.data,
                        errorMessage = null,
                        successMessage = "Profile updated successfully."
                    )
                }

                is Result.Error -> {

                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = result.message,
                        successMessage = null
                    )
                }
            }
        }
    }

    fun loadDepots() {

        val repository = depotRepository ?: return

        viewModelScope.launch {

            when (val result = repository.getDepots(active = true)) {

                is Result.Success -> {

                    _uiState.value = _uiState.value.copy(
                        depots = result.data
                    )
                }

                is Result.Error -> Unit
            }
        }
    }

    fun clearMessages() {

        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}