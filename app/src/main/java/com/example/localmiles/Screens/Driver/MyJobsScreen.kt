package com.example.localmiles.Screens.Driver

import androidx.compose.foundation.clickable
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
import com.example.localmiles.Core.Network.JobResponse

@Composable
fun MyJobsScreen(
    viewModel: DriverViewModel,
    onJobClick: (Long) -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.loadMyJobs()
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "My Jobs",
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
            if (viewModel.isLoading && viewModel.jobs.isEmpty()) {
                LoadingIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (viewModel.error != null && viewModel.jobs.isEmpty()) {
                Text(
                    text = viewModel.error ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (viewModel.jobs.isEmpty()) {
                Text(
                    text = "No jobs listed",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.jobs) { job ->
                        JobItem(
                            job = job,
                            stopCount = viewModel.stopCounts[job.jobId] ?: 0,
                            onClick = { onJobClick(job.jobId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JobItem(job: JobResponse, stopCount: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
            Text(text = "Scheduled: ${job.scheduledDate}")
            Text(text = if (stopCount == 1) "1 stop" else "$stopCount stops")
            if (job.notes != null) {
                Text(
                    text = job.notes,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2
                )
            }
        }
    }
}
