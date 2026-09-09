package com.example.localmiles.Core.Network.Dto

import com.google.gson.annotations.SerializedName

data class JobUpsertRequest(
    @SerializedName("ClientId") val clientId: Long? = null,
    @SerializedName("DepotId") val depotId: Long? = null,
    @SerializedName("DriverId") val driverId: Long? = null,
    @SerializedName("VehicleId") val vehicleId: Long? = null,
    /** ISO-8601 date/time string. */
    @SerializedName("ScheduledDate") val scheduledDate: String? = null,
    @SerializedName("Status") val status: String? = null,
    @SerializedName("Priority") val priority: String? = null,
    @SerializedName("Notes") val notes: String? = null,
    @SerializedName("EstimatedDistanceKm") val estimatedDistanceKm: Double? = null,
    @SerializedName("EstimatedDurationMinutes") val estimatedDurationMinutes: Int? = null,
)

//Body for PATCH jobs/{id}/status.
data class JobStatusRequest(
    @SerializedName("Status") val status: String,
)
