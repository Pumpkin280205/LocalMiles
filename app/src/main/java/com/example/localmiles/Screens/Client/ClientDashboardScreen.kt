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
import androidx.compose.material3.OutlinedButton
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
import com.example.localmiles.Core.Network.JobResponse

private val LocalMilesTeal = Color(0xFF004759)
private val LocalMilesGreen = Color(0xFF018B58)
private val LocalMilesLime = Color(0xFF8AC237)
private val LocalMilesOrange = Color(0xFFF28800)
private val ScreenBackground = Color(0xFFF7F8F8)
private val SoftGrey = Color(0xFFF1F3F3)
private val TextGrey = Color(0xFF667277)

@Composable
fun ClientDashboardScreen(
    viewModel: ClientViewModel,
    onTrackShipment: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadClientJobs()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {

        // LocalMiles header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalMilesTeal)
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LocalMiles",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Client Dashboard",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }

                TextButton(onClick = onOpenSettings) {
                    Text(
                        text = "Settings",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "My Deliveries",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = LocalMilesTeal
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Track your shipments in real time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGrey
                    )
                }

                TextButton(onClick = onLogout) {
                    Text(
                        text = "Log out",
                        color = LocalMilesOrange,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = LocalMilesGreen
                        )
                    }
                }

                uiState.errorMessage != null -> {
                    ErrorCard(
                        message = uiState.errorMessage
                            ?: "Unable to load deliveries.",
                        onRetry = {
                            viewModel.loadClientJobs()
                        }
                    )
                }

                uiState.jobs.isEmpty() -> {
                    EmptyDeliveriesCard()
                }

                else -> {
                    Text(
                        text = "${uiState.jobs.size} shipments",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextGrey
                    )

                    Spacer(modifier = Modifier.height(10.dp))

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

                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientDeliveryCard(
    job: JobResponse,
    onTrackShipment: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
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
                    text = "Delivery #${job.jobId}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LocalMilesTeal
                )

                StatusBadge(status = job.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(
                color = Color(0xFFE6EAEA)
            )

            Spacer(modifier = Modifier.height(12.dp))

            DeliveryInformationRow(
                label = "Scheduled",
                value = formatDate(job.scheduledDate)
            )

            Spacer(modifier = Modifier.height(8.dp))

            DeliveryInformationRow(
                label = "Priority",
                value = formatStatus(job.priority)
            )

            job.estimatedDistanceKm?.let { distance ->
                Spacer(modifier = Modifier.height(8.dp))

                DeliveryInformationRow(
                    label = "Distance",
                    value = "$distance km"
                )
            }

            job.estimatedDurationMinutes?.let { duration ->
                Spacer(modifier = Modifier.height(8.dp))

                DeliveryInformationRow(
                    label = "Estimated time",
                    value = "$duration min"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onTrackShipment(job.jobId)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LocalMilesGreen
                )
            ) {
                Text(
                    text = "Track Shipment",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DeliveryInformationRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextGrey
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = LocalMilesTeal
        )
    }
}

@Composable
private fun StatusBadge(
    status: String
) {
    val backgroundColor = when (status.lowercase()) {
        "completed", "delivered" -> LocalMilesGreen
        "enroute", "en route", "inprogress", "in progress" -> LocalMilesLime
        "pending" -> LocalMilesOrange
        "cancelled", "failed" -> Color(0xFFC94B45)
        else -> LocalMilesTeal
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor
    ) {
        Text(
            text = formatStatus(status),
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 6.dp
            ),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyDeliveriesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        color = SoftGrey,
                        shape = CircleShape
                    )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No deliveries yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LocalMilesTeal
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Your assigned shipments will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextGrey
            )
        }
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Unable to load deliveries",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFC94B45)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = message,
                color = TextGrey
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Try Again",
                    color = LocalMilesTeal
                )
            }
        }
    }
}

// Makes API values  easier to read
private fun formatStatus(value: String): String {
    return when (value.lowercase()) {
        "enroute" -> "En Route"
        "inprogress" -> "In Progress"
        else -> value
    }
}

// Keeps the API data intact
private fun formatDate(value: String): String {
    return value
        .replace("T", " ")
        .substringBefore(".")
        .take(16)
}