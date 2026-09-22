package com.example.localmiles.Screens.Dispatcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateJobScreen(
    viewModel: DispatcherViewModel,
    jobId: Long? = null,
    onBack: () -> Unit = {},
    onJobSaved: () -> Unit = {}
) {

    val uiState by viewModel.uiState.collectAsState()

    val isEditMode = jobId != null

    var clientId by remember { mutableStateOf("") }
    var depotId by remember { mutableStateOf<Long?>(null) }
    var driverId by remember { mutableStateOf<Long?>(null) }
    var vehicleId by remember { mutableStateOf<Long?>(null) }

    var scheduledDate by remember {
        mutableStateOf("")
    }

    var status by remember {
        mutableStateOf("Pending")
    }

    var priority by remember {
        mutableStateOf("Normal")
    }

    var notes by remember {
        mutableStateOf("")
    }

    var estimatedDistance by remember {
        mutableStateOf("")
    }

    var estimatedDuration by remember {
        mutableStateOf("")
    }

    var showValidationError by remember {
        mutableStateOf(false)
    }

    var statusExpanded by remember {
        mutableStateOf(false)
    }

    var priorityExpanded by remember {
        mutableStateOf(false)
    }

    var clientExpanded by remember {
        mutableStateOf(false)
    }

    var depotExpanded by remember {
        mutableStateOf(false)
    }

    var driverExpanded by remember {
        mutableStateOf(false)
    }

    var vehicleExpanded by remember {
        mutableStateOf(false)
    }

    /*
     * Load supporting data and, when editing, load the selected job.
     */
    LaunchedEffect(jobId) {

        viewModel.clearSaveSuccessful()

        viewModel.loadDispatcherData()

        if (jobId != null) {
            viewModel.loadJob(jobId)
        }
    }

    /*
     * Populate the form when an existing job has been loaded.
     */
    LaunchedEffect(uiState.selectedJob, jobId) {

        val job = uiState.selectedJob

        if (job != null && job.jobId == jobId) {

            clientId = job.clientId.toString()

            depotId = job.depotId
            driverId = job.driverId
            vehicleId = job.vehicleId

            scheduledDate = job.scheduledDate

            status = job.status

            priority = job.priority

            notes = job.notes ?: ""

            estimatedDistance =
                job.estimatedDistanceKm?.toString() ?: ""

            estimatedDuration =
                job.estimatedDurationMinutes?.toString() ?: ""
        }
    }

    /*
     * Return to the job list after a successful create/edit.
     */
    LaunchedEffect(uiState.saveSuccessful) {

        if (uiState.saveSuccessful) {

            viewModel.clearSaveSuccessful()

            onJobSaved()
        }
    }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        text = if (isEditMode) {
                            "Edit Delivery Job"
                        } else {
                            "Create Delivery Job"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {

                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }

    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            if (uiState.isLoading && isEditMode && uiState.selectedJob == null) {

                CircularProgressIndicator(
                    modifier = Modifier.align(
                        Alignment.Center
                    )
                )

            } else {

                Column(

                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(
                            rememberScrollState()
                        ),

                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Text(
                        text = if (isEditMode) {
                            "Update the delivery job details."
                        } else {
                            "Enter the details for the new delivery job."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )

                    /*
                     * Client
                     */

                    Text(
                        text = "Client",
                        fontWeight = FontWeight.Medium
                    )

                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        OutlinedTextField(

                            value = uiState.clients
                                .firstOrNull {
                                    it.clientId == clientId.toLongOrNull()
                                }
                                ?.let {
                                    "${it.contactPerson} (${it.companyName})"
                                }
                                ?: "Select client",

                            onValueChange = {},

                            readOnly = true,

                            modifier = Modifier.fillMaxWidth(),

                            label = {
                                Text("Client")
                            },

                            trailingIcon = {

                                TextButton(
                                    onClick = {
                                        clientExpanded = true
                                    }
                                ) {
                                    Text("Select")
                                }
                            }
                        )

                        DropdownMenu(

                            expanded = clientExpanded,

                            onDismissRequest = {
                                clientExpanded = false
                            }
                        ) {

                            if (uiState.clients.isEmpty()) {

                                DropdownMenuItem(
                                    text = { Text("No clients found") },
                                    onClick = { clientExpanded = false }
                                )
                            }

                            uiState.clients.forEach { client ->

                                DropdownMenuItem(

                                    text = {
                                        Text(
                                            "${client.contactPerson} (${client.companyName})"
                                        )
                                    },

                                    onClick = {

                                        clientId = client.clientId.toString()
                                        showValidationError = false

                                        clientExpanded = false
                                    }
                                )
                            }
                        }
                    }


                    /*
                     * Depot
                     */

                    Text(
                        text = "Depot",
                        fontWeight = FontWeight.Medium
                    )

                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        OutlinedTextField(

                            value = uiState.depots
                                .firstOrNull {
                                    it.depotId == depotId
                                }
                                ?.name
                                ?: "Select depot",

                            onValueChange = {},

                            readOnly = true,

                            modifier = Modifier.fillMaxWidth(),

                            label = {
                                Text("Depot")
                            },

                            trailingIcon = {

                                TextButton(
                                    onClick = {
                                        depotExpanded = true
                                    }
                                ) {
                                    Text("Select")
                                }
                            }
                        )

                        DropdownMenu(

                            expanded = depotExpanded,

                            onDismissRequest = {
                                depotExpanded = false
                            }
                        ) {

                            uiState.depots
                                .filter { it.isActive }
                                .forEach { depot ->

                                    DropdownMenuItem(

                                        text = {
                                            Text(
                                                "${depot.name} (#${depot.depotId})"
                                            )
                                        },

                                        onClick = {

                                            depotId = depot.depotId

                                            depotExpanded = false
                                        }
                                    )
                                }
                        }
                    }


                    /*
                     * Driver
                     */

                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        OutlinedTextField(

                            value = uiState.drivers
                                .firstOrNull {
                                    it.driverId == driverId
                                }
                                ?.let {
                                    "Driver #${it.driverId}"
                                }
                                ?: "Select driver",

                            onValueChange = {},

                            readOnly = true,

                            modifier = Modifier.fillMaxWidth(),

                            label = {
                                Text("Driver")
                            },

                            trailingIcon = {

                                TextButton(
                                    onClick = {
                                        driverExpanded = true
                                    }
                                ) {
                                    Text("Select")
                                }
                            }
                        )

                        DropdownMenu(

                            expanded = driverExpanded,

                            onDismissRequest = {
                                driverExpanded = false
                            }
                        ) {

                            uiState.drivers
                                .filter {
                                    it.isActive
                                }
                                .forEach { driver ->

                                    DropdownMenuItem(

                                        text = {

                                            Text(
                                                "Driver #${driver.driverId} - ${driver.employeeNumber}"
                                            )
                                        },

                                        onClick = {

                                            driverId = driver.driverId

                                            driverExpanded = false
                                        }
                                    )
                                }
                        }
                    }


                    /*
                     * Vehicle
                     */

                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        OutlinedTextField(

                            value = uiState.vehicles
                                .firstOrNull {
                                    it.vehicleId == vehicleId
                                }
                                ?.let {
                                    "${it.registrationNumber} - ${it.make} ${it.model}"
                                }
                                ?: "Select vehicle",

                            onValueChange = {},

                            readOnly = true,

                            modifier = Modifier.fillMaxWidth(),

                            label = {
                                Text("Vehicle")
                            },

                            trailingIcon = {

                                TextButton(
                                    onClick = {
                                        vehicleExpanded = true
                                    }
                                ) {
                                    Text("Select")
                                }
                            }
                        )

                        DropdownMenu(

                            expanded = vehicleExpanded,

                            onDismissRequest = {
                                vehicleExpanded = false
                            }
                        ) {

                            uiState.vehicles
                                .filter {
                                    it.isActive
                                }
                                .forEach { vehicle ->

                                    DropdownMenuItem(

                                        text = {

                                            Text(
                                                "${vehicle.registrationNumber} - " +
                                                        "${vehicle.make} ${vehicle.model}"
                                            )
                                        },

                                        onClick = {

                                            vehicleId = vehicle.vehicleId

                                            vehicleExpanded = false
                                        }
                                    )
                                }
                        }
                    }


                    /*
                     * Scheduled date
                     */

                    OutlinedTextField(

                        value = scheduledDate,

                        onValueChange = {
                            scheduledDate = it
                            showValidationError = false
                        },

                        modifier = Modifier.fillMaxWidth(),

                        label = {
                            Text("Scheduled date")
                        },

                        placeholder = {
                            Text("2026-09-20T10:00:00")
                        },

                        supportingText = {
                            Text(
                                "Use ISO-8601 format, for example 2026-09-20T10:00:00"
                            )
                        },

                        singleLine = true
                    )


                    /*
                     * Status
                     */

                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        OutlinedTextField(

                            value = status,

                            onValueChange = {},

                            readOnly = true,

                            modifier = Modifier.fillMaxWidth(),

                            label = {
                                Text("Status")
                            },

                            trailingIcon = {

                                TextButton(
                                    onClick = {
                                        statusExpanded = true
                                    }
                                ) {
                                    Text("Select")
                                }
                            }
                        )

                        DropdownMenu(

                            expanded = statusExpanded,

                            onDismissRequest = {
                                statusExpanded = false
                            }
                        ) {

                            listOf(
                                "Pending",
                                "Assigned",
                                "EnRoute",
                                "Completed",
                                "Cancelled"
                            ).forEach { option ->

                                DropdownMenuItem(

                                    text = {
                                        Text(option)
                                    },

                                    onClick = {

                                        status = option

                                        statusExpanded = false
                                    }
                                )
                            }
                        }
                    }


                    /*
                     * Priority
                     */

                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        OutlinedTextField(

                            value = priority,

                            onValueChange = {},

                            readOnly = true,

                            modifier = Modifier.fillMaxWidth(),

                            label = {
                                Text("Priority")
                            },

                            trailingIcon = {

                                TextButton(
                                    onClick = {
                                        priorityExpanded = true
                                    }
                                ) {
                                    Text("Select")
                                }
                            }
                        )

                        DropdownMenu(

                            expanded = priorityExpanded,

                            onDismissRequest = {
                                priorityExpanded = false
                            }
                        ) {

                            listOf(
                                "Low",
                                "Normal",
                                "High",
                                "Urgent"
                            ).forEach { option ->

                                DropdownMenuItem(

                                    text = {
                                        Text(option)
                                    },

                                    onClick = {

                                        priority = option

                                        priorityExpanded = false
                                    }
                                )
                            }
                        }
                    }


                    /*
                     * Notes
                     */

                    OutlinedTextField(

                        value = notes,

                        onValueChange = {
                            notes = it
                        },

                        modifier = Modifier.fillMaxWidth(),

                        label = {
                            Text("Notes")
                        },

                        minLines = 3
                    )


                    /*
                     * Estimated distance
                     */

                    OutlinedTextField(

                        value = estimatedDistance,

                        onValueChange = {
                            estimatedDistance = it
                        },

                        modifier = Modifier.fillMaxWidth(),

                        label = {
                            Text("Estimated distance (km)")
                        },

                        placeholder = {
                            Text("Example: 25.5")
                        },

                        singleLine = true
                    )


                    /*
                     * Estimated duration
                     */

                    OutlinedTextField(

                        value = estimatedDuration,

                        onValueChange = {
                            estimatedDuration = it
                        },

                        modifier = Modifier.fillMaxWidth(),

                        label = {
                            Text("Estimated duration (minutes)")
                        },

                        placeholder = {
                            Text("Example: 45")
                        },

                        singleLine = true
                    )


                    /*
                     * Validation
                     */

                    if (showValidationError) {

                        Text(
                            text = "Please enter a valid Client ID and Scheduled Date.",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (uiState.errorMessage != null) {

                        Text(
                            text = uiState.errorMessage
                                ?: "An error occurred.",
                            color = MaterialTheme.colorScheme.error
                        )
                    }


                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )


                    /*
                     * Buttons
                     */

                    Row(

                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        TextButton(

                            onClick = onBack,

                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }


                        Button(

                            onClick = {

                                val parsedClientId =
                                    clientId.toLongOrNull()

                                if (
                                    parsedClientId == null ||
                                    scheduledDate.isBlank()
                                ) {

                                    showValidationError = true

                                    return@Button
                                }

                                val parsedDistance =
                                    estimatedDistance
                                        .toDoubleOrNull()

                                val parsedDuration =
                                    estimatedDuration
                                        .toIntOrNull()

                                if (isEditMode && jobId != null) {

                                    viewModel.updateJob(

                                        jobId = jobId,

                                        clientId = parsedClientId,

                                        depotId = depotId,

                                        driverId = driverId,

                                        vehicleId = vehicleId,

                                        scheduledDate = scheduledDate,

                                        status = status,

                                        priority = priority,

                                        notes = notes.ifBlank {
                                            null
                                        },

                                        estimatedDistanceKm =
                                            parsedDistance,

                                        estimatedDurationMinutes =
                                            parsedDuration
                                    )

                                } else {

                                    viewModel.createJob(

                                        clientId = parsedClientId,

                                        depotId = depotId,

                                        driverId = driverId,

                                        vehicleId = vehicleId,

                                        scheduledDate = scheduledDate,

                                        status = status,

                                        priority = priority,

                                        notes = notes.ifBlank {
                                            null
                                        },

                                        estimatedDistanceKm =
                                            parsedDistance,

                                        estimatedDurationMinutes =
                                            parsedDuration
                                    )
                                }
                            },

                            enabled = !uiState.isSaving,

                            modifier = Modifier.weight(1f)
                        ) {

                            if (uiState.isSaving) {

                                CircularProgressIndicator(
                                    modifier = Modifier.height(20.dp)
                                )

                            } else {

                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null
                                )

                                Spacer(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                )

                                Text(
                                    if (isEditMode) {
                                        "Save Changes"
                                    } else {
                                        "Create Job"
                                    }
                                )
                            }
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(24.dp)
                    )
                }
            }
        }
    }
}