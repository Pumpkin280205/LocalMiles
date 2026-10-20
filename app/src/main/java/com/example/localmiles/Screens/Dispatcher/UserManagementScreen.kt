package com.example.localmiles.Screens.Dispatcher

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.localmiles.Common.Components.AppTopBar
import com.example.localmiles.Common.Components.EmptyStateCard
import com.example.localmiles.Common.Components.LoadingIndicator
import com.example.localmiles.Common.Components.PickerField
import com.example.localmiles.Core.Network.Dto.DepotResponse
import com.example.localmiles.Core.Network.Dto.DriverResponse
import com.example.localmiles.Core.Network.Dto.UserAccountResponse
import com.example.localmiles.Core.Utils.Role

private val ASSIGNABLE_ROLES = listOf(Role.ADMIN, Role.DRIVER, Role.CLIENT)

private fun roleTitle(role: String) = when (role) {
    Role.ADMIN -> "Dispatcher"
    Role.DRIVER -> "Driver"
    Role.CLIENT -> "Client"
    else -> role
}

/**
 * Admin-only screen: lists every account and lets the admin change a user's
 * role directly from the app, instead of editing the Role column in Supabase
 * by hand. Backed by GET/PATCH /api/users, which are gated server-side to
 * ROLE.ADMIN regardless of what this screen shows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    viewModel: UserManagementViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var roleFilter by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearInfoMessage()
        }
    }

    val query = searchQuery.trim()
    val visibleUsers = uiState.users
        .filter { roleFilter == null || it.role == roleFilter }
        .filter {
            query.isEmpty() ||
                "${it.firstName} ${it.lastName}".contains(query, ignoreCase = true) ||
                it.email.contains(query, ignoreCase = true)
        }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Manage Users",
                subtitle = "Role changes apply at the user's next login",
                showBack = true,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.loadUsers() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh users")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        // Errors inside the promotion dialog are shown in the dialog itself.
        if (uiState.errorMessage != null && uiState.pendingDriverPromotion == null) {
            AlertDialog(
                onDismissRequest = viewModel::clearError,
                confirmButton = {
                    TextButton(onClick = viewModel::clearError) { Text("OK") }
                },
                title = { Text("Something went wrong") },
                text = { Text(uiState.errorMessage!!) },
            )
        }

        uiState.pendingDriverPromotion?.let { user ->
            PromoteToDriverDialog(
                user = user,
                existingDriver = uiState.driverFor(user.userId),
                depots = uiState.depots,
                suggestedEmployeeNumber = uiState.suggestedEmployeeNumber,
                errors = uiState.driverFormErrors,
                serverError = uiState.errorMessage,
                isSubmitting = uiState.isSubmittingDriverPromotion,
                onDismiss = viewModel::cancelPromoteToDriver,
                onConfirm = { depotId, employeeNumber, phoneNumber, licenceNumber ->
                    viewModel.confirmPromoteToDriver(
                        depotId = depotId,
                        employeeNumber = employeeNumber,
                        phoneNumber = phoneNumber,
                        licenceNumber = licenceNumber,
                    )
                },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading && uiState.users.isEmpty()) {
                LoadingIndicator(message = "Loading users...")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search name or email") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = roleFilter == null,
                                onClick = { roleFilter = null },
                                label = { Text("Everyone ${uiState.users.size}") }
                            )
                            ASSIGNABLE_ROLES.forEach { role ->
                                FilterChip(
                                    selected = roleFilter == role,
                                    onClick = { roleFilter = if (roleFilter == role) null else role },
                                    label = { Text("${roleTitle(role)}s ${uiState.users.count { it.role == role }}") }
                                )
                            }
                        }
                    }

                    if (visibleUsers.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Default.People,
                                title = "No users found",
                                message = if (query.isNotEmpty()) "Nothing matches \"$query\"." else "No accounts with this role yet."
                            )
                        }
                    }

                    items(visibleUsers, key = { it.userId }) { user ->
                        UserRow(
                            user = user,
                            driver = uiState.driverFor(user.userId),
                            depotName = { id -> uiState.depots.firstOrNull { it.depotId == id }?.name },
                            isUpdating = uiState.updatingUserId == user.userId,
                            onRoleSelected = { role -> viewModel.setRole(user.userId, role) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserRow(
    user: UserAccountResponse,
    driver: DriverResponse?,
    depotName: (Long) -> String?,
    isUpdating: Boolean,
    onRoleSelected: (String) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${user.firstName} ${user.lastName}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (user.role == Role.DRIVER && driver != null) {
                    Text(
                        text = "${driver.employeeNumber} · ${depotName(driver.depotId) ?: "Depot #${driver.depotId}"}" +
                            if (!driver.isActive) " · inactive" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Box {
                if (isUpdating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    TextButton(onClick = { menuExpanded = true }) {
                        Text(roleTitle(user.role))
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Change role for ${user.firstName}")
                    }
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    ASSIGNABLE_ROLES.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(if (role == user.role) "${roleTitle(role)} (current)" else "Make ${roleTitle(role).lowercase()}") },
                            enabled = role != user.role,
                            onClick = {
                                menuExpanded = false
                                onRoleSelected(role)
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Collects the Driver record's required fields (depot, employee number, phone,
 * licence) before promoting a user to the driver role. Fields are validated in
 * UserManagementViewModel (DriverFormValidator) and errors are shown under the
 * field that caused them; a returning driver's old details are pre-filled.
 */
