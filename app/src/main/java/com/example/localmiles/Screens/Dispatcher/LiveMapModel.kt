package com.example.localmiles.Screens.Dispatcher

import com.example.localmiles.Core.Network.Dto.DepotResponse
import com.example.localmiles.Core.Network.Dto.JobDriverLocationResponse
import com.example.localmiles.Core.Network.Dto.StopResponse
import com.example.localmiles.Core.Network.JobResponse
import com.example.localmiles.Core.Utils.Coordinates
import com.example.localmiles.Core.Utils.JobStatus
import com.example.localmiles.Core.Utils.StopStatus

/**
 * A job that's on the road (or about to be) as the Live Map shows it: where
 * its driver is, and how far through its stops they are.
 *
 * The position is the driver's live GPS (sent by the driver app while En
 * Route, stored in Firestore) when a recent ping exists. Otherwise it falls
 * back to their most recent confirmed point — the last stop they arrived at or
 * delivered, or the depot. [isLive] says which, and the label says so too.
 */
data class ActiveDelivery(
    val job: JobResponse,
    val driverName: String,
    val stops: List<StopResponse>,
    val completedStops: Int,
    val driverPosition: Coordinates?,
    val positionLabel: String,
    val depot: DepotResponse?,
    val isLive: Boolean = false,
)

// Older pings are from a previous shift, not where the driver is now.
const val LIVE_LOCATION_MAX_AGE_SECONDS = 30 * 60L

/** The driver's live GPS position and label, or null if there's no recent ping. */
fun livePosition(live: JobDriverLocationResponse?): Pair<Coordinates, String>? {
    if (live == null) return null
    val age = live.ageSeconds
    if (age != null && age > LIVE_LOCATION_MAX_AGE_SECONDS) return null

    val point = Coordinates(live.location.latitude, live.location.longitude)
    if (!point.isPlottable()) return null

    return point to "Live GPS · ${describeAge(age)}"
}

/** 42 -> "just now", 300 -> "5 min ago". */
fun describeAge(seconds: Long?): String = when {
    seconds == null -> "time unknown"
    seconds < 60 -> "just now"
    else -> "${seconds / 60} min ago"
}

/** Stops saved before geocoding existed sit at 0,0 — never draw those. */
fun Coordinates.isPlottable(): Boolean =
    latitude in -90.0..90.0 && longitude in -180.0..180.0 && !(latitude == 0.0 && longitude == 0.0)

fun StopResponse.coordinates() = Coordinates(latitude, longitude)

fun DepotResponse.coordinates() = Coordinates(latitude, longitude)

fun estimateDriverPosition(
    job: JobResponse,
    stops: List<StopResponse>,
    depot: DepotResponse?
): Pair<Coordinates?, String> {
    val depotPoint = depot?.coordinates()?.takeIf { it.isPlottable() }

    if (job.status == JobStatus.ASSIGNED) {
        return depotPoint to "Waiting at ${depot?.name ?: "the depot"}"
    }

    val lastReached = stops
        .filter { it.status == StopStatus.COMPLETED || it.status == StopStatus.ARRIVED }
        .maxByOrNull { it.sequenceNumber }

    if (lastReached != null && lastReached.coordinates().isPlottable()) {
        val verb = if (lastReached.status == StopStatus.ARRIVED) "At" else "Left"
        return lastReached.coordinates() to "$verb stop ${lastReached.sequenceNumber} · ${lastReached.recipientName}"
    }

    return depotPoint to "Departed ${depot?.name ?: "the depot"}"
}

/** En Route jobs first, then Assigned, each with their driver's last known position. */
fun buildActiveDeliveries(state: DispatcherUiState): List<ActiveDelivery> =
    state.jobs
        .filter { it.status == JobStatus.EN_ROUTE || it.status == JobStatus.ASSIGNED }
        .sortedBy { if (it.status == JobStatus.EN_ROUTE) 0 else 1 }
        .map { job ->
            val stops = state.stopsByJob[job.jobId].orEmpty().sortedBy { it.sequenceNumber }
            val depot = state.depots.firstOrNull { it.depotId == job.depotId }
            val live = livePosition(state.driverLocations[job.jobId])
            val (position, label) = live ?: estimateDriverPosition(job, stops, depot)
            ActiveDelivery(
                job = job,
                driverName = state.driverName(job.driverId),
                stops = stops,
                completedStops = stops.count { it.status == StopStatus.COMPLETED },
                driverPosition = position,
                positionLabel = label,
                depot = depot,
                isLive = live != null,
            )
        }
