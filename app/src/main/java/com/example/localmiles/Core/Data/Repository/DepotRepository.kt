package com.example.localmiles.Core.Data.Repository

import com.example.localmiles.Core.Network.ApiService
import com.example.localmiles.Core.Network.Dto.DepotResponse
import com.example.localmiles.Core.Network.Dto.DepotUpsertRequest
import com.example.localmiles.Core.Network.RetrofitClient
import com.example.localmiles.Core.Utils.Result
import com.example.localmiles.Core.Utils.SessionManager

class DepotRepository(
    session: SessionManager,
    private val api: ApiService = RetrofitClient.getApiService(session),
) {

    suspend fun getDepots(active: Boolean? = null): Result<List<DepotResponse>> =
        apiCall { api.getDepots(active).depots }

    suspend fun getDepot(id: Long): Result<DepotResponse> =
        apiCall { api.getDepot(id).depot }

    //POST depots — admin only.
    suspend fun createDepot(body: DepotUpsertRequest): Result<DepotResponse> =
        apiCall { api.createDepot(body).depot }

    //PATCH depots/{id} — admin only.
    suspend fun updateDepot(id: Long, body: DepotUpsertRequest): Result<DepotResponse> =
        apiCall { api.updateDepot(id, body).depot }

    //DELETE depots/{id} — admin only. Soft delete (IsActive = false).
    suspend fun deactivateDepot(id: Long): Result<DepotResponse> =
        apiCall { api.deactivateDepot(id).depot }
}
