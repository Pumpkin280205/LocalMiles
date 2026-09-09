package com.example.localmiles.Core.Data.Repository

import com.example.localmiles.Core.Network.ApiService
import com.example.localmiles.Core.Network.JobResponse
import com.example.localmiles.Core.Network.RetrofitClient
import com.example.localmiles.Core.Utils.Result
import com.example.localmiles.Core.Utils.SessionManager

/**
 * Read access to delivery jobs. Unwraps the API's { jobs, ... } / { job }
 * envelopes so callers just get the payload.
 *
 * Call site until Hilt is wired up:
 *     val jobRepo = JobRepository(SessionManager(context))
 */
class JobRepository(
    session: SessionManager,
    private val api: ApiService = RetrofitClient.getApiService(session),
) {

    /** GET jobs — every job visible to the logged-in user's role. */
    suspend fun getJobs(): Result<List<JobResponse>> =
        apiCall { api.getJobs().jobs }

    /** GET jobs/{id} — one job, or Result.Error if it is missing / not visible. */
    suspend fun getJob(id: String): Result<JobResponse> =
        apiCall { api.getJob(id).job }
}
