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
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import com.example.localmiles.Core.Network.Dto.DriverResponse
import com.example.localmiles.Core.Network.Dto.VehicleResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetManagementScreen(
    viewModel: DispatcherViewModel,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDispatcherData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Fleet Monitoring",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                            contentDescription = "Refresh fleet"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        when {
            uiState.isLoading &&
                    uiState.drivers.isEmpty() &&
                    uiState.vehicles.isEmpty() -> {

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
                        text = "Loading fleet information..."
                    )
                }
            }

            uiState.errorMessage != null &&
                    uiState.drivers.isEmpty() &&
                    uiState.vehicles.isEmpty() -> {

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
                        text = "Unable to load fleet information.",
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
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    item {
                        Spacer(
                            modifier = Modifier.size(4.dp)
                        )

                        Text(
                            text = "Fleet Overview",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.size(4.dp)
                        )

                        Text(
                            text = "Monitor drivers, vehicles and availability.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    item {
                        FleetSummaryCard(
                            drivers = uiState.drivers,
                            vehicles = uiState.vehicles
                        )
                    }

                    item {
                        Text(
                            text = "Drivers",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (uiState.drivers.isEmpty()) {
                        item {
                            Text(
                                text = "No drivers were returned by the server.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(
                            items = uiState.drivers,
                            key = { it.driverId }
                        ) { driver ->

                            val vehicle = uiState.vehicles.firstOrNull {
                                it.depotId == driver.depotId
                            }

                            FleetDriverCard(
                                driver = driver,
                                vehicle = vehicle
                            )
                        }
                    }

                    item {
                        Spacer(
                            modifier = Modifier.size(4.dp)
                        )

                        Text(
                            text = "Vehicles",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (uiState.vehicles.isEmpty()) {
                        item {
                            Text(
                                text = "No vehicles were returned by the server.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(
                            items = uiState.vehicles,
                            key = { it.vehicleId }
                        ) { vehicle ->

                            FleetVehicleCard(
                                vehicle = vehicle,
                                onToggleActive = { active ->
                                    viewModel.setVehicleActive(
                                        vehicleId = vehicle.vehicleId,
                                        active = active
                                    )
                                }
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
private fun FleetSummaryCard(
    drivers: List<DriverResponse>,
    vehicles: List<VehicleResponse>
) {
    val activeDrivers = drivers.count { it.isActive }
    val availableDrivers = drivers.count {
        it.isActive && it.isAvailable
    }

    val activeVehicles = vehicles.count { it.isActive }
    val availableVehicles = vehicles.count {
        it.isActive && it.isAvailable
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Fleet Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                FleetSummaryItem(
                    modifier = Modifier.weight(1f),
                    title = "Drivers",
                    value = "$activeDrivers",
                    detail = "$availableDrivers available"
                )

                FleetSummaryItem(
                    modifier = Modifier.weight(1f),
                    title = "Vehicles",
                    value = "$activeVehicles",
                    detail = "$availableVehicles available"
                )
            }
        }
    }
}

@Composable
private fun FleetSummaryItem(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    detail: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun FleetDriverCard(
    driver: DriverResponse,
    vehicle: VehicleResponse?
) {
    val statusText = when {
        !driver.isActive -> "Inactive"
        driver.isAvailable -> "Available"
        else -> "Unavailable"
    }

    val statusIcon = when {
        driver.isActive && driver.isAvailable -> Icons.Default.CheckCircle
        else -> Icons.Default.Warning
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
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = "Driver #${driver.driverId}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Employee: ${driver.employeeNumber}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )

                    Text(
                        text = statusText,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider()

            Text(
                text = "Phone: ${driver.phoneNumber}"
            )

            Text(
                text = "Licence: ${driver.licenceNumber}"
            )

            Text(
                text = "Depot ID: ${driver.depotId}"
            )

            if (vehicle != null) {
                Text(
                    text = "Vehicle: ${vehicle.registrationNumber}"
                )

                Text(
                    text = "${vehicle.make} ${vehicle.model}"
                )
            } else {
                Text(
                    text = "Vehicle: No vehicle information available",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FleetVehicleCard(
    vehicle: VehicleResponse,
    onToggleActive: (Boolean) -> Unit
) {
    val statusText = when {
        !vehicle.isActive -> "Inactive"
        vehicle.isAvailable -> "Available"
        else -> "Unavailable"
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
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = vehicle.registrationNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${vehicle.make} ${vehicle.model}"
                    )
                }

                Text(
                    text = statusText,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider()

            Text(
                text = "Vehicle ID: ${vehicle.vehicleId}"
            )

            Text(
                text = "Type: ${vehicle.vehicleType}"
            )

            Text(
                text = "Capacity: ${vehicle.capacityKg} kg"
            )

            Text(
                text = "Depot ID: ${vehicle.depotId}"
            )

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Active",
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = if (vehicle.isActive) {
                            "This vehicle can be assigned to jobs."
                        } else {
                            "This vehicle is hidden from job assignment."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = vehicle.isActive,
                    onCheckedChange = onToggleActive
                )
            }
        }
    }
}