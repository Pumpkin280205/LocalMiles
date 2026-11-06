package com.example.localmiles.Screens.Driver

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.localmiles.Common.Components.AppTopBar
import com.example.localmiles.Common.Components.DashboardHeader
import com.example.localmiles.Common.Components.SettingsAction

@Composable
fun DriverDashboardScreen(
    onViewJobs: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit
) {
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
