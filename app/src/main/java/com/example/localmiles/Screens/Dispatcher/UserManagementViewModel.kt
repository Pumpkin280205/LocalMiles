package com.example.localmiles.Screens.Dispatcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.localmiles.Core.Data.Repository.DepotRepository
import com.example.localmiles.Core.Data.Repository.DriverRepository
import com.example.localmiles.Core.Data.Repository.UserRepository
import com.example.localmiles.Core.Network.Dto.DepotResponse
import com.example.localmiles.Core.Network.Dto.DriverUpsertRequest
import com.example.localmiles.Core.Network.Dto.UserAccountResponse
import com.example.localmiles.Core.Utils.Result
import com.example.localmiles.Core.Utils.Role
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
    // Depots for the "promote to driver" form's depot picker.
    val depots: List<DepotResponse> = emptyList(),
    // Non-null while the "promote to driver" dialog is open for this user —
    // driver accounts need a Driver record (depot, licence, etc.), not just a
    // role flip, so promotion collects those fields before calling the API.
    val pendingDriverPromotion: UserAccountResponse? = null,
    val isSubmittingDriverPromotion: Boolean = false,
)

class UserManagementViewModel(
    private val userRepository: UserRepository,
    private val driverRepository: DriverRepository,
    private val depotRepository: DepotRepository,
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

    /**
     * Promoting to "driver" needs a Driver record (depot/licence/etc.), not just
     * a role flip — see promoteToDriver. Every other role change is a plain
     * PATCH /users/{id}/role.
     */
    fun setRole(userId: Long, role: String) {
        if (role == Role.DRIVER) {
            val user = _uiState.value.users.firstOrNull { it.userId == userId } ?: return
            beginPromoteToDriver(user)
            return
        }

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

    /** Opens the driver-details dialog and loads active depots for its picker. */
    private fun beginPromoteToDriver(user: UserAccountResponse) {
        _uiState.value = _uiState.value.copy(pendingDriverPromotion = user)
        if (_uiState.value.depots.isEmpty()) {
            viewModelScope.launch {
                when (val result = depotRepository.getDepots(active = true)) {
                    is Result.Success -> _uiState.value = _uiState.value.copy(depots = result.data)
                    is Result.Error -> _uiState.value =
                        _uiState.value.copy(errorMessage = result.message)
                }
            }
        }
    }

    fun cancelPromoteToDriver() {
        _uiState.value = _uiState.value.copy(pendingDriverPromotion = null)
    }

    /**
     * Creates the Driver record for the pending user, then flips their role to
     * "driver". If the Driver record fails to create, the role is left
     * untouched so the account never ends up as a driver with no Driver row
     * (which 404s on every driver-only endpoint).
     */
    fun confirmPromoteToDriver(
        depotId: Long,
        employeeNumber: String,
        phoneNumber: String,
        licenceNumber: String,
    ) {
        val user = _uiState.value.pendingDriverPromotion ?: return
        if (_uiState.value.isSubmittingDriverPromotion) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingDriverPromotion = true)

            val driverResult = driverRepository.createDriver(
                DriverUpsertRequest(
                    userId = user.userId,
                    depotId = depotId,
                    employeeNumber = employeeNumber,
                    phoneNumber = phoneNumber,
                    licenceNumber = licenceNumber,
                    isAvailable = true,
                    isActive = true,
                )
            )

            if (driverResult is Result.Error) {
                _uiState.value = _uiState.value.copy(
                    isSubmittingDriverPromotion = false,
                    errorMessage = driverResult.message,
                )
                return@launch
            }

            when (val roleResult = userRepository.updateUserRole(user.userId, Role.DRIVER)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isSubmittingDriverPromotion = false,
                    pendingDriverPromotion = null,
                    users = _uiState.value.users.map { if (it.userId == user.userId) roleResult.data else it },
                )
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isSubmittingDriverPromotion = false,
                    errorMessage = "Driver record created, but setting the role failed: ${roleResult.message}",
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
