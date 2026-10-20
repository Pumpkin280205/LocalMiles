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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import com.example.localmiles.Core.Network.Dto.DepotResponse
import com.example.localmiles.Core.Network.Dto.DriverResponse
import com.example.localmiles.Core.Network.Dto.VehicleResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetManagementScreen(
    viewModel: DispatcherViewModel,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    var showAddVehicleDialog by remember {
        mutableStateOf(false)
    }

    var editingVehicle by remember {
        mutableStateOf<VehicleResponse?>(null)
    }

    LaunchedEffect(Unit) {
        viewModel.loadDispatcherData()
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Fleet Management",
                subtitle = "Drivers and vehicles",
                showBack = true,
                onBack = onBack,
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showAddVehicleDialog = true
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add vehicle"
                )
            }
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
                                vehicle = vehicle,
                                name = uiState.driverName(driver.driverId),
                                depotName = uiState.depotName(driver.depotId)
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
                                depotName = uiState.depotName(vehicle.depotId),
                                onToggleActive = { active ->
                                    viewModel.setVehicleActive(
                                        vehicleId = vehicle.vehicleId,
                                        active = active
                                    )
                                },
                                onEdit = {
                                    editingVehicle = vehicle
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

    if (showAddVehicleDialog) {
        VehicleFormDialog(
            vehicle = null,
            depots = uiState.depots,
            isSaving = uiState.isSavingVehicle,
            errorMessage = uiState.errorMessage,
            onDismiss = {
                showAddVehicleDialog = false
            },
            onSubmit = { depotId, registrationNumber, make, model, vehicleType, capacityKg ->
                viewModel.createVehicle(
                    depotId = depotId,
                    registrationNumber = registrationNumber,
                    make = make,
                    model = model,
                    vehicleType = vehicleType,
                    capacityKg = capacityKg,
                    onSuccess = {
                        showAddVehicleDialog = false
                    }
                )
            }
        )
    }

    if (editingVehicle != null) {
        val vehicle = editingVehicle!!

        VehicleFormDialog(
            vehicle = vehicle,
            depots = uiState.depots,
            isSaving = uiState.isSavingVehicle,
            errorMessage = uiState.errorMessage,
            onDismiss = {
                editingVehicle = null
            },
            onSubmit = { depotId, registrationNumber, make, model, vehicleType, capacityKg ->
                viewModel.updateVehicleDetails(
                    vehicleId = vehicle.vehicleId,
                    depotId = depotId,
                    registrationNumber = registrationNumber,
                    make = make,
                    model = model,
                    vehicleType = vehicleType,
                    capacityKg = capacityKg,
                    onSuccess = {
                        editingVehicle = null
                    }
                )
            }
        )
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
    vehicle: VehicleResponse?,
    name: String,
    depotName: String
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
                        text = name,
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
                text = "Depot: $depotName"
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
    depotName: String,
    onToggleActive: (Boolean) -> Unit,
    onEdit: () -> Unit
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

                IconButton(
                    onClick = onEdit
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit vehicle"
                    )
                }
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
                text = "Depot: $depotName"
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

/**
 * Add/Edit form for a vehicle. [vehicle] is null when adding a new one, or the
 * vehicle being edited otherwise.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehicleFormDialog(
    vehicle: VehicleResponse?,
    depots: List<DepotResponse>,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (
        depotId: Long,
        registrationNumber: String,
        make: String,
        model: String,
        vehicleType: String,
        capacityKg: Double
    ) -> Unit
) {
    val isEditMode = vehicle != null

    var depotMenuExpanded by remember {
        mutableStateOf(false)
    }

    var selectedDepot by remember(depots, vehicle) {
        mutableStateOf(
            depots.firstOrNull { it.depotId == vehicle?.depotId }
                ?: depots.firstOrNull()
        )
    }

    var registrationNumber by remember(vehicle) {
        mutableStateOf(vehicle?.registrationNumber ?: "")
    }

    var make by remember(vehicle) {
        mutableStateOf(vehicle?.make ?: "")
    }

    var model by remember(vehicle) {
        mutableStateOf(vehicle?.model ?: "")
    }

    var vehicleType by remember(vehicle) {
        mutableStateOf(vehicle?.vehicleType ?: "")
    }

    var capacityKg by remember(vehicle) {
        mutableStateOf(vehicle?.capacityKg?.toString() ?: "")
    }

    val canSubmit = selectedDepot != null &&
            registrationNumber.isNotBlank() &&
            make.isNotBlank() &&
            model.isNotBlank() &&
            vehicleType.isNotBlank() &&
            capacityKg.toDoubleOrNull() != null &&
            !isSaving

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) onDismiss()
        },
        title = {
            Text(if (isEditMode) "Edit vehicle" else "Add vehicle")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                ExposedDropdownMenuBox(
                    expanded = depotMenuExpanded,
                    onExpandedChange = { depotMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedDepot?.name ?: "No active depots",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Depot") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = depotMenuExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = depotMenuExpanded,
                        onDismissRequest = { depotMenuExpanded = false }
                    ) {
                        depots.forEach { depot ->
                            DropdownMenuItem(
                                text = { Text(depot.name) },
                                onClick = {
                                    selectedDepot = depot
                                    depotMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = registrationNumber,
                    onValueChange = { registrationNumber = it },
                    label = { Text("Registration number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = make,
                    onValueChange = { make = it },
                    label = { Text("Make") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = vehicleType,
                    onValueChange = { vehicleType = it },
                    label = { Text("Vehicle type") },
                    placeholder = { Text("Truck, Van, ...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = capacityKg,
                    onValueChange = { capacityKg = it },
                    label = { Text("Capacity (kg)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSubmit,
                onClick = {
                    val depotId = selectedDepot?.depotId ?: return@TextButton
                    val capacity = capacityKg.toDoubleOrNull() ?: return@TextButton

                    onSubmit(
                        depotId,
                        registrationNumber.trim(),
                        make.trim(),
                        model.trim(),
                        vehicleType.trim(),
                        capacity
                    )
                }
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text(if (isEditMode) "Save Changes" else "Add Vehicle")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("Cancel")
            }
        }
    )
}