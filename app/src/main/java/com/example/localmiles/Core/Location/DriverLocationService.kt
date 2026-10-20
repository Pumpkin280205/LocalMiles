package com.example.localmiles.Core.Location

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.localmiles.Core.Data.Repository.LocationRepository
import com.example.localmiles.Core.Network.Dto.LocationPingRequest
import com.example.localmiles.Core.Notifications.NotificationChannels
import com.example.localmiles.Core.Utils.Result
import com.example.localmiles.Core.Utils.SessionManager
import com.example.localmiles.MainActivity
import com.example.localmiles.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Sends the driver's GPS position to the API (POST drivers/me/location, stored
 * in Firestore) every [INTERVAL_MS] while a job is En Route, so the dispatcher's
 * Live Map and the client's tracking show where the driver really is.
 *
 * A foreground service so it keeps reporting with the screen off or the app in
 * the background; Android shows an ongoing "sharing location" notification.
 * Started from the driver's job screen, stopped when the job completes or the
 * driver logs out.
 */
class DriverLocationService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var locationRepository: LocationRepository
    private var jobId: Long? = null

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { report(it) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        locationRepository = LocationRepository(SessionManager(this))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP || !hasLocationPermission(this)) {
            stopTracking()
            return START_NOT_STICKY
        }

        val newJobId = intent?.getLongExtra(EXTRA_JOB_ID, -1L)?.takeIf { it > 0 }
            ?: run { stopTracking(); return START_NOT_STICKY }

        startForeground(
            NOTIFICATION_ID,
            buildNotification(newJobId),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )

        if (jobId != newJobId) {
            jobId = newJobId
            _trackingJobId.value = newJobId
            requestUpdates()
        }

        // Not restarted by the system after being killed: the job screen
        // starts it again, and a stale job id is worse than no pings.
        return START_NOT_STICKY
    }

    @SuppressLint("MissingPermission") // checked in onStartCommand
    private fun requestUpdates() {
        fusedClient.removeLocationUpdates(callback)

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVAL_MS)
            .setMinUpdateIntervalMillis(MIN_INTERVAL_MS)
            .build()

        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }

    private fun report(location: Location) {
        val currentJobId = jobId ?: return

        scope.launch {
            val ping = LocationPingRequest(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracyMeters = location.accuracy.takeIf { location.hasAccuracy() },
                speedMps = location.speed.takeIf { location.hasSpeed() },
                heading = location.bearing.takeIf { location.hasBearing() && it >= 0f && it < 360f },
                recordedAt = Instant.ofEpochMilli(location.time).toString(),
                jobId = currentJobId,
            )

            when (val result = locationRepository.reportMyLocation(ping)) {
                is Result.Success -> Log.d(TAG, "Location sent for job $currentJobId")
                // Keep going — a dropped ping is replaced by the next one.
                is Result.Error -> Log.w(TAG, "Location ping failed: ${result.message}")
            }
        }
    }

    private fun buildNotification(jobId: Long) =
        NotificationCompat.Builder(this, NotificationChannels.TRACKING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Sharing your location")
            .setContentText("Job #$jobId is en route — your dispatcher can see where you are.")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    private fun stopTracking() {
        fusedClient.removeLocationUpdates(callback)
        jobId = null
        _trackingJobId.value = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        fusedClient.removeLocationUpdates(callback)
        _trackingJobId.value = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "DriverLocationService"
        private const val NOTIFICATION_ID = 4201
        private const val EXTRA_JOB_ID = "jobId"
        private const val ACTION_STOP = "com.example.localmiles.STOP_LOCATION"

        const val INTERVAL_MS = 30_000L
        private const val MIN_INTERVAL_MS = 15_000L

        private val _trackingJobId = MutableStateFlow<Long?>(null)

        /** The job whose location is being shared right now, or null. */
        val trackingJobId: StateFlow<Long?> = _trackingJobId.asStateFlow()

        fun hasLocationPermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

        /** Start (or switch to) sharing location for [jobId]. Needs location permission. */
        fun start(context: Context, jobId: Long) {
            if (!hasLocationPermission(context)) return
            ContextCompat.startForegroundService(
                context,
                Intent(context, DriverLocationService::class.java).putExtra(EXTRA_JOB_ID, jobId)
            )
        }

        /** Stop sharing. Safe to call when it isn't running. */
        fun stop(context: Context) {
            if (_trackingJobId.value == null) return
            context.startService(
                Intent(context, DriverLocationService::class.java).setAction(ACTION_STOP)
            )
        }
    }
}
