package com.example.localmiles.Core.Data.Repository

import com.example.localmiles.Core.Network.ApiService
import com.example.localmiles.Core.Network.Dto.StopResponse
import com.example.localmiles.Core.Network.Dto.StopStatusRequest
import com.example.localmiles.Core.Network.Dto.StopUpsertRequest
import com.example.localmiles.Core.Network.RetrofitClient
import com.example.localmiles.Core.Utils.Result
import com.example.localmiles.Core.Utils.SessionManager

class StopRepository(
    session: SessionManager,
    private val api: ApiService = RetrofitClient.getApiService(session),
) {

    suspend fun getStopsForJob(jobId: Long): Result<List<StopResponse>> =
        apiCall { api.getStopsForJob(jobId).stops }

    suspend fun getStop(id: Long): Result<StopResponse> =
        apiCall { api.getStop(id).stop }

    //POST jobs/{jobId}/stops — admin only.
    suspend fun createStop(jobId: Long, body: StopUpsertRequest): Result<StopResponse> =
        apiCall { api.createStop(jobId, body).stop }

    //PATCH stops/{id} — admin only. Edits planning fields.
    suspend fun updateStop(id: Long, body: StopUpsertRequest): Result<StopResponse> =
        apiCall { api.updateStop(id, body).stop }

    //PATCH stops/{id}/status — assigned driver or admin.
    suspend fun updateStatus(id: Long, status: String): Result<StopResponse> =
        apiCall { api.updateStopStatus(id, StopStatusRequest(status)).stop }

    //DELETE stops/{id} — admin only. Sets Status = "Cancelled".
    suspend fun cancelStop(id: Long): Result<StopResponse> =
        apiCall { api.cancelStop(id).stop }
}
