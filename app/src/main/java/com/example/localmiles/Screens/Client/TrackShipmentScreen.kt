package com.example.localmiles.Screens.Client

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localmiles.Core.Network.Dto.StopResponse

@Composable
fun TrackShipmentScreen(
    jobId: Long,
    viewModel: ClientViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(jobId) {
        viewModel.loadShipmentTracking(jobId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // Top app bar
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text(
                        text = "‹",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                Column {
                    Text(
                        text = "Track Shipment",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Delivery #$jobId",
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            uiState.errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Unable to load shipment",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = uiState.errorMessage
                            ?: "Unable to load shipment.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.loadShipmentTracking(jobId)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "Try Again",
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            uiState.selectedJob != null -> {
                val job = uiState.selectedJob!!

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    item {

                        // Shipment status summary
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 2.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp)
                            ) {

                                Text(
                                    text = "Estimated Delivery",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = formatScheduledDate(job.scheduledDate),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                StatusPill(job.status)

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = statusMessage(job.status),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )

                                Spacer(modifier = Modifier.height(18.dp))


                                ShipmentProgress(
                                    status = job.status
                                )
                            }
                        }
                    }

                    item {

                        // Delivery details
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 2.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp)
                            ) {

                                Text(
                                    text = "Delivery Details",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                TrackingDetailRow(
                                    label = "Delivery",
                                    value = "#${job.jobId}"
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )

                                TrackingDetailRow(
                                    label = "Priority",
                                    value = job.priority
                                )

                                job.estimatedDistanceKm?.let { distance ->
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )

                                    TrackingDetailRow(
                                        label = "Distance",
                                        value = "$distance km"
                                    )
                                }

                                job.estimatedDurationMinutes?.let { duration ->
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )

                                    TrackingDetailRow(
                                        label = "Duration",
                                        value = "$duration min"
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.SpaceBetween,
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Stops",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "${uiState.stops.size} total",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (uiState.stops.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Text(
                                    text = "No delivery stops are available for this shipment.",
                                    modifier = Modifier.padding(18.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {

                        items(
                            items = uiState.stops,
                            key = { stop -> stop.stopId }
                        ) { stop ->

                            DeliveryStopCard(
                                stop = stop
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "LocalMiles",
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Text(
                            text = "Deliver locally. Drive further.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Shipment information is unavailable.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Maps a status string to a (background, content) color pair drawn from the
// active MaterialTheme, so status colors invert correctly in dark mode.
@Composable
private fun statusColorPair(status: String): Pair<Color, Color> {
    val colorScheme = MaterialTheme.colorScheme

    return when (status.lowercase()) {
        "completed", "delivered" -> colorScheme.primary to colorScheme.onPrimary
        "enroute", "en route", "in progress" -> colorScheme.secondary to colorScheme.onSecondary
        "cancelled", "failed" -> colorScheme.error to colorScheme.onError
        "pending" -> colorScheme.tertiary to colorScheme.onTertiary
        else -> colorScheme.primaryContainer to colorScheme.onPrimaryContainer
    }
}

@Composable
private fun StatusPill(status: String) {

    val (backgroundColor, contentColor) = statusColorPair(status)

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(50.dp)
    ) {
        Text(
            text = formatStatus(status),
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 6.dp
            ),
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ShipmentProgress(status: String) {

    val completedSteps = when (status.lowercase()) {
        "completed", "delivered" -> 4
        "enroute", "en route", "in progress" -> 3
        "assigned" -> 2
        "pending" -> 1
        else -> 1
    }

    val completedColor = MaterialTheme.colorScheme.primary
    val completedContentColor = MaterialTheme.colorScheme.onPrimary
    val pendingColor = MaterialTheme.colorScheme.surfaceVariant
    val pendingContentColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        for (step in 1..4) {

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(
                        color = if (step <= completedSteps)
                            completedColor
                        else
                            pendingColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (step <= completedSteps) "✓" else "$step",
                    color = if (step <= completedSteps)
                        completedContentColor
                    else
                        pendingContentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (step < 4) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .background(
                            if (step < completedSteps)
                                completedColor
                            else
                                pendingColor
                        )
                )
            }
        }
    }
}

@Composable
private fun TrackingDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )

        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun DeliveryStopCard(
    stop: StopResponse
) {

    val (statusColour, statusContentColour) = statusColorPair(stop.status)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {

            // Numbered stop marker
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        statusColour,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stop.sequenceNumber.toString(),
                    color = statusContentColour,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    Text(
                        text = stop.recipientName,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = formatStatus(stop.status),
                        color = statusColour,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stop.address,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )

                if (
                    stop.deliveryWindowStart != null ||
                    stop.deliveryWindowEnd != null
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Delivery window: ${
                            stop.deliveryWindowStart ?: "N/A"
                        } - ${
                            stop.deliveryWindowEnd ?: "N/A"
                        }",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                stop.instructions
                    ?.takeIf { it.isNotBlank() }
                    ?.let { instructions ->

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Instructions",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = instructions,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }

                stop.arrivedAt?.let { arrived ->
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Arrived: ${formatDateTime(arrived)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                stop.completedAt?.let { completed ->
                    Text(
                        text = "Completed: ${formatDateTime(completed)}",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

private fun formatStatus(status: String): String {
    return when (status.lowercase()) {
        "enroute" -> "En Route"
        "inprogress" -> "In Progress"
        else -> status
            .replace("_", " ")
            .replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
    }
}

private fun statusMessage(status: String): String {
    return when (status.lowercase()) {
        "completed", "delivered" ->
            "Your delivery has been completed."

        "enroute", "en route", "in progress" ->
            "Your delivery is on the way."

        "cancelled" ->
            "This delivery has been cancelled."

        "pending" ->
            "Your delivery is being prepared."

        else ->
            "Follow your delivery progress below."
    }
}

private fun formatScheduledDate(value: String): String {
    if (value.isBlank()) return "To be confirmed"

    return try {
        val clean = value
            .replace("T", " ")
            .replace("Z", "")

        val parts = clean.split(" ")

        if (parts.size >= 2) {
            "${parts[0]}  ${parts[1].take(5)}"
        } else {
            clean
        }
    } catch (_: Exception) {
        value
    }
}

private fun formatDateTime(value: String): String {
    return try {
        value
            .replace("T", " ")
            .replace("Z", "")
            .take(16)
    } catch (_: Exception) {
        value
    }
}
