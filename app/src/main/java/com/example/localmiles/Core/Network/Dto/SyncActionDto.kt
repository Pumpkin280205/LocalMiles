package com.example.localmiles.Core.Network.Dto

import com.google.gson.annotations.SerializedName

//One queued offline action sent to POST sync.
data class SyncActionRequest(
    @SerializedName("SyncActionId") val syncActionId: String,
    @SerializedName("ActionType") val actionType: String,
    @SerializedName("EntityType") val entityType: String,
    @SerializedName("EntityId") val entityId: String,
    @SerializedName("Payload") val payload: Map<String, @JvmSuppressWildcards Any>,
)

//Body for POST sync — 1..50 queued actions.
data class SyncSubmitRequest(
    val actions: List<SyncActionRequest>,
)

//Per-action outcome returned by POST sync.
data class SyncResultItem(
    @SerializedName("SyncActionId") val syncActionId: String,
    val outcome: String,
    val status: String? = null,
    val error: String? = null,
)

data class SyncSubmitResponse(
    val results: List<SyncResultItem>,
)

//A queued action's server-side row, as returned by GET sync / GET sync/{id}.
data class SyncActionResponse(
    @SerializedName("SyncActionId") val syncActionId: String,
    @SerializedName("UserId") val userId: Long,
    @SerializedName("ActionType") val actionType: String,
    @SerializedName("EntityType") val entityType: String,
    @SerializedName("EntityId") val entityId: String,
    @SerializedName("Payload") val payload: Map<String, @JvmSuppressWildcards Any>? = null,
    @SerializedName("SyncStatus") val syncStatus: String,
    @SerializedName("RetryCount") val retryCount: Int? = null,
    @SerializedName("ErrorMessage") val errorMessage: String? = null,
    @SerializedName("LastAttemptAt") val lastAttemptAt: String? = null,
    @SerializedName("CreatedAt") val createdAt: String? = null,
)

data class SyncActionListResponse(
    val actions: List<SyncActionResponse>,
    val limit: Int,
    val offset: Int,
)

data class SyncActionDetailResponse(
    val action: SyncActionResponse,
)
