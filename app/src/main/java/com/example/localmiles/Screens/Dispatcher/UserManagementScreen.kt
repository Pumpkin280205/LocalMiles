package com.example.localmiles.Screens.Dispatcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.ui.unit.dp
import com.example.localmiles.Core.Network.Dto.DepotResponse
import com.example.localmiles.Core.Network.Dto.UserAccountResponse
import com.example.localmiles.Core.Utils.Role

private val ASSIGNABLE_ROLES = listOf(Role.ADMIN, Role.DRIVER, Role.CLIENT)

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

    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Column {
                Text(
                    text = "Manage users",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Change a person's role — takes effect next time they log in",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (uiState.errorMessage != null) {
            AlertDialog(
                onDismissRequest = viewModel::clearError,
                confirmButton = {
                    TextButton(onClick = viewModel::clearError) { Text("OK") }
                },
                title = { Text("Something went wrong") },
                text = { Text(uiState.errorMessage!!) },
            )
        }

        if (uiState.pendingDriverPromotion != null) {
            PromoteToDriverDialog(
                user = uiState.pendingDriverPromotion!!,
                depots = uiState.depots,
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

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.users, key = { it.userId }) { user ->
                    UserRow(
                        user = user,
                        isUpdating = uiState.updatingUserId == user.userId,
                        onRoleSelected = { role -> viewModel.setRole(user.userId, role) },
                    )
                }
            }
        }
    }
}

@Composable
private fun UserRow(
    user: UserAccountResponse,
    isUpdating: Boolean,
    onRoleSelected: (String) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
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
                    fontWeight = FontWeight.Medium,
                )
                Text(text = user.email, style = MaterialTheme.typography.bodySmall)
            }

            Box {
                if (isUpdating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    TextButton(onClick = { menuExpanded = true }) {
                        Text(user.role)
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Change role")
                    }
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    ASSIGNABLE_ROLES.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role) },
                            onClick = {
                                menuExpanded = false
                                if (role != user.role) onRoleSelected(role)
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
 * licence) before promoting a user to the driver role. A driver row is a hard
 * requirement — see UserManagementViewModel.confirmPromoteToDriver.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PromoteToDriverDialog(
    user: UserAccountResponse,
    depots: List<DepotResponse>,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (depotId: Long, employeeNumber: String, phoneNumber: String, licenceNumber: String) -> Unit,
) {
    var depotMenuExpanded by remember { mutableStateOf(false) }
    var selectedDepot by remember(depots) { mutableStateOf(depots.firstOrNull()) }
    var employeeNumber by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var licenceNumber by remember { mutableStateOf("") }

    val canSubmit = selectedDepot != null &&
        employeeNumber.isNotBlank() &&
        phoneNumber.isNotBlank() &&
        licenceNumber.isNotBlank() &&
        !isSubmitting

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text("Make ${user.firstName} a driver") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "A driver account needs a depot and licence details before it can go live.",
                    style = MaterialTheme.typography.bodySmall,
                )

                ExposedDropdownMenuBox(
                    expanded = depotMenuExpanded,
                    onExpandedChange = { depotMenuExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedDepot?.name ?: "No active depots",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Depot") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = depotMenuExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = depotMenuExpanded,
                        onDismissRequest = { depotMenuExpanded = false },
                    ) {
                        depots.forEach { depot ->
                            DropdownMenuItem(
                                text = { Text(depot.name) },
                                onClick = {
                                    selectedDepot = depot
                                    depotMenuExpanded = false
                                },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = employeeNumber,
                    onValueChange = { employeeNumber = it },
                    label = { Text("Employee number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = licenceNumber,
                    onValueChange = { licenceNumber = it },
                    label = { Text("Licence number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSubmit,
                onClick = {
                    val depotId = selectedDepot?.depotId ?: return@TextButton
                    onConfirm(depotId, employeeNumber.trim(), phoneNumber.trim(), licenceNumber.trim())
                },
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Create driver")
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