@Composable
private fun PromoteToDriverDialog(
    user: UserAccountResponse,
    existingDriver: DriverResponse?,
    depots: List<DepotResponse>,
    suggestedEmployeeNumber: String,
    errors: DriverFormErrors,
    serverError: String?,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (depotId: Long, employeeNumber: String, phoneNumber: String, licenceNumber: String) -> Unit,
) {
    var selectedDepot by remember(depots) {
        mutableStateOf(depots.firstOrNull { it.depotId == existingDriver?.depotId } ?: depots.firstOrNull())
    }
    var employeeNumber by remember(suggestedEmployeeNumber) { mutableStateOf(suggestedEmployeeNumber) }
    var phoneNumber by remember { mutableStateOf(existingDriver?.phoneNumber ?: "") }
    var licenceNumber by remember { mutableStateOf(existingDriver?.licenceNumber ?: "") }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text("Make ${user.firstName} a driver") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = if (existingDriver != null)
                        "${user.firstName} was a driver before — their previous details are filled in and their record will be reactivated."
                    else
                        "A driver needs a depot and licence details before they can be assigned jobs.",
                    style = MaterialTheme.typography.bodySmall,
                )

                PickerField(
                    label = "Depot",
                    options = depots,
                    selected = selectedDepot,
                    optionLabel = { it.name },
                    onSelected = { selectedDepot = it },
                    placeholder = if (depots.isEmpty()) "No active depots" else "Select depot",
                    isError = errors.depot != null,
                    supportingText = errors.depot
                )

                OutlinedTextField(
                    value = employeeNumber,
                    onValueChange = { employeeNumber = it },
                    label = { Text("Employee number") },
                    isError = errors.employeeNumber != null,
                    supportingText = { Text(errors.employeeNumber ?: "Suggested — change it if you use your own numbering") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone number") },
                    placeholder = { Text("082 123 4567") },
                    isError = errors.phoneNumber != null,
                    supportingText = errors.phoneNumber?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = licenceNumber,
                    onValueChange = { licenceNumber = it.uppercase() },
                    label = { Text("Driver's licence number") },
                    isError = errors.licenceNumber != null,
                    supportingText = errors.licenceNumber?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                serverError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSubmitting && selectedDepot != null,
                onClick = {
                    val depotId = selectedDepot?.depotId ?: return@Button
                    onConfirm(depotId, employeeNumber, phoneNumber, licenceNumber)
                },
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (existingDriver != null) "Reactivate driver" else "Create driver")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Cancel")
            }
        },
    )
}
