package com.example.localmiles.Screens.Dispatcher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localmiles.Core.Network.Dto.DepotResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveMapScreen(
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
                        text = "Live Map",
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
                            contentDescription = "Refresh map"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        when {
            uiState.isLoading && uiState.depots.isEmpty() -> {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text = "Loading map locations..."
                    )
                }
            }

            uiState.errorMessage != null && uiState.depots.isEmpty() -> {

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
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text = "Unable to load map locations.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = uiState.errorMessage ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
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

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Text(
                        text = "Depot Locations",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Map pins represent active depot locations returned by the backend.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 3.dp
                        )
                    ) {

                        DepotMap(
                            depots = uiState.depots.filter { it.isActive }
                        )
                    }

                    Text(
                        text = "Locations",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.8f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        if (uiState.depots.isEmpty()) {
                            item {
                                Text(
                                    text = "No depot locations were returned by the server.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {

                            items(
                                items = uiState.depots.filter { it.isActive },
                                key = { it.depotId }
                            ) { depot ->

                                DepotLocationCard(
                                    depot = depot
                                )
                            }
                        }
                    }

                    if (uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DepotMap(
    depots: List<DepotResponse>
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.surfaceVariant
            )
    ) {

        if (depots.isEmpty()) {
            Text(
                text = "No active depot locations available.",
                modifier = Modifier.align(Alignment.Center),
                fontWeight = FontWeight.Bold
            )

            return
        }

        val minLatitude = depots.minOf { it.latitude }
        val maxLatitude = depots.maxOf { it.latitude }
        val minLongitude = depots.minOf { it.longitude }
        val maxLongitude = depots.maxOf { it.longitude }

        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {

            val horizontalPadding = 50f
            val verticalPadding = 50f

            val longitudeRange =
                (maxLongitude - minLongitude).takeIf { it > 0.000001 }
                    ?: 1.0

            val latitudeRange =
                (maxLatitude - minLatitude).takeIf { it > 0.000001 }
                    ?: 1.0

            depots.forEach { depot ->

                val xRatio =
                    ((depot.longitude - minLongitude) / longitudeRange)
                        .toFloat()

                val yRatio =
                    ((maxLatitude - depot.latitude) / latitudeRange)
                        .toFloat()

                val xPosition =
                    horizontalPadding +
                            xRatio * (size.width - horizontalPadding * 2)

                val yPosition =
                    verticalPadding +
                            yRatio * (size.height - verticalPadding * 2)

                drawCircle(
                    color = Color.Red,
                    radius = 18f,
                    center = Offset(
                        x = xPosition,
                        y = yPosition
                    )
                )

                drawCircle(
                    color = Color.White,
                    radius = 7f,
                    center = Offset(
                        x = xPosition,
                        y = yPosition
                    )
                )
            }
        }

        Text(
            text = "Active depot locations",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(12.dp),
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "N",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DepotLocationCard(
    depot: DepotResponse
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color.White
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {

                Text(
                    text = depot.name,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Depot #${depot.depotId}"
                )

                Text(
                    text = depot.address
                )

                Text(
                    text = "Latitude: ${depot.latitude}"
                )

                Text(
                    text = "Longitude: ${depot.longitude}"
                )
            }
        }
    }
}