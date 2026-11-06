package com.example.localmiles.Screens.Driver

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
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

    val context = LocalContext.current
    var photoFile by remember { mutableStateOf<java.io.File?>(null) }
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var padSize by remember { mutableStateOf(IntSize.Zero) }

    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        photoFile = if (taken) downscalePhoto(podPhotoFile(context)) else photoFile
    }

    val stop = viewModel.currentJobStops.find { it.stopId == stopId }

    val scope = rememberCoroutineScope()
    var locating by remember { mutableStateOf(false) }
    var submitAfterPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            submitAfterPermission = true
        } else {
            submitError = "Location permission is needed to prove you are at the stop."
        }
    }

    // Send the phone's real location. The API rejects a POD more than 200 m from the stop
    val submit: () -> Unit = {
        submitError = null
        scope.launch {
            locating = true
            val fix = currentPosition(context)
            locating = false
            if (fix == null) {
                submitError = "Could not get your location. Move somewhere with a clear view of the sky and try again."
                return@launch
            }
            val away = stop?.let { distanceMeters(fix.latitude, fix.longitude, it.latitude, it.longitude) }
            if (away != null && away > MAX_POD_DISTANCE_METERS) {
                submitError = "You are ${away.roundToInt()} m from this stop (max ${MAX_POD_DISTANCE_METERS.roundToInt()} m)."
                return@launch
            }
            viewModel.submitPod(
                stopId = stopId,
                jobId = jobId,
                recipientName = recipientName,
                latitude = fix.latitude,
                longitude = fix.longitude,
                notes = notes,
                photo = photoFile,
                signature = renderSignature(context, strokes.toList(), padSize),
                onSuccess = onSuccess,
                onError = { message -> submitError = message },
            )
        }
    }

    LaunchedEffect(submitAfterPermission) {
        if (submitAfterPermission) {
            submitAfterPermission = false
            submit()
        }
    }
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
                .verticalScroll(rememberScrollState())
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

            OutlinedButton(
                onClick = {
                    val uri: Uri = FileProvider.getUriForFile(
                        context, "${context.packageName}.fileprovider", podPhotoFile(context)
                    )
                    takePhoto.launch(uri)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (photoFile == null) "Take photo (optional)" else "Photo added - retake")
            }

            Text("Recipient signature (optional)", style = MaterialTheme.typography.labelLarge)
            SignaturePad(strokes = strokes, onSize = { padSize = it })
            if (strokes.isNotEmpty()) {
                TextButton(onClick = { strokes.clear() }) { Text("Clear signature") }
            }

            if (submitError != null) {
                Text(
                    text = submitError ?: "",
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (hasLocationPermission(context)) {
                        submit()
                    } else {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = recipientName.isNotBlank() && !viewModel.isLoading && !locating
            ) {
                if (viewModel.isLoading || locating) {
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
