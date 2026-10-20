package com.example.localmiles.Screens.Dispatcher

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.localmiles.Common.Components.AppTopBar
import com.example.localmiles.Common.Components.EmptyStateCard
import com.example.localmiles.Common.Components.ErrorState
import com.example.localmiles.Common.Components.LoadingIndicator
import com.example.localmiles.Common.Components.PickerField
import com.example.localmiles.Common.Components.StatusBadge
import com.example.localmiles.Core.Network.Dto.ClientResponse
import com.example.localmiles.Core.Network.Dto.DriverResponse
import com.example.localmiles.Core.Network.JobResponse
import com.example.localmiles.Core.Utils.DateFormatting
import com.example.localmiles.Core.Utils.DispatcherPreferences
import com.example.localmiles.Core.Utils.HistoryRange
import com.example.localmiles.Core.Utils.JobPriority
import com.example.localmiles.Core.Utils.JobStatus

/**
 * Completed and cancelled deliveries, filterable by search text, outcome, date
 * range (default from Dispatcher Settings), driver, client and priority.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryHistoryScreen(
    viewModel: DispatcherViewModel,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val preferences by DispatcherPreferences.settings.collectAsState()

    var filter by remember { mutableStateOf(HistoryFilter(range = preferences.historyRange)) }
    var showMoreFilters by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadDispatcherData()
    }

    val historyJobs = filterHistory(uiState.jobs, filter) { job ->
        "${uiState.clientName(job.clientId)} ${uiState.driverName(job.driverId)}"
    }
    val completed = historyJobs.count { it.status == JobStatus.COMPLETED }
    val cancelled = historyJobs.count { it.status == JobStatus.CANCELLED }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Delivery History",
                subtitle = "${historyJobs.size} result(s)",
                showBack = true,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.loadDispatcherData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh history")
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
            when {
                uiState.isLoading && uiState.jobs.isEmpty() ->
                    LoadingIndicator(message = "Loading delivery history...")

                uiState.errorMessage != null && uiState.jobs.isEmpty() ->
                    ErrorState(
                        title = "Unable to load delivery history",
                        message = uiState.errorMessage ?: "Unknown error",
                        onRetry = { viewModel.loadDispatcherData() }
                    )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    item {
                        OutlinedTextField(
                            value = filter.query,
                            onValueChange = { filter = filter.copy(query = it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search job #, client, driver or notes") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (filter.query.isNotEmpty()) {
                                    IconButton(onClick = { filter = filter.copy(query = "") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true
                        )
                    }

                    item {
                        ChipRow {
                            FilterChip(
                                selected = filter.status == null,
                                onClick = { filter = filter.copy(status = null) },
                                label = { Text("All outcomes") }
                            )
                            FilterChip(
                                selected = filter.status == JobStatus.COMPLETED,
                                onClick = { filter = filter.copy(status = JobStatus.COMPLETED) },
                                label = { Text("Completed") }
                            )
                            FilterChip(
                                selected = filter.status == JobStatus.CANCELLED,
                                onClick = { filter = filter.copy(status = JobStatus.CANCELLED) },
                                label = { Text("Cancelled") }
                            )
                        }
                    }

                    item {
                        ChipRow {
                            HistoryRange.entries.forEach { range ->
                                FilterChip(
                                    selected = filter.range == range,
                                    onClick = { filter = filter.copy(range = range) },
                                    label = { Text(range.label) }
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showMoreFilters = !showMoreFilters }) {
                                Icon(Icons.Default.Tune, contentDescription = null)
                                Text(
                                    text = " More filters" +
                                        listOfNotNull(filter.driverId, filter.clientId, filter.priority)
                                            .size.takeIf { it > 0 }?.let { " ($it)" }.orEmpty()
                                )
                            }
                            TextButton(onClick = {
                                filter = filter.copy(
                                    sort = if (filter.sort == HistorySort.NEWEST) HistorySort.OLDEST else HistorySort.NEWEST
                                )
                            }) {
                                Icon(Icons.Default.SwapVert, contentDescription = null)
                                Text(" ${filter.sort.label}")
                            }
                            if (filter.activeFilterCount > 0) {
                                TextButton(onClick = {
                                    filter = HistoryFilter(range = preferences.historyRange)
                                }) {
                                    Text("Reset")
                                }
                            }
                        }
                    }

                    if (showMoreFilters) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                PickerField(
                                    label = "Driver",
                                    options = listOf<DriverResponse?>(null) + uiState.drivers,
                                    selected = uiState.drivers.firstOrNull { it.driverId == filter.driverId },
                                    optionLabel = { it?.let { d -> uiState.driverName(d.driverId) } ?: "Any driver" },
                                    onSelected = { filter = filter.copy(driverId = it?.driverId) },
                                    placeholder = "Any driver"
                                )
                                PickerField(
                                    label = "Client",
                                    options = listOf<ClientResponse?>(null) + uiState.clients,
                                    selected = uiState.clients.firstOrNull { it.clientId == filter.clientId },
                                    optionLabel = { it?.companyName ?: "Any client" },
                                    onSelected = { filter = filter.copy(clientId = it?.clientId) },
                                    placeholder = "Any client"
                                )
                                ChipRow {
                                    FilterChip(
                                        selected = filter.priority == null,
                                        onClick = { filter = filter.copy(priority = null) },
                                        label = { Text("Any priority") }
                                    )
                                    JobPriority.ALL.forEach { priority ->
                                        FilterChip(
                                            selected = filter.priority == priority,
                                            onClick = { filter = filter.copy(priority = priority) },
                                            label = { Text(priority) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "$completed completed · $cancelled cancelled" +
                                (filter.range.days?.let { " · last $it days" } ?: ""),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (historyJobs.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Default.History,
                                title = "No deliveries match",
                                message = if (filter.activeFilterCount > 0 || filter.range != HistoryRange.ALL_TIME)
                                    "Try a wider date range or clear some filters."
                                else
                                    "Completed or cancelled jobs will appear here."
                            )
                        }
                    } else {
                        items(historyJobs, key = { it.jobId }) { job ->
                            DeliveryHistoryCard(job = job, uiState = uiState)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@Composable
private fun DeliveryHistoryCard(
    job: JobResponse,
    uiState: DispatcherUiState
) {
    var expanded by remember { mutableStateOf(false) }
    val stops = uiState.stopsByJob[job.jobId].orEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded },
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
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Hide details" else "Show details"
                )
            }

            Text(
                text = "${DateFormatting.display(job.scheduledDate)} · ${uiState.driverName(job.driverId)}",
                style = MaterialTheme.typography.bodyMedium
            )

            if (expanded) {
                HorizontalDivider()
                HistoryDetail("Depot", uiState.depotName(job.depotId))
                HistoryDetail("Vehicle", uiState.vehicleName(job.vehicleId))
                HistoryDetail("Priority", job.priority)
                job.estimatedDistanceKm?.let { HistoryDetail("Est. distance", "$it km") }
                job.estimatedDurationMinutes?.let { HistoryDetail("Est. duration", "$it min") }
                if (!job.notes.isNullOrBlank()) HistoryDetail("Notes", job.notes)

                if (stops.isNotEmpty()) {
                    Text(
                        text = "Stops",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    stops.forEach { stop ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${stop.sequenceNumber}. ${stop.recipientName}")
                                Text(
                                    text = stop.completedAt?.let { "Delivered ${DateFormatting.display(it)}" }
                                        ?: stop.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusBadge(status = stop.status)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryDetail(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
