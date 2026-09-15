package com.example.localmiles.Screens.Client

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localmiles.Core.Network.JobResponse
import androidx.compose.ui.tooling.preview.Preview

// Main dashboard displayed after a client successfully logs in
@Composable
fun ClientDashboardScreen(
    viewModel: ClientViewModel,
    onTrackShipment: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Load the authenticated client's deliveries when the screen first opens
    LaunchedEffect(Unit) {
        viewModel.loadClientJobs()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Dashboard heading and settings navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "My Deliveries",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Track and manage your shipments",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Client account actions
            Column(
                horizontalAlignment = Alignment.End
            ) {
                TextButton(onClick = onOpenSettings) {
                    Text("Settings")
                }

                TextButton(onClick = onLogout) {
                    Text("Log out")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        when {
            // Shows a progress indicator while jobs are being requested
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            // Shows an error message and allow the client to retry
            uiState.errorMessage != null -> {
                Text(
                    text = uiState.errorMessage ?: "Unable to load deliveries.",
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.loadClientJobs() }
                ) {
                    Text("Try Again")
                }
            }

            // Provide a useful empty state when the client has no deliveries
            uiState.jobs.isEmpty() -> {
                Text(
                    text = "No deliveries found.",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Your assigned shipments will appear here.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Display all deliveries returned for the authenticated client.
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.jobs,
                        key = { job -> job.jobId }
                    ) { job ->
                        ClientDeliveryCard(
                            job = job,
                            onTrackShipment = onTrackShipment
                        )
                    }
                }
            }
        }
    }
}

// Displays a summary of one delivery on the client dashboard
@Composable
private fun ClientDeliveryCard(
    job: JobResponse,
    onTrackShipment: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Delivery #${job.jobId}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Status: ${job.status}",
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Priority: ${job.priority}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Scheduled: ${job.scheduledDate}",
                style = MaterialTheme.typography.bodyMedium
            )

            job.estimatedDistanceKm?.let { distance ->
                Text(
                    text = "Estimated distance: $distance km",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            job.estimatedDurationMinutes?.let { duration ->
                Text(
                    text = "Estimated duration: $duration minutes",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    onTrackShipment(job.jobId)
                }
            ) {
                Text("Track Shipment")
            }


        }
    }
}

// Preview used only during development to check the appearance
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ClientDeliveryCardPreview() {

    val sampleJob = JobResponse(
        jobId = 1001,
        clientId = 1,
        driverId = 4,
        vehicleId = 2,
        depotId = 1,
        scheduledDate = "2026-09-15 14:30",
        status = "EnRoute",
        priority = "High",
        notes = "Handle with care",
        estimatedDistanceKm = 18.5,
        estimatedDurationMinutes = 35,
        createdByUserId = 1,
        createdAt = "2026-09-15 09:00"
    )

    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "My Deliveries",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            ClientDeliveryCard(
                job = sampleJob,
                onTrackShipment = {}
            )
        }
    }
}
