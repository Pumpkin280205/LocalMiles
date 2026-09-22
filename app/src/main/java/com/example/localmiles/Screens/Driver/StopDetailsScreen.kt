package com.example.localmiles.Screens.Driver

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.localmiles.Common.Components.AppTopBar
import com.example.localmiles.Common.Components.LoadingIndicator
import com.example.localmiles.Common.Components.StatusBadge
import com.example.localmiles.Core.Network.Dto.StopResponse
import com.example.localmiles.Core.Network.JobResponse

@Composable
fun StopDetailsScreen(
    jobId: Long,
    viewModel: DriverViewModel,
    onStopClick: (Long) -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(jobId) {
        viewModel.loadStopsForJob(jobId)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Job #$jobId",
                showBack = true,
                onBack = onBack
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (viewModel.isLoading && viewModel.currentJobStops.isEmpty() && viewModel.currentJob == null) {
                LoadingIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (viewModel.error != null && viewModel.currentJobStops.isEmpty() && viewModel.currentJob == null) {
                Text(
                    text = viewModel.error ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.currentJob?.let { job ->
                        item {
                            JobDetailsCard(
                                job = job,
                                onStartJob = { viewModel.updateJobStatus(jobId, "EnRoute") },
                            )
                        }

                        item {
                            val sortedStops = viewModel.currentJobStops.sortedBy { it.sequenceNumber }
                            val total = sortedStops.size
                            val activeIndex = sortedStops.indexOfFirst {
                                it.status != "Completed" && it.status != "Cancelled"
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Stops",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (activeIndex == -1) {
                                        "All $total stops completed"
                                    } else {
                                        "Stop ${activeIndex + 1} of $total"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }

                    val sortedStops = viewModel.currentJobStops.sortedBy { it.sequenceNumber }
                    val activeStopId = sortedStops.firstOrNull {
                        it.status != "Completed" && it.status != "Cancelled"
                    }?.stopId

                    items(sortedStops) { stop ->
                        StopItem(
                            stop = stop,
                            isActive = stop.stopId == activeStopId,
                            onUpdateStatus = { status ->
                                viewModel.updateStopStatus(stop.stopId, jobId, status)
                            },
                            onCapturePod = {
                                onStopClick(stop.stopId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JobDetailsCard(job: JobResponse, onStartJob: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Job #${job.jobId}",
                    style = MaterialTheme.typography.titleMedium
                )
                StatusBadge(status = job.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Client ID: ${job.clientId}")
            Text(text = "Depot ID: ${job.depotId}")
            Text(
                text = "Vehicle: ${job.vehicleId?.toString() ?: "Unassigned"}"
            )
            Text(text = "Scheduled: ${job.scheduledDate}")
            Text(text = "Priority: ${job.priority}")

            job.estimatedDistanceKm?.let { distance ->
                Text(text = "Distance: $distance km")
            }

            job.estimatedDurationMinutes?.let { duration ->
                Text(text = "Estimated time: $duration min")
            }

            if (!job.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Notes: ${job.notes}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Only Assigned -> EnRoute is a valid driver-initiated transition on the backend.
            if (job.status == "Assigned") {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onStartJob,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Start Job")
                }
            }
        }
    }
}

@Composable
fun StopItem(
    stop: StopResponse,
    isActive: Boolean,
    onUpdateStatus: (String) -> Unit,
    onCapturePod: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${stop.sequenceNumber} ${stop.address}",
                    style = MaterialTheme.typography.titleMedium
                )
                StatusBadge(status = stop.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Customer: ${stop.recipientName}")
            if (stop.instructions != null) {
                Text(
                    text = stop.instructions,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Only the next stop in the dispatcher's sequence is actionable —
                // stops must be completed in order.
                if (!isActive && stop.status != "Completed" && stop.status != "Cancelled") {
                    Text(
                        text = "Waiting for earlier stops",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (isActive && stop.status == "Pending") {
                    Button(onClick = { onUpdateStatus("Arrived") }) {
                        Text("Mark Arrived")
                    }
                } else if (isActive && stop.status == "Arrived") {
                    Button(onClick = onCapturePod) {
                        Text("Capture POD")
                    }
                }
            }
        }
    }
}
