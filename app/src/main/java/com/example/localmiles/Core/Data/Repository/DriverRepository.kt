package com.example.localmiles.Core.Data.Repository

import com.example.localmiles.Core.Network.ApiService
import com.example.localmiles.Core.Network.Dto.DriverResponse
import com.example.localmiles.Core.Network.Dto.DriverUpsertRequest
import com.example.localmiles.Core.Network.RetrofitClient
import com.example.localmiles.Core.Utils.Result
import com.example.localmiles.Core.Utils.SessionManager

class DriverRepository(
    session: SessionManager,
    private val api: ApiService = RetrofitClient.getApiService(session),
) {

    //GET drivers with optional filters.
    suspend fun getDrivers(
        depotId: Long? = null,
        available: Boolean? = null,
        active: Boolean? = null,
    ): Result<List<DriverResponse>> =
        apiCall { api.getDrivers(depotId, available, active).drivers }

    suspend fun getMyDriver(): Result<DriverResponse> =
        apiCall { api.getMyDriver().driver }

    //GET drivers/{id}.
    suspend fun getDriver(id: Long): Result<DriverResponse> =
        apiCall { api.getDriver(id).driver }

    //POST drivers — admin only.
    suspend fun createDriver(body: DriverUpsertRequest): Result<DriverResponse> =
        apiCall { api.createDriver(body).driver }

    //PATCH drivers/{id} — admin only.
    suspend fun updateDriver(id: Long, body: DriverUpsertRequest): Result<DriverResponse> =
        apiCall { api.updateDriver(id, body).driver }

    //Convenience: flip a driver's availability (admin only).
    suspend fun setAvailable(id: Long, available: Boolean): Result<DriverResponse> =
        apiCall { api.updateDriver(id, DriverUpsertRequest(isAvailable = available)).driver }

    //DELETE drivers/{id} — admin only. Soft delete (IsActive = false).
    suspend fun deactivateDriver(id: Long): Result<DriverResponse> =
        apiCall { api.deactivateDriver(id).driver }
}
