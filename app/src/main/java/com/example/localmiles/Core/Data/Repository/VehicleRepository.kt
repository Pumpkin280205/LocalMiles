package com.example.localmiles.Core.Data.Repository

import com.example.localmiles.Core.Network.ApiService
import com.example.localmiles.Core.Network.Dto.VehicleResponse
import com.example.localmiles.Core.Network.Dto.VehicleUpsertRequest
import com.example.localmiles.Core.Network.RetrofitClient
import com.example.localmiles.Core.Utils.Result
import com.example.localmiles.Core.Utils.SessionManager

class VehicleRepository(
    session: SessionManager,
    private val api: ApiService = RetrofitClient.getApiService(session),
) {

    suspend fun getVehicles(
        depotId: Long? = null,
        available: Boolean? = null,
        active: Boolean? = null,
    ): Result<List<VehicleResponse>> =
        apiCall { api.getVehicles(depotId, available, active).vehicles }

    suspend fun getVehicle(id: Long): Result<VehicleResponse> =
        apiCall { api.getVehicle(id).vehicle }

    //POST vehicles — admin only.
    suspend fun createVehicle(body: VehicleUpsertRequest): Result<VehicleResponse> =
        apiCall { api.createVehicle(body).vehicle }

    //PATCH vehicles/{id} — admin only.
    suspend fun updateVehicle(id: Long, body: VehicleUpsertRequest): Result<VehicleResponse> =
        apiCall { api.updateVehicle(id, body).vehicle }

    //Convenience: flip a vehicle's availability (admin only).
    suspend fun setAvailable(id: Long, available: Boolean): Result<VehicleResponse> =
        apiCall { api.updateVehicle(id, VehicleUpsertRequest(isAvailable = available)).vehicle }

    //DELETE vehicles/{id} — admin only. Soft delete (IsActive = false).
    suspend fun deactivateVehicle(id: Long): Result<VehicleResponse> =
        apiCall { api.deactivateVehicle(id).vehicle }
}
