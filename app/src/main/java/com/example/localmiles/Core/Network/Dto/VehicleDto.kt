package com.example.localmiles.Core.Network.Dto

import com.google.gson.annotations.SerializedName

data class VehicleResponse(
    @SerializedName("VehicleId") val vehicleId: Long,
    @SerializedName("DepotId") val depotId: Long,
    @SerializedName("RegistrationNumber") val registrationNumber: String,
    @SerializedName("Make") val make: String,
    @SerializedName("Model") val model: String,
    @SerializedName("VehicleType") val vehicleType: String,
    @SerializedName("CapacityKg") val capacityKg: Double,
    @SerializedName("IsAvailable") val isAvailable: Boolean,
    @SerializedName("IsActive") val isActive: Boolean,
)

data class VehicleListResponse(
    val vehicles: List<VehicleResponse>,
)

data class VehicleDetailResponse(
    val vehicle: VehicleResponse,
)

data class VehicleUpsertRequest(
    @SerializedName("DepotId") val depotId: Long? = null,
    @SerializedName("RegistrationNumber") val registrationNumber: String? = null,
    @SerializedName("Make") val make: String? = null,
    @SerializedName("Model") val model: String? = null,
    @SerializedName("VehicleType") val vehicleType: String? = null,
    @SerializedName("CapacityKg") val capacityKg: Double? = null,
    @SerializedName("IsAvailable") val isAvailable: Boolean? = null,
    @SerializedName("IsActive") val isActive: Boolean? = null,
)
