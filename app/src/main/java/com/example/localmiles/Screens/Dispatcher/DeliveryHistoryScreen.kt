package com.example.localmiles.Screens.Dispatcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localmiles.Core.Network.JobResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryHistoryScreen(
    viewModel: DispatcherViewModel,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDispatcherData()
    }

    val historyJobs = uiState.jobs.filter { job ->
        job.status.equals("Completed", ignoreCase = true) ||
                job.status.equals("Cancelled", ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Delivery History",
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
                            contentDescription = "Refresh history"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        when {
            uiState.isLoading && uiState.jobs.isEmpty() -> {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()

                    Spacer(
                        modifier = Modifier.size(12.dp)
                    )

                    Text(
                        text = "Loading delivery history..."
                    )
                }
            }

            uiState.errorMessage != null && uiState.jobs.isEmpty() -> {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )

                    Spacer(
                        modifier = Modifier.size(12.dp)
                    )

                    Text(
                        text = "Unable to load delivery history.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.size(8.dp)
                    )

                    Text(
                        text = uiState.errorMessage ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error
                    )

                    Spacer(
                        modifier = Modifier.size(16.dp)
                    )

                    Button(
                        onClick = {
                            viewModel.loadDispatcherData()
                        }
                    ) {
                        Text("Retry")
                    }
                }
            }

            else -> {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    item {
                        Text(
                            text = "Completed & Cancelled Deliveries",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.size(4.dp)
                        )

                        Text(
                            text = "${historyJobs.size} historical job(s) found.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (historyJobs.isEmpty()) {

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
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp)
                                    )

                                    Text(
                                        text = "No historical deliveries found.",
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        text = "Completed or cancelled jobs will appear here."
                                    )
                                }
                            }
                        }

                    } else {

                        items(
                            items = historyJobs,
                            key = { it.jobId }
                        ) { job ->

                            DeliveryHistoryCard(
                                job = job
                            )
                        }
                    }

                    if (uiState.errorMessage != null) {
                        item {
                            Text(
                                text = uiState.errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    item {
                        Spacer(
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeliveryHistoryCard(
    job: JobResponse
) {
    val isCompleted = job.status.equals(
        "Completed",
        ignoreCase = true
    )

    val statusIcon = if (isCompleted) {
        Icons.Default.CheckCircle
    } else {
        Icons.Default.Error
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
                    imageVector = statusIcon,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp)
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

                Text(
                    text = job.status,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Driver ID: ${job.driverId ?: "Unassigned"}"
            )

            Text(
                text = "Vehicle ID: ${job.vehicleId ?: "Unassigned"}"
            )

            Text(
                text = "Depot ID: ${job.depotId}"
            )

            Text(
                text = "Scheduled: ${job.scheduledDate}"
            )

            Text(
                text = "Priority: ${job.priority}"
            )

            if (!job.notes.isNullOrBlank()) {
                Text(
                    text = "Notes: ${job.notes}"
                )
            }

            if (job.estimatedDistanceKm != null) {
                Text(
                    text = "Estimated distance: ${job.estimatedDistanceKm} km"
                )
            }

            if (job.estimatedDurationMinutes != null) {
                Text(
                    text = "Estimated duration: ${job.estimatedDurationMinutes} minutes"
                )
            }
        }
    }
}