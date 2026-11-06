package com.example.localmiles.Screens.Driver

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.localmiles.Common.Components.AppTopBar
import com.example.localmiles.Common.Components.DashboardHeader
import com.example.localmiles.Common.Components.SettingsAction
import com.example.localmiles.Screens.Driver.Sync.OfflineQueueManager

/**
 * Driver dashboard displaying offline synchronization information.
 *
 * References:
 * Android Developers (2026) State and Jetpack Compose.
 * Available at: https://developer.android.com/develop/ui/compose/state
 *
 * Android Developers (2026) WorkManager.
 * Available at: https://developer.android.com/topic/libraries/architecture/workmanager
 */
@Composable
fun DriverDashboardScreen(
    onViewJobs: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current

    // Create the queue manager using its updated factory method.
    val offlineQueueManager = remember(context) {
        OfflineQueueManager.create(context)
    }

    // Observe the real Room synchronization summary.
    val syncSummary by remember(offlineQueueManager) {
        offlineQueueManager.observeSummary()
    }.collectAsState(
        initial = com.example.localmiles.Screens.Driver.Sync.SyncSummary()
    )

    Scaffold(
        topBar = {
            AppTopBar(
                title = "LocalMiles",
                subtitle = "Driver Dashboard",
                actions = {
                    SettingsAction(onClick = onSettings)
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {

            Spacer(modifier = Modifier.height(20.dp))

            DashboardHeader(
                title = "Welcome, Driver!",
                subtitle = "Your assigned deliveries are waiting.",
                onLogout = onLogout
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Text(
                        text = "Offline Synchronization",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Pending: ${syncSummary.pending}")
                    Text("Synced: ${syncSummary.synced}")
                    Text("Failed: ${syncSummary.failed}")

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when {
                            syncSummary.pending > 0 ->
                                "Updates are waiting to synchronize."

                            syncSummary.failed > 0 ->
                                "Some updates failed. Please retry."

                            else ->
                                "No pending or failed updates."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (
                        syncSummary.pending > 0 ||
                        syncSummary.failed > 0
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                OfflineQueueManager.schedule(context)
                            }
                        ) {
                            Text("Retry Sync")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onViewJobs,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("My Jobs")
            }
        }
    }
}