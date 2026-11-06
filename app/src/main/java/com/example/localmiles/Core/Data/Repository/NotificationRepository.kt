package com.example.localmiles.Core.Data.Repository

import com.example.localmiles.Core.Network.ApiService
import com.example.localmiles.Core.Network.Dto.MarkAllReadResponse
import com.example.localmiles.Core.Network.Dto.NotificationCreateRequest
import com.example.localmiles.Core.Network.Dto.NotificationDeleteResponse
import com.example.localmiles.Core.Network.Dto.NotificationReadRequest
import com.example.localmiles.Core.Network.Dto.NotificationResponse
import com.example.localmiles.Core.Network.RetrofitClient
import com.example.localmiles.Core.Utils.Result
import com.example.localmiles.Core.Utils.SessionManager

class NotificationRepository(
    session: SessionManager,
    private val api: ApiService = RetrofitClient.getApiService(session),
) {

    //GET notifications — the caller's own, newest first.
    suspend fun getNotifications(
        isRead: Boolean? = null,
        type: String? = null,
        limit: Int? = null,
        offset: Int? = null,
    ): Result<List<NotificationResponse>> =
        apiCall { api.getNotifications(isRead, type, limit, offset).notifications }

    suspend fun getUnreadCount(): Result<Int> =
        apiCall { api.getUnreadNotificationCount().count }

    //owner or admin.
    suspend fun getNotification(id: Long): Result<NotificationResponse> =
        apiCall { api.getNotification(id).notification }

    //POST notifications — admin only.
    suspend fun createNotification(body: NotificationCreateRequest): Result<NotificationResponse> =
        apiCall { api.createNotification(body).notification }

    suspend fun markAllRead(): Result<Int> =
        apiCall { api.markAllNotificationsRead().updated }

    //owner only.
    suspend fun markRead(id: Long, isRead: Boolean): Result<NotificationResponse> =
        apiCall { api.updateNotification(id, NotificationReadRequest(isRead)).notification }

    //owner or admin.
    suspend fun deleteNotification(id: Long): Result<NotificationDeleteResponse> =
        apiCall { api.deleteNotification(id) }
}
