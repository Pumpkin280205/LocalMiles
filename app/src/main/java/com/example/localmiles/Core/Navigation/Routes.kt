package com.example.localmiles.Core.Navigation

import com.example.localmiles.Core.Utils.Role

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"

    const val DISPATCHER_DASHBOARD = "dispatcher/dashboard"
    const val DRIVER_DASHBOARD = "driver/dashboard"
    const val DRIVER_MY_JOBS = "driver/jobs"
    const val DRIVER_JOB_STOPS = "driver/jobs/{jobId}/stops"
    const val DRIVER_CAPTURE_POD = "driver/stops/{stopId}/pod/{jobId}"
    const val CLIENT_DASHBOARD = "client/dashboard"

    // Client-specific navigation routes
    const val CLIENT_TRACK_SHIPMENT = "client/track/{jobId}"
    const val CLIENT_SETTINGS = "client/settings"

    // Helper functions for dynamic routes
    fun driverJobStops(jobId: Long): String = "driver/jobs/$jobId/stops"
    fun driverCapturePod(stopId: Long, jobId: Long): String = "driver/stops/$stopId/pod/$jobId"
    fun clientTrackShipment(jobId: Long): String = "client/track/$jobId"
}

/** Single source of truth for which home screen a role lands on after login. */
fun homeRouteForRole(role: String?): String = when (role) {
    Role.ADMIN -> Routes.DISPATCHER_DASHBOARD
    Role.DRIVER -> Routes.DRIVER_DASHBOARD
    Role.CLIENT -> Routes.CLIENT_DASHBOARD
    else -> Routes.LOGIN            // null / unknown role -> force re-login
}
