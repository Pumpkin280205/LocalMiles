package com.example.localmiles.Core.Network.Dto

import com.google.gson.annotations.SerializedName

data class DepotResponse(
    @SerializedName("DepotId") val depotId: Long,
    @SerializedName("Name") val name: String,
    @SerializedName("Address") val address: String,
    @SerializedName("Latitude") val latitude: Double,
    @SerializedName("Longitude") val longitude: Double,
    @SerializedName("ContactNumber") val contactNumber: String? = null,
    @SerializedName("IsActive") val isActive: Boolean,
)

data class DepotListResponse(
    val depots: List<DepotResponse>,
)

data class DepotDetailResponse(
    val depot: DepotResponse,
)

data class DepotUpsertRequest(
    @SerializedName("Name") val name: String? = null,
    @SerializedName("Address") val address: String? = null,
    @SerializedName("Latitude") val latitude: Double? = null,
    @SerializedName("Longitude") val longitude: Double? = null,
    @SerializedName("ContactNumber") val contactNumber: String? = null,
    @SerializedName("IsActive") val isActive: Boolean? = null,
)
