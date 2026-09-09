package com.example.localmiles.Core.Network.Dto

import com.google.gson.annotations.SerializedName

data class NotificationResponse(
    @SerializedName("NotificationId") val notificationId: Long,
    @SerializedName("UserId") val userId: Long,
    @SerializedName("Title") val title: String,
    @SerializedName("Message") val message: String,
    @SerializedName("NotificationType") val notificationType: String,
    @SerializedName("RelatedEntityId") val relatedEntityId: String? = null,
    @SerializedName("IsRead") val isRead: Boolean,
    @SerializedName("CreatedAt") val createdAt: String,
)

data class NotificationListResponse(
    val notifications: List<NotificationResponse>,
    val limit: Int,
    val offset: Int,
)

data class NotificationDetailResponse(
    val notification: NotificationResponse,
)

data class UnreadCountResponse(
    val count: Int,
)

data class MarkAllReadResponse(
    val updated: Int,
)

data class NotificationDeleteResponse(
    val deleted: Boolean,
    val notificationId: Long,
)

data class NotificationCreateRequest(
    @SerializedName("UserId") val userId: Long? = null,
    @SerializedName("UserIds") val userIds: List<Long>? = null,
    @SerializedName("Title") val title: String,
    @SerializedName("Message") val message: String,
    @SerializedName("NotificationType") val notificationType: String,
    @SerializedName("RelatedEntityId") val relatedEntityId: String? = null,
)

data class NotificationBulkResponse(
    val notifications: List<NotificationResponse>,
    val count: Int,
)

//Body for PATCH notifications/{id}.
data class NotificationReadRequest(
    @SerializedName("IsRead") val isRead: Boolean,
)
