package com.example.localmiles.Screens.Dispatcher

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.localmiles.Common.Components.AppTopBar
import com.example.localmiles.Common.Components.EmptyStateCard
import com.example.localmiles.Common.Components.ErrorState
import com.example.localmiles.Common.Components.LoadingIndicator
import com.example.localmiles.Common.Components.StatusBadge
import com.example.localmiles.Core.Network.JobResponse
import com.example.localmiles.Core.Utils.AddressGeocoder
import com.example.localmiles.Core.Utils.DateFormatting
import com.example.localmiles.Core.Utils.DispatcherPreferences
import com.example.localmiles.Core.Utils.JobStatus
import kotlinx.coroutines.launch

/**
 * Dispatcher job board. Each card offers only the status changes the API
 * allows from the job's current status (Pending → Assigned → En Route →
 * Completed), and explains what's missing (driver, stops) instead of letting
 * the request fail.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobListScreen(
    viewModel: DispatcherViewModel,
    onBack: () -> Unit = {},
    onCreateJob: () -> Unit = {},
    onEditJob: (JobResponse) -> Unit = {},
    addStopsForJobId: Long? = null,
    onAddStopsPromptHandled: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val preferences by DispatcherPreferences.settings.collectAsState()

    var jobToCancel by remember { mutableStateOf<JobResponse?>(null) }
    var jobForAddStop by remember { mutableStateOf<JobResponse?>(null) }
    var isNewJobStopFlow by remember { mutableStateOf(false) }
    var pendingStatusChange by remember { mutableStateOf<Pair<JobResponse, String>?>(null) }

    var selectedStatusFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadDispatcherData()
    }

    // Step 2 of creating a job: open Add Stop for it as soon as it's loaded.
    LaunchedEffect(addStopsForJobId, uiState.jobs) {
        if (addStopsForJobId != null) {
            uiState.jobs.firstOrNull { it.jobId == addStopsForJobId }?.let { job ->
                jobForAddStop = job
                isNewJobStopFlow = true
                onAddStopsPromptHandled()
            }
        }
    }

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearInfoMessage()
        }
    }

    // Action errors go to a snackbar so they're visible
    // wherever the dispatcher has scrolled to.
    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage
        if (message != null && uiState.jobs.isNotEmpty() && jobForAddStop == null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    val query = searchQuery.trim()
    val filteredJobs = uiState.jobs
        .filter { selectedStatusFilter == null || it.status == selectedStatusFilter }
        .filter { job ->
            query.isEmpty() ||
                job.jobId.toString().contains(query) ||
                uiState.clientName(job.clientId).contains(query, ignoreCase = true) ||
                uiState.driverName(job.driverId).contains(query, ignoreCase = true)
        }
        // Open work first, soonest scheduled first.
        .sortedWith(compareBy<JobResponse> { JobStatus.isClosed(it.status) }.thenBy { it.scheduledDate })

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Delivery Jobs",
                subtitle = "${uiState.jobs.count { !JobStatus.isClosed(it.status) }} open",
                showBack = true,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.loadDispatcherData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh jobs")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateJob,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New job") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.jobs.isEmpty() ->
                    LoadingIndicator(message = "Loading jobs...")

                uiState.errorMessage != null && uiState.jobs.isEmpty() ->
                    ErrorState(
                        title = "Unable to load jobs",
                        message = uiState.errorMessage ?: "Unknown error",
                        onRetry = {
                            viewModel.clearError()
                            viewModel.loadDispatcherData()
                        }
                    )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search job #, client or driver") },
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
                                selected = selectedStatusFilter == null,
                                onClick = { selectedStatusFilter = null },
                                label = { Text("All ${uiState.jobs.size}") }
                            )
                            JobStatus.ALL.forEach { status ->
                                FilterChip(
                                    selected = selectedStatusFilter == status,
                                    onClick = {
                                        selectedStatusFilter = if (selectedStatusFilter == status) null else status
                                    },
                                    label = { Text("${JobStatus.label(status)} ${uiState.jobs.count { it.status == status }}") }
                                )
                            }
                        }
                    }

                    if (filteredJobs.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Default.Assignment,
                                title = "No jobs found",
                                message = when {
                                    query.isNotEmpty() -> "Nothing matches \"$query\"."
                                    selectedStatusFilter != null -> "There are no ${JobStatus.label(selectedStatusFilter!!)} jobs."
                                    else -> "Create a delivery job with the New job button."
                                }
                            )
                        }
                    } else {
                        items(items = filteredJobs, key = { it.jobId }) { job ->
                            JobCard(
                                job = job,
                                uiState = uiState,
                                onEdit = { onEditJob(job) },
                                onCancel = { jobToCancel = job },
                                onStatusChange = { status ->
                                    if (preferences.confirmStatusChanges) {
                                        pendingStatusChange = job to status
                                    } else {
                                        viewModel.updateJobStatus(job.jobId, status)
                                    }
                                },
                                onAddStop = {
                                    isNewJobStopFlow = false
                                    jobForAddStop = job
                                }
                            )
                        }
                    }

                    // Room for the FAB.
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }

    jobToCancel?.let { job ->
        AlertDialog(
            onDismissRequest = { jobToCancel = null },
            title = { Text("Cancel Job #${job.jobId}?") },
            text = {
                Text(
                    "${uiState.clientName(job.clientId)}'s delivery will be cancelled and moved to " +
                        "Delivery History. This can't be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cancelJob(job.jobId)
                    jobToCancel = null
                }) { Text("Cancel job", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { jobToCancel = null }) { Text("Keep job") }
            }
        )
    }

    pendingStatusChange?.let { (job, status) ->
        AlertDialog(
            onDismissRequest = { pendingStatusChange = null },
            title = { Text("${statusActionLabel(job.status, status)}?") },
            text = {
                Text(
                    "Job #${job.jobId} for ${uiState.clientName(job.clientId)} will move from " +
                        "${JobStatus.label(job.status)} to ${JobStatus.label(status)}." +
                        if (status == JobStatus.EN_ROUTE) "\n\n${uiState.driverName(job.driverId)} will see it as in progress." else ""
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateJobStatus(job.jobId, status)
                    pendingStatusChange = null
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { pendingStatusChange = null }) { Text("Not now") }
            }
        )
    }

    jobForAddStop?.let { job ->
        AddStopDialog(
            job = job,
            nextSequence = uiState.nextStopSequence(job.jobId),
            existingStops = uiState.stopCounts[job.jobId] ?: 0,
            isNewJob = isNewJobStopFlow,
            isSaving = uiState.isSavingStop,
            serverError = uiState.errorMessage,
            onDismiss = {
                viewModel.clearError()
                jobForAddStop = null
            },
            onSave = { sequence, recipient, address, latitude, longitude, instructions, addAnother ->
                viewModel.createStop(
                    jobId = job.jobId,
                    sequenceNumber = sequence,
                    recipientName = recipient,
                    address = address,
                    latitude = latitude,
                    longitude = longitude,
                    instructions = instructions,
                    onSuccess = { if (!addAnother) jobForAddStop = null }
                )
            }
        )
    }
}

/** Button text for moving a job between two statuses. */
private fun statusActionLabel(from: String, to: String): String = when (to) {
    JobStatus.ASSIGNED -> "Mark assigned"
    JobStatus.EN_ROUTE -> "Dispatch"
    JobStatus.COMPLETED -> "Mark completed"
    JobStatus.PENDING -> if (from == JobStatus.ASSIGNED) "Unassign" else "Move to Pending"
    JobStatus.CANCELLED -> "Cancel job"
    else -> "Set ${JobStatus.label(to)}"
}

