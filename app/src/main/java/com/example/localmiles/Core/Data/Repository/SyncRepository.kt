package com.example.localmiles.Core.Data.Repository

import com.example.localmiles.Core.Network.ApiService
import com.example.localmiles.Core.Network.Dto.SyncActionRequest
import com.example.localmiles.Core.Network.Dto.SyncActionResponse
import com.example.localmiles.Core.Network.Dto.SyncResultItem
import com.example.localmiles.Core.Network.Dto.SyncSubmitRequest
import com.example.localmiles.Core.Network.RetrofitClient
import com.example.localmiles.Core.Utils.Result
import com.example.localmiles.Core.Utils.SessionManager

class SyncRepository(
    session: SessionManager,
    private val api: ApiService = RetrofitClient.getApiService(session),
) {

    suspend fun submit(actions: List<SyncActionRequest>): Result<List<SyncResultItem>> =
        apiCall { api.submitSync(SyncSubmitRequest(actions)).results }

    //GET sync — the caller's own queued actions, newest first.
    suspend fun getActions(
        status: String? = null,
        entityType: String? = null,
        limit: Int? = null,
        offset: Int? = null,
    ): Result<List<SyncActionResponse>> =
        apiCall { api.getSyncActions(status, entityType, limit, offset).actions }

    //GET sync/{id} — one action's server-side status by its UUID.
    suspend fun getAction(id: String): Result<SyncActionResponse> =
        apiCall { api.getSyncAction(id).action }

    //POST sync/{id}/retry — re-attempt a Failed action.
    suspend fun retry(id: String): Result<SyncActionResponse> =
        apiCall { api.retrySyncAction(id).action }
}
