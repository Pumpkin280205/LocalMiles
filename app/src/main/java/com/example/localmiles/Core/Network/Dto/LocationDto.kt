package com.example.localmiles.Core.Network.Dto

import com.google.gson.annotations.SerializedName

//Body for POST drivers/me/location — one GPS ping, stored in Firestore by the API.
data class LocationPingRequest(
    @SerializedName("Latitude") val latitude: Double,
    @SerializedName("Longitude") val longitude: Double,
    @SerializedName("AccuracyMeters") val accuracyMeters: Float? = null,
    @SerializedName("SpeedMps") val speedMps: Float? = null,
    // 0 to <360 degrees.
    @SerializedName("Heading") val heading: Float? = null,
    // ISO 8601 — when the phone took the fix, not when it was sent.
    @SerializedName("RecordedAt") val recordedAt: String? = null,
    @SerializedName("JobId") val jobId: Long? = null,
)

data class LocationPingResponse(
    val pingId: String,
    val latestUpdated: Boolean,
)

//A driver's latest known position (Firestore driverLocations/{driverId}).
data class DriverLocationResponse(
    @SerializedName("DriverId") val driverId: Long,
    @SerializedName("JobId") val jobId: Long? = null,
    @SerializedName("Latitude") val latitude: Double,
    @SerializedName("Longitude") val longitude: Double,
    @SerializedName("AccuracyMeters") val accuracyMeters: Double? = null,
    @SerializedName("SpeedMps") val speedMps: Double? = null,
    @SerializedName("Heading") val heading: Double? = null,
    @SerializedName("RecordedAt") val recordedAt: String? = null,
    @SerializedName("ReceivedAt") val receivedAt: String? = null,
)

//Envelope returned by GET jobs/{id}/driver-location.
data class JobDriverLocationResponse(
    val jobId: Long,
    val location: DriverLocationResponse,
    // Seconds since the phone recorded this fix, or null if unknown.
    val ageSeconds: Long? = null,
)