@Composable
private fun JobCard(
    job: JobResponse,
    uiState: DispatcherUiState,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    onStatusChange: (String) -> Unit,
    onAddStop: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var stopsExpanded by remember { mutableStateOf(false) }

    val stops = uiState.stopsByJob[job.jobId].orEmpty()
    val stopCount = uiState.stopCounts[job.jobId] ?: stops.size
    val delivered = stops.count { it.status == "Completed" }
    val isClosed = JobStatus.isClosed(job.status)

    // The forward step for this job (never Cancelled — that's in the menu).
    val primaryNext = JobStatus.nextStatuses(job.status)
        .firstOrNull { it != JobStatus.CANCELLED && it != JobStatus.PENDING }
    val blocker = primaryNext?.let { uiState.statusBlocker(job, it) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Job #${job.jobId}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = uiState.clientName(job.clientId),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(status = job.status)

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More actions for job ${job.jobId}")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        if (!isClosed) {
                            DropdownMenuItem(
                                text = { Text("Edit details") },
                                onClick = { menuExpanded = false; onEdit() }
                            )
                            DropdownMenuItem(
                                text = { Text("Add stop") },
                                onClick = { menuExpanded = false; onAddStop() }
                            )
                        }
                        if (JobStatus.PENDING in JobStatus.nextStatuses(job.status)) {
                            DropdownMenuItem(
                                text = { Text(statusActionLabel(job.status, JobStatus.PENDING)) },
                                onClick = { menuExpanded = false; onStatusChange(JobStatus.PENDING) }
                            )
                        }
                        if (JobStatus.CANCELLED in JobStatus.nextStatuses(job.status)) {
                            DropdownMenuItem(
                                text = { Text("Cancel job", color = MaterialTheme.colorScheme.error) },
                                onClick = { menuExpanded = false; onCancel() }
                            )
                        }
                        if (isClosed) {
                            DropdownMenuItem(
                                text = { Text("No actions — job is ${JobStatus.label(job.status).lowercase()}") },
                                onClick = { menuExpanded = false },
                                enabled = false
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            JobDetailRow("Scheduled", DateFormatting.display(job.scheduledDate))
            JobDetailRow("Depot", uiState.depotName(job.depotId))
            JobDetailRow("Driver", uiState.driverName(job.driverId))
            JobDetailRow("Vehicle", uiState.vehicleName(job.vehicleId))
            JobDetailRow("Priority", job.priority)

            if (!job.notes.isNullOrBlank()) {
                Text(
                    text = job.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        stopCount == 0 -> "No stops yet"
                        stops.isNotEmpty() -> "$delivered of $stopCount stops delivered"
                        else -> if (stopCount == 1) "1 stop" else "$stopCount stops"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                if (stops.isNotEmpty()) {
                    IconButton(onClick = { stopsExpanded = !stopsExpanded }) {
                        Icon(
                            if (stopsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (stopsExpanded) "Hide stops" else "Show stops"
                        )
                    }
                }
            }

            if (stopsExpanded) {
                stops.forEach { stop ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${stop.sequenceNumber}.",
                            fontWeight = FontWeight.Bold
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stop.recipientName, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                stop.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusBadge(status = stop.status)
                    }
                }
            }

            if (!isClosed && blocker != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = blocker,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            if (!isClosed) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = if (stopCount == 0 || job.driverId != null) onAddStop else onEdit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            when {
                                stopCount == 0 -> "Add stops"
                                job.driverId == null -> "Assign driver"
                                else -> "Add stop"
                            }
                        )
                    }

                    if (primaryNext != null) {
                        Button(
                            onClick = { onStatusChange(primaryNext) },
                            enabled = blocker == null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(statusActionLabel(job.status, primaryNext))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JobDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Adds a stop with real coordinates. The address is geocoded on save; if the
 * Geocoder can't place it, the dispatcher can type the coordinates instead —
 * a stop is never saved at 0,0 (which made every POD fail the 200 m check).
 */
@Composable
private fun AddStopDialog(
    job: JobResponse,
    nextSequence: Int,
    existingStops: Int,
    isNewJob: Boolean,
    isSaving: Boolean,
    serverError: String?,
    onDismiss: () -> Unit,
    onSave: (
        sequenceNumber: Int,
        recipientName: String,
        address: String,
        latitude: Double,
        longitude: Double,
        instructions: String?,
        addAnother: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Reset the form for each new stop once the previous one is saved.
    var sequenceNumber by remember(nextSequence) { mutableStateOf(nextSequence.toString()) }
    var recipientName by remember(nextSequence) { mutableStateOf("") }
    var address by remember(nextSequence) { mutableStateOf("") }
    var instructions by remember(nextSequence) { mutableStateOf("") }
    var manualLatitude by remember(nextSequence) { mutableStateOf("") }
    var manualLongitude by remember(nextSequence) { mutableStateOf("") }
    var showManualCoordinates by remember(nextSequence) { mutableStateOf(false) }
    var isLocating by remember { mutableStateOf(false) }
    var localError by remember(nextSequence) { mutableStateOf<String?>(null) }

    val parsedSequence = sequenceNumber.toIntOrNull()?.takeIf { it > 0 }
    val manualLat = manualLatitude.toDoubleOrNull()?.takeIf { it in -90.0..90.0 && it != 0.0 }
    val manualLng = manualLongitude.toDoubleOrNull()?.takeIf { it in -180.0..180.0 && it != 0.0 }

    val isValid = parsedSequence != null &&
        recipientName.isNotBlank() &&
        address.isNotBlank() &&
        (!showManualCoordinates || (manualLat != null && manualLng != null))

    val busy = isSaving || isLocating

    fun submit(addAnother: Boolean) {
        localError = null
        val sequence = parsedSequence ?: return

        if (showManualCoordinates && manualLat != null && manualLng != null) {
            onSave(sequence, recipientName.trim(), address.trim(), manualLat, manualLng,
                instructions.trim().ifBlank { null }, addAnother)
            return
        }

        scope.launch {
            isLocating = true
            val point = AddressGeocoder.geocode(context, address.trim())
            isLocating = false
            if (point == null) {
                showManualCoordinates = true
                localError = "Couldn't find that address on the map. Check it, or enter the coordinates below."
            } else {
                onSave(sequence, recipientName.trim(), address.trim(), point.latitude, point.longitude,
                    instructions.trim().ifBlank { null }, addAnother)
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = {
            Column {
                Text(if (isNewJob) "Add stops to Job #${job.jobId}" else "Add stop to Job #${job.jobId}")
                Text(
                    text = if (isNewJob) "Step 2 of 2 · $existingStops added so far"
                    else "$existingStops stop(s) on this job",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = recipientName,
                    onValueChange = { recipientName = it },
                    label = { Text("Recipient name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = {
                        address = it
                        if (showManualCoordinates) localError = null
                    },
                    label = { Text("Delivery address *") },
                    supportingText = { Text("Street, suburb and city — used to place the stop on the map") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (showManualCoordinates) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = manualLatitude,
                            onValueChange = { manualLatitude = it },
                            label = { Text("Latitude") },
                            placeholder = { Text("-29.85") },
                            isError = manualLatitude.isNotBlank() && manualLat == null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = manualLongitude,
                            onValueChange = { manualLongitude = it },
                            label = { Text("Longitude") },
                            placeholder = { Text("31.02") },
                            isError = manualLongitude.isNotBlank() && manualLng == null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    TextButton(onClick = { showManualCoordinates = true }) {
                        Text("Enter coordinates manually")
                    }
                }

                OutlinedTextField(
                    value = sequenceNumber,
                    onValueChange = { sequenceNumber = it },
                    label = { Text("Stop order") },
                    supportingText = { Text("Drivers must complete stops in this order") },
                    isError = parsedSequence == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Instructions (optional)") },
                    placeholder = { Text("e.g. Gate code 1234, leave at reception") },
                    modifier = Modifier.fillMaxWidth()
                )

                (localError ?: serverError)?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                TextButton(
                    onClick = { submit(addAnother = true) },
                    enabled = isValid && !busy,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Save & add another")
                }
            }
        },
        confirmButton = {
            Button(
                enabled = isValid && !busy,
                onClick = { submit(addAnother = false) }
            ) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isLocating) "Locating..." else "Saving...")
                } else {
                    Text("Save stop")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text(if (isNewJob && existingStops > 0) "Done" else "Cancel")
            }
        }
    )
}
