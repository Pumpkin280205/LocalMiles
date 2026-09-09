package com.example.localmiles.Core.Network.Dto

import com.google.gson.annotations.SerializedName

data class StopResponse(
    @SerializedName("StopId") val stopId: Long,
    @SerializedName("JobId") val jobId: Long,
    @SerializedName("SequenceNumber") val sequenceNumber: Int,
    @SerializedName("RecipientName") val recipientName: String,
    @SerializedName("Address") val address: String,
    @SerializedName("Latitude") val latitude: Double,
    @SerializedName("Longitude") val longitude: Double,
    @SerializedName("DeliveryWindowStart") val deliveryWindowStart: String? = null,
    @SerializedName("DeliveryWindowEnd") val deliveryWindowEnd: String? = null,
    @SerializedName("Status") val status: String,
    @SerializedName("Instructions") val instructions: String? = null,
    @SerializedName("ArrivedAt") val arrivedAt: String? = null,
    @SerializedName("CompletedAt") val completedAt: String? = null,
)

data class StopListResponse(
    val stops: List<StopResponse>,
)

data class StopDetailResponse(
    val stop: StopResponse,
)

data class StopUpsertRequest(
    @SerializedName("SequenceNumber") val sequenceNumber: Int? = null,
    @SerializedName("RecipientName") val recipientName: String? = null,
    @SerializedName("Address") val address: String? = null,
    @SerializedName("Latitude") val latitude: Double? = null,
    @SerializedName("Longitude") val longitude: Double? = null,
    @SerializedName("DeliveryWindowStart") val deliveryWindowStart: String? = null,
    @SerializedName("DeliveryWindowEnd") val deliveryWindowEnd: String? = null,
    @SerializedName("Status") val status: String? = null,
    @SerializedName("Instructions") val instructions: String? = null,
    @SerializedName("ArrivedAt") val arrivedAt: String? = null,
    @SerializedName("CompletedAt") val completedAt: String? = null,
)

//Body for PATCH stops/{id}/status.
data class StopStatusRequest(
    @SerializedName("Status") val status: String,
)
