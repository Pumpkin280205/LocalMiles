package com.example.localmiles.Screens.Dispatcher

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.localmiles.Common.Components.PickerField
import com.example.localmiles.Common.Components.RoleSettingsScreen
import com.example.localmiles.Common.Components.SettingSwitchRow
import com.example.localmiles.Common.Components.SettingsCardBody
import com.example.localmiles.Common.Components.SettingsDivider
import com.example.localmiles.Common.Components.SettingsLinkRow
import com.example.localmiles.Common.Components.SettingsSection
import com.example.localmiles.Common.Components.SettingsSpacer
import com.example.localmiles.Core.Network.Dto.DepotResponse
import com.example.localmiles.Core.Utils.DispatcherPreferences
import com.example.localmiles.Core.Utils.HistoryRange
import com.example.localmiles.Core.Utils.JobPriority

/**
 * Dispatcher settings: the shared settings sections every role has,
 * plus the admin's profile and the operational defaults the other dispatcher
 * screens read 
 */
@Composable
fun DispatcherSettingsScreen(
    viewModel: DispatcherSettingsViewModel,
    onBack: () -> Unit,
    onManageUsers: () -> Unit = {},
    onFleet: () -> Unit = {},
    onLiveMap: () -> Unit = {},
    onHistory: () -> Unit = {},
    onLogout: () -> Unit = {}
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by DispatcherPreferences.settings.collectAsStateWithLifecycle()

    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadCurrentUser()
        viewModel.loadDepots()
    }

    LaunchedEffect(uiState.user) {
        uiState.user?.let { user ->
            firstName = user.firstName
            lastName = user.lastName
        }
    }

    RoleSettingsScreen(
        subtitle = "Dispatcher preferences",
        onBack = onBack,
        notificationsDescription = "Receive delivery, failed-stop and driver updates.",
        profileContent = {
            SettingsSection(
                title = "Account",
                description = "Your name as drivers and clients see it."
            ) {
                SettingsCardBody {
                    if (uiState.isLoading) {
                        CircularProgressIndicator()
                    } else {
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("First name") },
                            singleLine = true,
                            enabled = !uiState.isSaving
                        )
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Last name") },
                            singleLine = true,
                            enabled = !uiState.isSaving
                        )
                        Text(
                            text = "${uiState.user?.email ?: ""} · Dispatcher (admin)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { viewModel.updateProfile(firstName = firstName, lastName = lastName) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isSaving
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Save profile")
                            }
                        }
                        uiState.successMessage?.let {
                            Text(text = it, color = MaterialTheme.colorScheme.primary)
                        }
                        uiState.errorMessage?.let {
                            Text(text = it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        roleContent = {
            SettingsSection(
                title = "New Job Defaults",
                description = "Pre-filled on Create Job so routine jobs take fewer taps."
            ) {
                SettingsCardBody {
                    PickerField(
                        label = "Default depot",
                        options = listOf<DepotResponse?>(null) + uiState.depots,
                        selected = uiState.depots.firstOrNull { it.depotId == settings.defaultDepotId },
                        optionLabel = { it?.name ?: "No default" },
                        onSelected = { depot -> DispatcherPreferences.update { s -> s.copy(defaultDepotId = depot?.depotId) } },
                        placeholder = "No default"
                    )

                    Text("Default priority", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        JobPriority.ALL.forEach { priority ->
                            FilterChip(
                                selected = settings.defaultPriority == priority,
                                onClick = { DispatcherPreferences.update { it.copy(defaultPriority = priority) } },
                                label = { Text(priority) }
                            )
                        }
                    }
                }
                SettingsDivider()
                SettingSwitchRow(
                    title = "Add stops after creating a job",
                    description = "Opens the Add Stop form for the new job.",
                    checked = settings.promptForStopsAfterCreate,
                    onCheckedChange = { checked -> DispatcherPreferences.update { it.copy(promptForStopsAfterCreate = checked) } }
                )
                SettingsDivider()
                SettingSwitchRow(
                    title = "Confirm status changes",
                    description = "Ask before dispatching, completing or unassigning a job.",
                    checked = settings.confirmStatusChanges,
                    onCheckedChange = { checked -> DispatcherPreferences.update { it.copy(confirmStatusChanges = checked) } }
                )
            }

            SettingsSpacer()

            SettingsSection(
                title = "Live Map",
                description = "How often driver positions and stops refresh."
            ) {
                SettingSwitchRow(
                    title = "Auto-refresh",
                    description = "Reloads the map while it's open.",
                    checked = settings.mapAutoRefresh,
                    onCheckedChange = { checked -> DispatcherPreferences.update { it.copy(mapAutoRefresh = checked) } }
                )
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DispatcherPreferences.MAP_REFRESH_OPTIONS.forEach { seconds ->
                        FilterChip(
                            selected = settings.mapRefreshSeconds == seconds,
                            enabled = settings.mapAutoRefresh,
                            onClick = { DispatcherPreferences.update { it.copy(mapRefreshSeconds = seconds) } },
                            label = { Text("Every ${seconds}s") }
                        )
                    }
                }
            }

            SettingsSpacer()

            SettingsSection(
                title = "Delivery History",
                description = "The date range History opens with."
            ) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HistoryRange.entries.forEach { range ->
                        FilterChip(
                            selected = settings.historyRange == range,
                            onClick = { DispatcherPreferences.update { it.copy(historyRange = range) } },
                            label = { Text(range.label) }
                        )
                    }
                }
            }

            SettingsSpacer()

            SettingsSection(
                title = "Admin Tools",
                description = "Jump straight to the dispatcher tools."
            ) {
                SettingsLinkRow(Icons.Default.People, "Manage users & drivers", "Change roles, add drivers", onManageUsers)
                SettingsDivider()
                SettingsLinkRow(Icons.Default.DirectionsCar, "Fleet management", "Vehicles and availability", onFleet)
                SettingsDivider()
                SettingsLinkRow(Icons.Default.Map, "Live map", "Drivers and active stops", onLiveMap)
                SettingsDivider()
                SettingsLinkRow(Icons.Default.History, "Delivery history", "Completed and cancelled jobs", onHistory)
            }

            SettingsSpacer()

            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset dispatcher defaults")
            }

            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Text("  Log out")
            }
        }
    )

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log out") },
            text = { Text("Are you sure you want to log out? Your dispatcher defaults stay on this device.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) { Text("Log out") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset defaults?") },
            text = { Text("Default depot, priority, map refresh and history range go back to their original values.") },
            confirmButton = {
                TextButton(onClick = {
                    DispatcherPreferences.reset()
                    showResetDialog = false
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }
}
