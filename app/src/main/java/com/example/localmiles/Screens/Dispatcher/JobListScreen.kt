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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.localmiles.Core.Network.JobResponse

@Composable
fun JobListScreen(
    viewModel: DispatcherViewModel,
    onBack: () -> Unit = {},
    onCreateJob: () -> Unit = {},
    onEditJob: (JobResponse) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedJobForDelete by remember {
        mutableStateOf<JobResponse?>(null)
    }

    var selectedStatusFilter by remember {
        mutableStateOf("All")
    }

    var filterMenuExpanded by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        viewModel.loadDispatcherData()
    }

    val filteredJobs = if (selectedStatusFilter == "All") {
        uiState.jobs
    } else {
        uiState.jobs.filter {
            it.status.equals(
                selectedStatusFilter,
                ignoreCase = true
            )
        }
    }

    val pendingCount = uiState.jobs.count {
        it.status.equals("Pending", ignoreCase = true)
    }

    val assignedCount = uiState.jobs.count {
        it.status.equals("Assigned", ignoreCase = true)
    }

    val enRouteCount = uiState.jobs.count {
        it.status.equals("EnRoute", ignoreCase = true)
    }

    val completedCount = uiState.jobs.count {
        it.status.equals("Completed", ignoreCase = true)
    }

    val cancelledCount = uiState.jobs.count {
        it.status.equals("Cancelled", ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Delivery Jobs",
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
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.loadDispatcherData()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh jobs"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateJob
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create job"
                )
            }
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            when {

                uiState.isLoading && uiState.jobs.isEmpty() -> {

                    CircularProgressIndicator(
                        modifier = Modifier.align(
                            Alignment.Center
                        )
                    )
                }

                uiState.errorMessage != null && uiState.jobs.isEmpty() -> {

                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        Text(
                            text = "Unable to load jobs.",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = uiState.errorMessage
                                ?: "Unknown error"
                        )

                        Button(
                            onClick = {
                                viewModel.clearError()
                                viewModel.loadDispatcherData()
                            }
                        ) {
                            Text("Try Again")
                        }
                    }
                }

                else -> {

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        item {

                            Text(
                                text = "Job Monitoring",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(4.dp)
                            )

                            Text(
                                text = "Monitor delivery jobs and manage their operational status.",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )
                        }

                        item {

                            JobStatusSummaryCard(
                                total = uiState.jobs.size,
                                pending = pendingCount,
                                assigned = assignedCount,
                                enRoute = enRouteCount,
                                completed = completedCount,
                                cancelled = cancelledCount
                            )
                        }

                        item {

                            Box(
                                modifier = Modifier.fillMaxWidth()
                            ) {

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        filterMenuExpanded = true
                                    },
                                    elevation = CardDefaults.cardElevation(
                                        defaultElevation = 2.dp
                                    )
                                ) {

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {

                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {

                                            Text(
                                                text = "Filter jobs",
                                                fontWeight = FontWeight.Bold
                                            )

                                            Text(
                                                text = "Showing: $selectedStatusFilter"
                                            )
                                        }

                                        TextButton(
                                            onClick = {
                                                filterMenuExpanded = true
                                            }
                                        ) {
                                            Text("Change")
                                        }
                                    }
                                }

                                DropdownMenu(
                                    expanded = filterMenuExpanded,
                                    onDismissRequest = {
                                        filterMenuExpanded = false
                                    }
                                ) {

                                    listOf(
                                        "All",
                                        "Pending",
                                        "Assigned",
                                        "EnRoute",
                                        "Completed",
                                        "Cancelled"
                                    ).forEach { status ->

                                        DropdownMenuItem(
                                            text = {
                                                Text(status)
                                            },
                                            onClick = {
                                                selectedStatusFilter = status
                                                filterMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        item {

                            Text(
                                text = "${filteredJobs.size} job(s) shown",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (filteredJobs.isEmpty()) {

                            item {

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(
                                        defaultElevation = 2.dp
                                    )
                                ) {

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {

                                        Icon(
                                            imageVector = Icons.Default.Assignment,
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp)
                                        )

                                        Text(
                                            text = "No jobs found",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Text(
                                            text = if (selectedStatusFilter == "All") {
                                                "Create a new delivery job using the + button."
                                            } else {
                                                "There are no jobs with status \"$selectedStatusFilter\"."
                                            }
                                        )
                                    }
                                }
                            }

                        } else {

                            items(
                                items = filteredJobs,
                                key = { it.jobId }
                            ) { job ->

                                JobCard(
                                    job = job,
                                    onEdit = {
                                        onEditJob(job)
                                    },
                                    onDelete = {
                                        selectedJobForDelete = job
                                    },
                                    onStatusChange = { status ->
                                        viewModel.updateJobStatus(
                                            jobId = job.jobId,
                                            status = status
                                        )
                                    }
                                )
                            }
                        }

                        if (uiState.errorMessage != null) {

                            item {

                                Text(
                                    text = uiState.errorMessage
                                        ?: "An error occurred.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        item {
                            Spacer(
                                modifier = Modifier.height(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedJobForDelete != null) {

        val job = selectedJobForDelete!!

        AlertDialog(
            onDismissRequest = {
                selectedJobForDelete = null
            },
            title = {
                Text("Cancel delivery job?")
            },
            text = {
                Text(
                    "Job #${job.jobId} will be cancelled. " +
                            "This action cannot be undone from the Dispatcher screen."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {

                        viewModel.cancelJob(
                            jobId = job.jobId
                        )

                        selectedJobForDelete = null
                    }
                ) {
                    Text("Cancel Job")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedJobForDelete = null
                    }
                ) {
                    Text("Keep Job")
                }
            }
        )
    }
}

@Composable
private fun JobStatusSummaryCard(
    total: Int,
    pending: Int,
    assigned: Int,
    enRoute: Int,
    completed: Int,
    cancelled: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Job Status Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "$total total job(s)",
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                JobStatusCount(
                    modifier = Modifier.weight(1f),
                    label = "Pending",
                    count = pending
                )

                JobStatusCount(
                    modifier = Modifier.weight(1f),
                    label = "Assigned",
                    count = assigned
                )

                JobStatusCount(
                    modifier = Modifier.weight(1f),
                    label = "EnRoute",
                    count = enRoute
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                JobStatusCount(
                    modifier = Modifier.weight(1f),
                    label = "Completed",
                    count = completed
                )

                JobStatusCount(
                    modifier = Modifier.weight(1f),
                    label = "Cancelled",
                    count = cancelled
                )

                Spacer(
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun JobStatusCount(
    modifier: Modifier = Modifier,
    label: String,
    count: Int
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun JobCard(
    job: JobResponse,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStatusChange: (String) -> Unit
) {

    var statusMenuExpanded by remember {
        mutableStateOf(false)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.Assignment,
                    contentDescription = null
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {

                    Text(
                        text = "Job #${job.jobId}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Client ID: ${job.clientId}"
                    )
                }

                IconButton(
                    onClick = onEdit
                ) {

                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit job"
                    )
                }

                IconButton(
                    onClick = onDelete
                ) {

                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Cancel job"
                    )
                }
            }

            Text(
                text = "Depot ID: ${job.depotId}"
            )

            Text(
                text = "Driver: ${
                    job.driverId?.toString() ?: "Unassigned"
                }"
            )

            Text(
                text = "Vehicle: ${
                    job.vehicleId?.toString() ?: "Unassigned"
                }"
            )

            Text(
                text = "Scheduled: ${job.scheduledDate}"
            )

            Text(
                text = "Priority: ${job.priority}",
                fontWeight = FontWeight.Medium
            )

            Box {

                TextButton(
                    onClick = {
                        statusMenuExpanded = true
                    }
                ) {
                    Text(
                        text = "Status: ${job.status}"
                    )
                }

                DropdownMenu(
                    expanded = statusMenuExpanded,
                    onDismissRequest = {
                        statusMenuExpanded = false
                    }
                ) {

                    val statuses = listOf(
                        "Pending",
                        "Assigned",
                        "EnRoute",
                        "Completed",
                        "Cancelled"
                    )

                    statuses.forEach { status ->

                        DropdownMenuItem(
                            text = {
                                Text(status)
                            },
                            onClick = {

                                statusMenuExpanded = false

                                if (status != job.status) {

                                    onStatusChange(status)
                                }
                            }
                        )
                    }
                }
            }

            if (!job.notes.isNullOrBlank()) {

                Text(
                    text = "Notes: ${job.notes}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}