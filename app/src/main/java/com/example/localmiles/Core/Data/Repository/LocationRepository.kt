package com.example.localmiles.Core.Data.Repository

import com.example.localmiles.Core.Network.ApiService
import com.example.localmiles.Core.Network.Dto.JobDriverLocationResponse
import com.example.localmiles.Core.Network.Dto.LocationPingRequest
import com.example.localmiles.Core.Network.Dto.LocationPingResponse
import com.example.localmiles.Core.Network.RetrofitClient
import com.example.localmiles.Core.Utils.Result
import com.example.localmiles.Core.Utils.SessionManager

/** Driver live location — the API stores it in Firestore. */
class LocationRepository(
    session: SessionManager,
    private val api: ApiService = RetrofitClient.getApiService(session),
) {

    //POST drivers/me/location — driver only.
    suspend fun reportMyLocation(ping: LocationPingRequest): Result<LocationPingResponse> =
        apiCall { api.postMyLocation(ping) }

    //GET jobs/{id}/driver-location — 404 until the driver has sent a ping.
    suspend fun getJobDriverLocation(jobId: Long): Result<JobDriverLocationResponse> =
        apiCall { api.getJobDriverLocation(jobId) }
}
