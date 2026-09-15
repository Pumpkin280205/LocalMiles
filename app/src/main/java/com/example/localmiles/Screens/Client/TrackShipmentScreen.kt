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
import androidx.compose.material3.HorizontalDivider
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
import com.example.localmiles.Core.Network.Dto.StopResponse

// Displays tracking information for a delivery selected from the client dashboard
@Composable
fun TrackShipmentScreen(
    jobId: Long,
    viewModel: ClientViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Loads the selected delivery and its stops when the screen opens
    LaunchedEffect(jobId) {
        viewModel.loadShipmentTracking(jobId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Allows the client to return to the dashboard
        TextButton(
            onClick = onBack
        ) {
            Text("Back")
        }

        Text(
            text = "Track Shipment",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {

            // Display a loading indicator while tracking data is requested
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            // Allow the client to retry if the tracking request fails
            uiState.errorMessage != null -> {
                Text(
                    text = uiState.errorMessage ?: "Unable to load shipment.",
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.loadShipmentTracking(jobId)
                    }
                ) {
                    Text("Try Again")
                }
            }

            uiState.selectedJob != null -> {
                val job = uiState.selectedJob!!

                // Summary of the selected delivery.
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Delivery #${job.jobId}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        TrackingDetailRow(
                            label = "Status",
                            value = job.status
                        )

                        TrackingDetailRow(
                            label = "Priority",
                            value = job.priority
                        )

                        TrackingDetailRow(
                            label = "Scheduled",
                            value = job.scheduledDate
                        )

                        job.estimatedDistanceKm?.let { distance ->
                            TrackingDetailRow(
                                label = "Estimated distance",
                                value = "$distance km"
                            )
                        }

                        job.estimatedDurationMinutes?.let { duration ->
                            TrackingDetailRow(
                                label = "Estimated duration",
                                value = "$duration minutes"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Delivery Progress",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.stops.isEmpty()) {
                    Text(
                        text = "No delivery stops are available for this shipment.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.stops,
                            key = { stop -> stop.stopId }
                        ) { stop ->
                            DeliveryStopCard(stop = stop)
                        }
                    }
                }
            }

            else -> {
                Text(
                    text = "Shipment information is unavailable.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// Displays a label and value in the shipment summary
@Composable
private fun TrackingDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold
        )

        Text(text = value)
    }
}

// Displays one stop in the shipment's delivery sequence
@Composable
private fun DeliveryStopCard(
    stop: StopResponse
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stop ${stop.sequenceNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = stop.status,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stop.recipientName,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = stop.address,
                style = MaterialTheme.typography.bodyMedium
            )

            // Displays the delivery window only when it was supplied by the API
            if (
                stop.deliveryWindowStart != null ||
                stop.deliveryWindowEnd != null
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Delivery Window",
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "${stop.deliveryWindowStart ?: "Not specified"} - " +
                            (stop.deliveryWindowEnd ?: "Not specified")
                )
            }

            // Special delivery instructions are optional
            stop.instructions?.takeIf { it.isNotBlank() }?.let { instructions ->
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Instructions",
                    fontWeight = FontWeight.SemiBold
                )

                Text(text = instructions)
            }

            stop.arrivedAt?.let { arrived ->
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Arrived: $arrived",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            stop.completedAt?.let { completed ->
                Text(
                    text = "Completed: $completed",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}