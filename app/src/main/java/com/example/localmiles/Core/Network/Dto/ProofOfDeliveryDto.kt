package com.example.localmiles.Core.Network.Dto

import com.google.gson.annotations.SerializedName

data class PodResponse(
    @SerializedName("PodId") val podId: Long,
    @SerializedName("StopId") val stopId: Long,
    @SerializedName("DriverId") val driverId: Long,
    @SerializedName("RecipientName") val recipientName: String,
    @SerializedName("Latitude") val latitude: Double,
    @SerializedName("Longitude") val longitude: Double,
    @SerializedName("PhotoUrl") val photoUrl: String? = null,
    @SerializedName("SignatureUrl") val signatureUrl: String? = null,
    @SerializedName("PhotoSignedUrl") val photoSignedUrl: String? = null,
    @SerializedName("SignatureSignedUrl") val signatureSignedUrl: String? = null,
    @SerializedName("Notes") val notes: String? = null,
    @SerializedName("CapturedAt") val capturedAt: String? = null,
)

data class PodListResponse(
    val pods: List<PodResponse>,
)

data class PodDetailResponse(
    val pod: PodResponse,
)

data class PodDeleteResponse(
    val deleted: Boolean,
    val podId: Long,
)
