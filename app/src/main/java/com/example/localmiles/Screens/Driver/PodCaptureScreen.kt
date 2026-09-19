package com.example.localmiles.Screens.Driver

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.localmiles.Common.Components.AppTopBar

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

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    // In a real app, we'd capture photo/signature/GPS here.
                    // For now, we stub GPS coordinates and submit.
                    viewModel.submitPod(
                        stopId = stopId,
                        jobId = jobId,
                        recipientName = recipientName,
                        latitude = -25.7479, // Pretoria stub
                        longitude = 28.2293,
                        notes = notes,
                        photo = null,
                        signature = null
                    )
                    onSuccess()
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
