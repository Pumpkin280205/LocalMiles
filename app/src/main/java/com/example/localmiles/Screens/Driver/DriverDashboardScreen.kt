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
 * Driver dashboard with offline synchronization status.
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

    // Reuse the same queue manager while this screen is displayed.
    val offlineQueueManager = remember(context) {
        OfflineQueueManager(context)
    }

    // Automatically update the UI when the number of pending actions changes.
    val pendingCount by offlineQueueManager
        .observePendingCount()
        .collectAsState(initial = 0)

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

            // OFFLINE SYNCHRONIZATION STATUS
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

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (pendingCount > 0) {
                            "Pending Sync: $pendingCount action(s)"
                        } else {
                            "No pending actions"
                        },
                        color = if (pendingCount > 0) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (pendingCount > 0) {
                            "Updates will synchronize when internet returns."
                        } else {
                            "All queued updates have been processed."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (pendingCount > 0) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                offlineQueueManager.scheduleSync()
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