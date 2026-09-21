package com.example.localmiles.Core.Navigation

import com.example.localmiles.Core.Utils.Role

object Routes {

    const val SPLASH = "splash"
    const val LOGIN = "login"

    // ============================================================
    // DISPATCHER ROUTES
    // ============================================================

    const val DISPATCHER_DASHBOARD = "dispatcher/dashboard"

    const val DISPATCHER_JOBS = "dispatcher/jobs"

    // Create a new job
    const val DISPATCHER_CREATE_JOB = "dispatcher/jobs/create"

    // Edit an existing job
    const val DISPATCHER_EDIT_JOB = "dispatcher/jobs/edit/{jobId}"

    const val DISPATCHER_FLEET = "dispatcher/fleet"

    const val DISPATCHER_LIVE_MAP = "dispatcher/map"

    const val DISPATCHER_HISTORY = "dispatcher/history"

    const val DISPATCHER_USERS = "dispatcher/users"

    // Dispatcher account settings
    const val DISPATCHER_SETTINGS = "dispatcher/settings"


    // ============================================================
    // DRIVER ROUTES
    // ============================================================

    const val DRIVER_DASHBOARD = "driver/dashboard"

    const val DRIVER_MY_JOBS = "driver/jobs"

    const val DRIVER_JOB_STOPS = "driver/jobs/{jobId}/stops"

    const val DRIVER_CAPTURE_POD = "driver/stops/{stopId}/pod/{jobId}"

    // Driver account settings
    const val DRIVER_SETTINGS = "driver/settings"


    // ============================================================
    // CLIENT ROUTES
    // ============================================================

    const val DRIVER_MY_JOBS = "driver/jobs"
    const val DRIVER_JOB_STOPS = "driver/jobs/{jobId}/stops"
    const val DRIVER_CAPTURE_POD = "driver/stops/{stopId}/pod/{jobId}"
    const val CLIENT_DASHBOARD = "client/dashboard"

    const val CLIENT_TRACK_SHIPMENT = "client/track/{jobId}"

    const val CLIENT_SETTINGS = "client/settings"


    // ============================================================
    // DISPATCHER HELPER FUNCTIONS
    // ============================================================

    fun dispatcherEditJob(jobId: Long): String =
        "dispatcher/jobs/edit/$jobId"


    // ============================================================
    // DRIVER HELPER FUNCTIONS
    // ============================================================

    fun driverJobStops(jobId: Long): String =
        "driver/jobs/$jobId/stops"

    fun driverCapturePod(
        stopId: Long,
        jobId: Long
    ): String =
        "driver/stops/$stopId/pod/$jobId"


    // ============================================================
    // CLIENT HELPER FUNCTIONS
    // ============================================================

    fun clientTrackShipment(jobId: Long): String =
        "client/track/$jobId"
    // Helper functions for dynamic routes
    fun driverJobStops(jobId: Long): String = "driver/jobs/$jobId/stops"
    fun driverCapturePod(stopId: Long, jobId: Long): String = "driver/stops/$stopId/pod/$jobId"
    fun clientTrackShipment(jobId: Long): String = "client/track/$jobId"
}


/**
 * Single source of truth for which home screen
 * a role lands on after login.
 */
fun homeRouteForRole(role: String?): String = when (role) {

    Role.ADMIN ->
        Routes.DISPATCHER_DASHBOARD

    Role.DRIVER ->
        Routes.DRIVER_DASHBOARD

    Role.CLIENT ->
        Routes.CLIENT_DASHBOARD

    else ->
        Routes.LOGIN
}