package com.example.localmiles.Core.Network

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse
    @GET("jobs")
    suspend fun getJobs(): JobListResponse

    //GET jobs/{id} — a single job. Response is an envelope: { job }.
    // @GET("jobs/{id}")
    suspend fun getJob(@Path("id") id: String): JobDetailResponse
}

//Body for POST auth/login. `idToken` is the Google ID token from Credential Manager.
data class LoginRequest(
    val idToken: String,
)

data class LoginResponse(
    val token: String,
    val user: UserResponse,
)

data class UserResponse(
    // Numeric UserId, serialized by the API as a JSON number; Gson reads it as a String.
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    //"admin" | "driver" | "client"
    val role: String,
)

//Envelope returned by GET jobs.
data class JobListResponse(
    val jobs: List<JobResponse>,
    val limit: Int,
    val offset: Int,
)

// Envelope returned by GET jobs/{id}.
data class JobDetailResponse(
    val job: JobResponse,
)

data class JobResponse(
    @SerializedName("JobId") val jobId: Long,
    @SerializedName("ClientId") val clientId: Long,
    @SerializedName("DriverId") val driverId: Long?,
    @SerializedName("VehicleId") val vehicleId: Long?,
    @SerializedName("DepotId") val depotId: Long,
    /** ISO 8601 date/time string. */
    @SerializedName("ScheduledDate") val scheduledDate: String,
    /** "Pending" | "Assigned" | "EnRoute" | "Completed" | "Cancelled" */
    @SerializedName("Status") val status: String,
    /** "Low" | "Normal" | "High" | "Urgent" */
    @SerializedName("Priority") val priority: String,
    @SerializedName("Notes") val notes: String?,
    @SerializedName("EstimatedDistanceKm") val estimatedDistanceKm: Double?,
    @SerializedName("EstimatedDurationMinutes") val estimatedDurationMinutes: Int?,
    @SerializedName("CreatedByUserId") val createdByUserId: Long,
    @SerializedName("CreatedAt") val createdAt: String,
)
