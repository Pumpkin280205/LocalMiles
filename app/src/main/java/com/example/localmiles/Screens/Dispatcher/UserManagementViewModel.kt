package com.example.localmiles.Screens.Dispatcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.localmiles.Core.Data.Repository.UserRepository
import com.example.localmiles.Core.Network.Dto.UserAccountResponse
import com.example.localmiles.Core.Utils.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserManagementUiState(
    val isLoading: Boolean = false,
    val users: List<UserAccountResponse> = emptyList(),
    val errorMessage: String? = null,
    // Non-null while a role change for this UserId is in flight (shows a small
    // spinner on just that row instead of blocking the whole screen).
    val updatingUserId: Long? = null,
)

class UserManagementViewModel(
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserManagementUiState())
    val uiState: StateFlow<UserManagementUiState> = _uiState.asStateFlow()

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = userRepository.getUsers()) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    users = result.data.sortedBy { it.userId },
                )
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.message,
                )
            }
        }
    }

    fun setRole(userId: Long, role: String) {
        if (_uiState.value.updatingUserId != null) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(updatingUserId = userId, errorMessage = null)
            when (val result = userRepository.updateUserRole(userId, role)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    updatingUserId = null,
                    users = _uiState.value.users.map { if (it.userId == userId) result.data else it },
                )
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    updatingUserId = null,
                    errorMessage = result.message,
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
