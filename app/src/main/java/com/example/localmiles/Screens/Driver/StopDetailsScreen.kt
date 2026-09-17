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
                title = "Job #$jobId Stops",
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
            if (viewModel.isLoading && viewModel.currentJobStops.isEmpty()) {
                LoadingIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (viewModel.error != null && viewModel.currentJobStops.isEmpty()) {
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
                    items(viewModel.currentJobStops) { stop ->
                        StopItem(
                            stop = stop,
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
fun StopItem(
    stop: StopResponse,
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
                    text = stop.address,
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
                if (stop.status == "Assigned") {
                    Button(onClick = { onUpdateStatus("EnRoute") }) {
                        Text("Start Delivery")
                    }
                } else if (stop.status == "EnRoute") {
                    Button(onClick = onCapturePod) {
                        Text("Capture POD")
                    }
                }
            }
        }
    }
}
