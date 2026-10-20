package com.example.localmiles.Screens.Dispatcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.localmiles.Core.Data.Repository.DepotRepository
import com.example.localmiles.Core.Data.Repository.DriverRepository
import com.example.localmiles.Core.Data.Repository.UserRepository
import com.example.localmiles.Core.Network.Dto.DepotResponse
import com.example.localmiles.Core.Network.Dto.DriverResponse
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
    // Short confirmation after a successful change.
    val infoMessage: String? = null,
    // Non-null while a role change for this UserId is in flight (shows a small
    // spinner on just that row instead of blocking the whole screen).
    val updatingUserId: Long? = null,
    // Depots for the "promote to driver" form's depot picker.
    val depots: List<DepotResponse> = emptyList(),
    // Every Driver row (active or not) — used for duplicate checks, the
    // suggested employee number, and to deactivate a demoted driver's record.
    val drivers: List<DriverResponse> = emptyList(),
    // Non-null while the "promote to driver" dialog is open for this user —
    // driver accounts need a Driver record (depot, licence, etc.), not just a
    // role flip, so promotion collects those fields before calling the API.
    val pendingDriverPromotion: UserAccountResponse? = null,
    val isSubmittingDriverPromotion: Boolean = false,
    val driverFormErrors: DriverFormErrors = DriverFormErrors(),
    val suggestedEmployeeNumber: String = "",
) {
    /** The (possibly deactivated) Driver row a user already has, if any. */
    fun driverFor(userId: Long): DriverResponse? = drivers.firstOrNull { it.userId == userId }
}

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
            refreshDrivers()
        }
    }

    // Best effort: without the list we just skip the duplicate pre-checks and
    // the server still enforces uniqueness.
    private suspend fun refreshDrivers() {
        val result = runCatching { driverRepository.getDrivers() }.getOrNull()
        if (result is Result.Success) {
            _uiState.value = _uiState.value.copy(drivers = result.data)
        }
        // Depot names for the driver rows (and ready for the promotion dialog).
        if (_uiState.value.depots.isEmpty()) {
            val depots = runCatching { depotRepository.getDepots(active = true) }.getOrNull()
            if (depots is Result.Success) {
                _uiState.value = _uiState.value.copy(depots = depots.data)
            }
        }
    }

    /**
     * Promoting to "driver" needs a Driver record (depot/licence/etc.), not just
     * a role flip — see promoteToDriver. Every other role change is a plain
     * PATCH /users/{id}/role, and moving a driver off the driver role also
     * deactivates their Driver record so they stop appearing in job pickers.
     */
    fun setRole(userId: Long, role: String) {
        if (role == Role.DRIVER) {
            val user = _uiState.value.users.firstOrNull { it.userId == userId } ?: return
            beginPromoteToDriver(user)
            return
        }

        if (_uiState.value.updatingUserId != null) return
        val previousRole = _uiState.value.users.firstOrNull { it.userId == userId }?.role

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(updatingUserId = userId, errorMessage = null)
            when (val result = userRepository.updateUserRole(userId, role)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        updatingUserId = null,
                        users = _uiState.value.users.map { if (it.userId == userId) result.data else it },
                        infoMessage = "${result.data.firstName} is now ${roleLabel(role)}.",
                    )
                    if (previousRole == Role.DRIVER) deactivateDriverRecord(userId)
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    updatingUserId = null,
                    errorMessage = result.message,
                )
            }
        }
    }

    private suspend fun deactivateDriverRecord(userId: Long) {
        val driver = _uiState.value.driverFor(userId)?.takeIf { it.isActive } ?: return
        when (val result = driverRepository.deactivateDriver(driver.driverId)) {
            is Result.Success -> _uiState.value = _uiState.value.copy(
                drivers = _uiState.value.drivers.map { if (it.driverId == driver.driverId) result.data else it }
            )
            is Result.Error -> _uiState.value = _uiState.value.copy(
                errorMessage = "Role changed, but their driver record is still active: ${result.message}"
            )
        }
    }

    /** Opens the driver-details dialog and loads active depots for its picker. */
    private fun beginPromoteToDriver(user: UserAccountResponse) {
        _uiState.value = _uiState.value.copy(
            pendingDriverPromotion = user,
            driverFormErrors = DriverFormErrors(),
            errorMessage = null,
            suggestedEmployeeNumber = _uiState.value.driverFor(user.userId)?.employeeNumber
                ?: DriverFormValidator.suggestEmployeeNumber(_uiState.value.drivers),
        )
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
        _uiState.value = _uiState.value.copy(
            pendingDriverPromotion = null,
            driverFormErrors = DriverFormErrors(),
            errorMessage = null,
        )
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

        val errors = DriverFormValidator.validate(
            userId = user.userId,
            depotId = depotId,
            employeeNumber = employeeNumber,
            phoneNumber = phoneNumber,
            licenceNumber = licenceNumber,
            existingDrivers = _uiState.value.drivers,
        )
        if (errors.hasErrors) {
            _uiState.value = _uiState.value.copy(driverFormErrors = errors)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSubmittingDriverPromotion = true,
                driverFormErrors = DriverFormErrors(),
                errorMessage = null,
            )

            val driverResult = driverRepository.createDriver(
                DriverUpsertRequest(
                    userId = user.userId,
                    depotId = depotId,
                    employeeNumber = employeeNumber.trim(),
                    phoneNumber = DriverFormValidator.normalisePhone(phoneNumber),
                    licenceNumber = licenceNumber.trim().uppercase(),
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

            val driver = (driverResult as Result.Success).data
            val drivers = _uiState.value.drivers.filter { it.driverId != driver.driverId } + driver

            when (val roleResult = userRepository.updateUserRole(user.userId, Role.DRIVER)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isSubmittingDriverPromotion = false,
                    pendingDriverPromotion = null,
                    drivers = drivers,
                    users = _uiState.value.users.map { if (it.userId == user.userId) roleResult.data else it },
                    infoMessage = "${user.firstName} is now a driver (${driver.employeeNumber}).",
                )
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isSubmittingDriverPromotion = false,
                    drivers = drivers,
                    errorMessage = "Driver record created, but setting the role failed: ${roleResult.message}",
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearInfoMessage() {
        _uiState.value = _uiState.value.copy(infoMessage = null)
    }
}

/** "admin" -> "a dispatcher", etc. — the UI never says "admin". */
fun roleLabel(role: String): String = when (role) {
    Role.ADMIN -> "a dispatcher"
    Role.DRIVER -> "a driver"
    Role.CLIENT -> "a client"
    else -> role
}
