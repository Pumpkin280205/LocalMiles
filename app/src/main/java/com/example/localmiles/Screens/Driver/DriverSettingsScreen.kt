package com.example.localmiles.Screens.Driver

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localmiles.Common.Components.AppTopBar
import com.example.localmiles.Common.Components.ThemeModeSetting

// Settings available to LocalMiles drivers.
@Composable
fun DriverSettingsScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            AppTopBar(title = "Settings", showBack = true, onBack = onBack)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Driver Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Manage your LocalMiles preferences",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            ThemeModeSetting()
        }
    }
}
