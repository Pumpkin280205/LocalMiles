package com.example.localmiles.Core.Data.Repository

import com.example.localmiles.Core.Network.ApiService
import com.example.localmiles.Core.Network.Dto.PodDeleteResponse
import com.example.localmiles.Core.Network.Dto.PodResponse
import com.example.localmiles.Core.Network.RetrofitClient
import com.example.localmiles.Core.Network.toImagePart
import com.example.localmiles.Core.Network.toPlainRequestBody
import com.example.localmiles.Core.Utils.Result
import com.example.localmiles.Core.Utils.SessionManager
import java.io.File

class PodRepository(
    session: SessionManager,
    private val api: ApiService = RetrofitClient.getApiService(session),
) {

    //POST stops/{stopId}/pod — driver (own delivery) or admin.
    suspend fun submitPod(
        stopId: Long,
        recipientName: String,
        latitude: Double,
        longitude: Double,
        notes: String? = null,
        capturedAt: String? = null,
        driverId: Long? = null,
        photo: File? = null,
        signature: File? = null,
    ): Result<PodResponse> =
        apiCall {
            api.submitPod(
                stopId = stopId,
                recipientName = recipientName.toPlainRequestBody(),
                latitude = latitude.toPlainRequestBody(),
                longitude = longitude.toPlainRequestBody(),
                notes = notes?.toPlainRequestBody(),
                capturedAt = capturedAt?.toPlainRequestBody(),
                driverId = driverId?.toPlainRequestBody(),
                photo = photo?.toImagePart("photo"),
                signature = signature?.toImagePart("signature"),
            ).pod
        }

    suspend fun getPodForStop(stopId: Long): Result<PodResponse> =
        apiCall { api.getPodForStop(stopId).pod }

    //Every POD across the job's stops.
    suspend fun getPodsForJob(jobId: Long): Result<List<PodResponse>> =
        apiCall { api.getPodsForJob(jobId).pods }

    suspend fun getPod(id: Long): Result<PodResponse> =
        apiCall { api.getPod(id).pod }

    //DELETE pod/{id} — admin only. Hard delete + removes the stored files.
    suspend fun deletePod(id: Long): Result<PodDeleteResponse> =
        apiCall { api.deletePod(id) }
}
