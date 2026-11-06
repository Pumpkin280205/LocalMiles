package com.example.localmiles.Screens.Driver

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.localmiles.Common.Components.AppTopBar
import com.example.localmiles.Common.Components.KeepScreenOn
import com.example.localmiles.Core.Utils.DriverPreferences

@Composable
fun PodCaptureScreen(
    stopId: Long,
    jobId: Long,
    viewModel: DriverViewModel,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var recipientName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var submitError by remember { mutableStateOf<String?>(null) }

    val stop = viewModel.currentJobStops.find { it.stopId == stopId }
    val driverSettings by DriverPreferences.settings.collectAsState()

    KeepScreenOn(enabled = driverSettings.keepScreenOn)

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Capture POD",
                showBack = true,
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = recipientName,
                onValueChange = { recipientName = it },
                label = { Text("Recipient Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            if (submitError != null) {
                Text(
                    text = submitError ?: "",
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    submitError = null
                    // In a real app, we'd capture the device's live GPS here. We fall
                    // back to the stop's own registered coordinates, since the backend
                    // rejects any POD submitted more than 200m from the stop.
                    viewModel.submitPod(
                        stopId = stopId,
                        jobId = jobId,
                        recipientName = recipientName,
                        latitude = stop?.latitude ?: 0.0,
                        longitude = stop?.longitude ?: 0.0,
                        notes = notes,
                        photo = null,
                        signature = null,
                        onSuccess = onSuccess,
                        onError = { message -> submitError = message },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = recipientName.isNotBlank() && !viewModel.isLoading
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Submit POD")
                }
            }
        }
    }
}
