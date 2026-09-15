package com.example.localmiles.Core.Data.Repository

import com.example.localmiles.Core.Network.ApiService
import com.example.localmiles.Core.Network.Dto.JobStatusRequest
import com.example.localmiles.Core.Network.Dto.JobUpsertRequest
import com.example.localmiles.Core.Network.JobResponse
import com.example.localmiles.Core.Network.RetrofitClient
import com.example.localmiles.Core.Utils.Result
import com.example.localmiles.Core.Utils.SessionManager
import com.example.localmiles.Core.Network.Dto.StopResponse

class JobRepository(
    session: SessionManager,
    private val api: ApiService = RetrofitClient.getApiService(session),
) {

    suspend fun getJobs(
        status: String? = null,
        priority: String? = null,
        clientId: Long? = null,
        driverId: Long? = null,
        vehicleId: Long? = null,
        depotId: Long? = null,
        scheduledFrom: String? = null,
        scheduledTo: String? = null,
        mine: Boolean? = null,
        limit: Int? = null,
        offset: Int? = null,
    ): Result<List<JobResponse>> =
        apiCall {
            api.getJobs(
                status, priority, clientId, driverId, vehicleId, depotId,
                scheduledFrom, scheduledTo, mine, limit, offset,
            ).jobs
        }

    //GET jobs/{id}.
    suspend fun getJob(id: Long): Result<JobResponse> =
        apiCall { api.getJob(id).job }

    // Retrieves the delivery stops for a jobn
// Used by client tracking to display the shipment's delivery progress
    suspend fun getStopsForJob(jobId: Long): Result<List<StopResponse>> =
        apiCall {
            api.getStopsForJob(jobId).stops
        }

    // POST jobs — admin only.
    suspend fun createJob(body: JobUpsertRequest): Result<JobResponse> =
        apiCall { api.createJob(body).job }

    //PATCH jobs/{id} — admin only. Only the non-null fields of [body] are sent.
    suspend fun updateJob(id: Long, body: JobUpsertRequest): Result<JobResponse> =
        apiCall { api.updateJob(id, body).job }

    //PATCH jobs/{id}/status — assigned driver or admin.
    suspend fun updateStatus(id: Long, status: String): Result<JobResponse> =
        apiCall { api.updateJobStatus(id, JobStatusRequest(status)).job }

    //DELETE jobs/{id} — admin only. Sets Status = "Cancelled".
    suspend fun cancelJob(id: Long): Result<JobResponse> =
        apiCall { api.cancelJob(id).job }
}
