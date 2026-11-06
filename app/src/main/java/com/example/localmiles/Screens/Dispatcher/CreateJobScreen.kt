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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.localmiles.Common.Components.AppTopBar
import com.example.localmiles.Common.Components.LoadingIndicator
import com.example.localmiles.Common.Components.PickerField
import com.example.localmiles.Common.Components.StatusBadge
import com.example.localmiles.Core.Network.Dto.DriverResponse
import com.example.localmiles.Core.Network.Dto.VehicleResponse
import com.example.localmiles.Core.Utils.DateFormatting
import com.example.localmiles.Core.Utils.DispatcherPreferences
import com.example.localmiles.Core.Utils.JobPriority
import com.example.localmiles.Core.Utils.JobStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Create / edit a delivery job.
 *
 * Status is no longer typed in here: a new job starts Pending, or Assigned when
 * a driver is picked, and later status changes go through the Jobs list where
 * only server-valid transitions are offered. 
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateJobScreen(
    viewModel: DispatcherViewModel,
    jobId: Long? = null,
    onBack: () -> Unit = {},
    onJobSaved: (createdJobId: Long?) -> Unit = {}
) {

    val uiState by viewModel.uiState.collectAsState()
    val preferences by DispatcherPreferences.settings.collectAsState()

    val isEditMode = jobId != null

    var clientId by remember { mutableStateOf<Long?>(null) }
    var depotId by remember { mutableStateOf(if (isEditMode) null else preferences.defaultDepotId) }
    var driverId by remember { mutableStateOf<Long?>(null) }
    var vehicleId by remember { mutableStateOf<Long?>(null) }

    var scheduledDate by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
    var scheduledTime by remember { mutableStateOf(LocalTime.of(9, 0)) }

    var priority by remember { mutableStateOf(preferences.defaultPriority) }
    var notes by remember { mutableStateOf("") }
    var estimatedDistance by remember { mutableStateOf("") }
    var estimatedDuration by remember { mutableStateOf("") }

    var submitted by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(jobId) {
        viewModel.clearSaveSuccessful()
        viewModel.clearError()
        viewModel.loadDispatcherData()
        if (jobId != null) {
            viewModel.loadJob(jobId)
        }
    }

    // Populate the form once the job being edited has loaded.
    LaunchedEffect(uiState.selectedJob, jobId) {
        val job = uiState.selectedJob
        if (job != null && job.jobId == jobId) {
            clientId = job.clientId
            depotId = job.depotId
            driverId = job.driverId
            vehicleId = job.vehicleId
            DateFormatting.parse(job.scheduledDate)?.let {
                scheduledDate = it.toLocalDate()
                scheduledTime = it.toLocalTime().withSecond(0).withNano(0)
            }
            priority = job.priority
            notes = job.notes ?: ""
            estimatedDistance = job.estimatedDistanceKm?.toString() ?: ""
            estimatedDuration = job.estimatedDurationMinutes?.toString() ?: ""
        }
    }

    LaunchedEffect(uiState.saveSuccessful) {
        if (uiState.saveSuccessful) {
            val createdId = if (isEditMode) null else uiState.lastCreatedJobId
            viewModel.clearSaveSuccessful()
            viewModel.clearLastCreatedJob()
            onJobSaved(createdId)
        }
    }

    // Drivers and vehicles are filtered to the chosen depot — you can't send a
    // Durban van out of a Johannesburg depot.
    val depotDrivers = uiState.drivers.filter { it.isActive && (depotId == null || it.depotId == depotId) }
    val depotVehicles = uiState.vehicles.filter { it.isActive && (depotId == null || it.depotId == depotId) }

    val parsedDistance = estimatedDistance.toDoubleOrNull()
    val parsedDuration = estimatedDuration.toIntOrNull()

    val clientError = submitted && clientId == null
    val depotError = submitted && depotId == null
    val distanceError = estimatedDistance.isNotBlank() && (parsedDistance == null || parsedDistance < 0)
    val durationError = estimatedDuration.isNotBlank() && (parsedDuration == null || parsedDuration < 0)

    val editingJob = uiState.selectedJob?.takeIf { it.jobId == jobId }

    // Picking a driver for a Pending job assigns it; that's the only status the
    // form changes, everything else is done from the Jobs list.
    val resultingStatus = when {
        !isEditMode -> if (driverId != null) JobStatus.ASSIGNED else JobStatus.PENDING
        editingJob?.status == JobStatus.PENDING && driverId != null -> JobStatus.ASSIGNED
        else -> editingJob?.status
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (isEditMode) "Edit Job #$jobId" else "New Delivery Job",
                subtitle = if (isEditMode) "Update assignment and schedule" else "Step 1 of 2 · job details",
                showBack = true,
                onBack = onBack
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            if (isEditMode && editingJob == null && uiState.isLoading) {
                LoadingIndicator(message = "Loading job...")
                return@Box
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                if (isEditMode && editingJob != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Current status", fontWeight = FontWeight.Medium)
                        StatusBadge(status = editingJob.status)
                    }
                }

                FormSection(title = "Who and where") {

                    PickerField(
                        label = "Client *",
                        options = uiState.clients,
                        selected = uiState.clients.firstOrNull { it.clientId == clientId },
                        optionLabel = { "${it.companyName} (${it.contactPerson})" },
                        onSelected = { clientId = it.clientId },
                        placeholder = "Select client",
                        isError = clientError,
                        supportingText = if (clientError) "Choose the client this delivery is for" else null
                    )

                    PickerField(
                        label = "Depot *",
                        options = uiState.depots.filter { it.isActive },
                        selected = uiState.depots.firstOrNull { it.depotId == depotId },
                        optionLabel = { it.name },
                        onSelected = { depot ->
                            if (depot.depotId != depotId) {
                                depotId = depot.depotId
                                // Clear picks that belong to another depot.
                                if (uiState.drivers.none { it.driverId == driverId && it.depotId == depot.depotId }) driverId = null
                                if (uiState.vehicles.none { it.vehicleId == vehicleId && it.depotId == depot.depotId }) vehicleId = null
                            }
                        },
                        placeholder = "Select depot",
                        isError = depotError,
                        supportingText = when {
                            depotError -> "Choose the depot the job leaves from"
                            !isEditMode && preferences.defaultDepotId != null && depotId == preferences.defaultDepotId ->
                                "Your default depot (change in Settings)"
                            else -> null
                        }
                    )
                }

                FormSection(title = "Assignment") {

                    PickerField(
                        label = "Driver",
                        options = listOf<DriverResponse?>(null) + depotDrivers,
                        selected = depotDrivers.firstOrNull { it.driverId == driverId },
                        optionLabel = { driver ->
                            if (driver == null) "Leave unassigned"
                            else "${uiState.driverName(driver.driverId)}${if (driver.isAvailable) "" else " (busy)"}"
                        },
                        onSelected = { driverId = it?.driverId },
                        placeholder = "Unassigned",
                        enabled = depotId != null,
                        supportingText = if (depotId == null) "Pick a depot first"
                        else "${depotDrivers.count { it.isAvailable }} of ${depotDrivers.size} drivers available"
                    )

                    PickerField(
                        label = "Vehicle",
                        options = listOf<VehicleResponse?>(null) + depotVehicles,
                        selected = depotVehicles.firstOrNull { it.vehicleId == vehicleId },
                        optionLabel = { vehicle ->
                            if (vehicle == null) "Leave unassigned"
                            else "${vehicle.registrationNumber} · ${vehicle.make} ${vehicle.model} (${vehicle.capacityKg.toInt()} kg)"
                        },
                        onSelected = { vehicleId = it?.vehicleId },
                        placeholder = "Unassigned",
                        enabled = depotId != null
                    )

                    if (resultingStatus != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isEditMode) "Will be saved as" else "Job will start as",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            StatusBadge(status = resultingStatus)
                        }
                    }
                }

                FormSection(title = "Schedule") {

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1.4f)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(DateFormatting.displayDate(scheduledDate))
                        }
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("%02d:%02d".format(scheduledTime.hour, scheduledTime.minute))
                        }
                    }

                    Text(
                        text = "Priority",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        JobPriority.ALL.forEach { option ->
                            FilterChip(
                                selected = priority == option,
                                onClick = { priority = option },
                                label = { Text(option) }
                            )
                        }
                    }
                }

                FormSection(title = "Extra details (optional)") {

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Notes for the driver") },
                        minLines = 2
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = estimatedDistance,
                            onValueChange = { estimatedDistance = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Distance (km)") },
                            isError = distanceError,
                            supportingText = if (distanceError) {{ Text("Enter a number") }} else null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = estimatedDuration,
                            onValueChange = { estimatedDuration = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Duration (min)") },
                            isError = durationError,
                            supportingText = if (durationError) {{ Text("Whole minutes") }} else null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }

                if (!isEditMode) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (preferences.promptForStopsAfterCreate)
                                "Next you'll add the delivery stops for this job."
                            else
                                "Add delivery stops from the Jobs list after saving.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                uiState.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error
                    )
                }

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
                            submitted = true
                            val client = clientId
                            val depot = depotId
                            if (client == null || depot == null || distanceError || durationError) {
                                return@Button
                            }

                            val isoDate = DateFormatting.toApi(scheduledDate, scheduledTime.hour, scheduledTime.minute)

                            if (isEditMode && jobId != null) {
                                viewModel.updateJob(
                                    jobId = jobId,
                                    clientId = client,
                                    depotId = depot,
                                    driverId = driverId,
                                    vehicleId = vehicleId,
                                    scheduledDate = isoDate,
                                    // Only send a status when the form itself changes it.
                                    status = resultingStatus?.takeIf { it != editingJob?.status },
                                    priority = priority,
                                    notes = notes.ifBlank { null },
                                    estimatedDistanceKm = parsedDistance,
                                    estimatedDurationMinutes = parsedDuration
                                )
                            } else {
                                viewModel.createJob(
                                    clientId = client,
                                    depotId = depot,
                                    driverId = driverId,
                                    vehicleId = vehicleId,
                                    scheduledDate = isoDate,
                                    status = resultingStatus,
                                    priority = priority,
                                    notes = notes.ifBlank { null },
                                    estimatedDistanceKm = parsedDistance,
                                    estimatedDurationMinutes = parsedDuration
                                )
                            }
                        },
                        enabled = !uiState.isSaving,
                        modifier = Modifier.weight(1.4f)
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isEditMode) "Save changes" else "Create & add stops")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = scheduledDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let {
                        scheduledDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = scheduledTime.hour,
            initialMinute = scheduledTime.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Delivery start time") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    scheduledTime = LocalTime.of(timeState.hour, timeState.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}

/** Titled card that groups related form fields. */
@Composable
internal fun FormSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}
