package com.example.localmiles.Core.Network

import com.example.localmiles.Core.Network.Dto.DepotDetailResponse
import com.example.localmiles.Core.Network.Dto.DepotListResponse
import com.example.localmiles.Core.Network.Dto.DepotUpsertRequest
import com.example.localmiles.Core.Network.Dto.DriverDetailResponse
import com.example.localmiles.Core.Network.Dto.DriverListResponse
import com.example.localmiles.Core.Network.Dto.DriverUpsertRequest
import com.example.localmiles.Core.Network.Dto.JobStatusRequest
import com.example.localmiles.Core.Network.Dto.JobUpsertRequest
import com.example.localmiles.Core.Network.Dto.MarkAllReadResponse
import com.example.localmiles.Core.Network.Dto.NotificationCreateRequest
import com.example.localmiles.Core.Network.Dto.NotificationDeleteResponse
import com.example.localmiles.Core.Network.Dto.NotificationDetailResponse
import com.example.localmiles.Core.Network.Dto.NotificationListResponse
import com.example.localmiles.Core.Network.Dto.NotificationReadRequest
import com.example.localmiles.Core.Network.Dto.PodDeleteResponse
import com.example.localmiles.Core.Network.Dto.PodDetailResponse
import com.example.localmiles.Core.Network.Dto.PodListResponse
import com.example.localmiles.Core.Network.Dto.StopDetailResponse
import com.example.localmiles.Core.Network.Dto.StopListResponse
import com.example.localmiles.Core.Network.Dto.StopStatusRequest
import com.example.localmiles.Core.Network.Dto.StopUpsertRequest
import com.example.localmiles.Core.Network.Dto.SyncActionDetailResponse
import com.example.localmiles.Core.Network.Dto.SyncActionListResponse
import com.example.localmiles.Core.Network.Dto.SyncSubmitRequest
import com.example.localmiles.Core.Network.Dto.SyncSubmitResponse
import com.example.localmiles.Core.Network.Dto.UnreadCountResponse
import com.example.localmiles.Core.Network.Dto.UserDetailResponse
import com.example.localmiles.Core.Network.Dto.UserListResponse
import com.example.localmiles.Core.Network.Dto.UserProfileUpdateRequest
import com.example.localmiles.Core.Network.Dto.UserRoleUpdateRequest
import com.example.localmiles.Core.Network.Dto.VehicleDetailResponse
import com.example.localmiles.Core.Network.Dto.VehicleListResponse
import com.example.localmiles.Core.Network.Dto.VehicleUpsertRequest
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // ---------------------------------------------------------------- Auth ----

    // POST auth/login — exchange a Google/Firebase ID token for a session JWT.
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    // ----------------------------------------------------------------- Users ---

    // Admin only — every account and its role.
    @GET("users")
    suspend fun getUsers(): UserListResponse

    // Admin only — promote/demote a user's role.
    @PATCH("users/{id}/role")
    suspend fun updateUserRole(
        @Path("id") id: Long,
        @Body body: UserRoleUpdateRequest
    ): UserDetailResponse

    // Logged-in user — update their own first and last name.
    @PATCH("users/me")
    suspend fun updateCurrentUser(
        @Body body: UserProfileUpdateRequest
    ): UserDetailResponse
    //admin only — every account and its role.
    @GET("users")
    suspend fun getUsers(): UserListResponse

    //admin only — promote/demote a user's role.
    @PATCH("users/{id}/role")
    suspend fun updateUserRole(@Path("id") id: Long, @Body body: UserRoleUpdateRequest): UserDetailResponse

    // --------------------------------------------------------------- Depots ---

    @GET("depots")
    suspend fun getDepots(
        @Query("active") active: Boolean? = null
    ): DepotListResponse

    @GET("depots/{id}")
    suspend fun getDepot(
        @Path("id") id: Long
    ): DepotDetailResponse

    // Admin only.
    @POST("depots")
    suspend fun createDepot(
        @Body body: DepotUpsertRequest
    ): DepotDetailResponse

    // Admin only.
    @PATCH("depots/{id}")
    suspend fun updateDepot(
        @Path("id") id: Long,
        @Body body: DepotUpsertRequest
    ): DepotDetailResponse

    // Admin only — soft delete (IsActive = false).
    @DELETE("depots/{id}")
    suspend fun deactivateDepot(
        @Path("id") id: Long
    ): DepotDetailResponse

    // -------------------------------------------------------------- Drivers ---

    @GET("drivers")
    suspend fun getDrivers(
        @Query("depotId") depotId: Long? = null,
        @Query("available") available: Boolean? = null,
        @Query("active") active: Boolean? = null,
    ): DriverListResponse

    // The Driver row linked to the caller's User.
    @GET("drivers/me")
    suspend fun getMyDriver(): DriverDetailResponse

    @GET("drivers/{id}")
    suspend fun getDriver(
        @Path("id") id: Long
    ): DriverDetailResponse

    // Admin only.
    @POST("drivers")
    suspend fun createDriver(
        @Body body: DriverUpsertRequest
    ): DriverDetailResponse

    // Admin only.
    @PATCH("drivers/{id}")
    suspend fun updateDriver(
        @Path("id") id: Long,
        @Body body: DriverUpsertRequest
    ): DriverDetailResponse

    // Admin only — soft delete (IsActive = false).
    @DELETE("drivers/{id}")
    suspend fun deactivateDriver(
        @Path("id") id: Long
    ): DriverDetailResponse

    // ------------------------------------------------------------- Vehicles ---

    @GET("vehicles")
    suspend fun getVehicles(
        @Query("depotId") depotId: Long? = null,
        @Query("available") available: Boolean? = null,
        @Query("active") active: Boolean? = null,
    ): VehicleListResponse

    @GET("vehicles/{id}")
    suspend fun getVehicle(
        @Path("id") id: Long
    ): VehicleDetailResponse

    // Admin only.
    @POST("vehicles")
    suspend fun createVehicle(
        @Body body: VehicleUpsertRequest
    ): VehicleDetailResponse

    // Admin only.
    @PATCH("vehicles/{id}")
    suspend fun updateVehicle(
        @Path("id") id: Long,
        @Body body: VehicleUpsertRequest
    ): VehicleDetailResponse

    // Admin only — soft delete (IsActive = false).
    @DELETE("vehicles/{id}")
    suspend fun deactivateVehicle(
        @Path("id") id: Long
    ): VehicleDetailResponse

    // ----------------------------------------------------------------- Jobs ---

    @GET("jobs")
    suspend fun getJobs(
        @Query("status") status: String? = null,
        @Query("priority") priority: String? = null,
        @Query("clientId") clientId: Long? = null,
        @Query("driverId") driverId: Long? = null,
        @Query("vehicleId") vehicleId: Long? = null,
        @Query("depotId") depotId: Long? = null,
        @Query("scheduledFrom") scheduledFrom: String? = null,
        @Query("scheduledTo") scheduledTo: String? = null,
        @Query("mine") mine: Boolean? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null,
    ): JobListResponse

    // GET jobs/{id} — envelope { job }.
    @GET("jobs/{id}")
    suspend fun getJob(
        @Path("id") id: Long
    ): JobDetailResponse

    // Admin only.
    @POST("jobs")
    suspend fun createJob(
        @Body body: JobUpsertRequest
    ): JobDetailResponse

    // Admin only — full update incl. driver/vehicle assignment.
    @PATCH("jobs/{id}")
    suspend fun updateJob(
        @Path("id") id: Long,
        @Body body: JobUpsertRequest
    ): JobDetailResponse

    @PATCH("jobs/{id}/status")
    suspend fun updateJobStatus(
        @Path("id") id: Long,
        @Body body: JobStatusRequest
    ): JobDetailResponse

    // Admin only — sets Status = "Cancelled".
    @DELETE("jobs/{id}")
    suspend fun cancelJob(
        @Path("id") id: Long
    ): JobDetailResponse

    // --------------------------------------------------------- DeliveryStops ---

    @GET("jobs/{jobId}/stops")
    suspend fun getStopsForJob(
        @Path("jobId") jobId: Long
    ): StopListResponse

    // Admin only.
    @POST("jobs/{jobId}/stops")
    suspend fun createStop(
        @Path("jobId") jobId: Long,
        @Body body: StopUpsertRequest
    ): StopDetailResponse

    @GET("stops/{id}")
    suspend fun getStop(
        @Path("id") id: Long
    ): StopDetailResponse

    // Admin only — edit planning fields.
    @PATCH("stops/{id}")
    suspend fun updateStop(
        @Path("id") id: Long,
        @Body body: StopUpsertRequest
    ): StopDetailResponse

    @PATCH("stops/{id}/status")
    suspend fun updateStopStatus(
        @Path("id") id: Long,
        @Body body: StopStatusRequest
    ): StopDetailResponse

    // Admin only — sets Status = "Cancelled".
    @DELETE("stops/{id}")
    suspend fun cancelStop(
        @Path("id") id: Long
    ): StopDetailResponse

    // ------------------------------------------------------ ProofOfDelivery ---

    @Multipart
    @POST("stops/{stopId}/pod")
    suspend fun submitPod(
        @Path("stopId") stopId: Long,
        @Part("RecipientName") recipientName: RequestBody,
        @Part("Latitude") latitude: RequestBody,
        @Part("Longitude") longitude: RequestBody,
        @Part("Notes") notes: RequestBody? = null,
        @Part("CapturedAt") capturedAt: RequestBody? = null,
        @Part("DriverId") driverId: RequestBody? = null,
        @Part photo: MultipartBody.Part? = null,
        @Part signature: MultipartBody.Part? = null,
    ): PodDetailResponse

    @GET("stops/{stopId}/pod")
    suspend fun getPodForStop(
        @Path("stopId") stopId: Long
    ): PodDetailResponse

    @GET("jobs/{jobId}/pods")
    suspend fun getPodsForJob(
        @Path("jobId") jobId: Long
    ): PodListResponse

    @GET("pod/{id}")
    suspend fun getPod(
        @Path("id") id: Long
    ): PodDetailResponse

    // Admin only — hard delete + removes the stored files.
    @DELETE("pod/{id}")
    suspend fun deletePod(
        @Path("id") id: Long
    ): PodDeleteResponse

    // -------------------------------------------------------- Notifications ---

    @GET("notifications")
    suspend fun getNotifications(
        @Query("isRead") isRead: Boolean? = null,
        @Query("type") type: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null,
    ): NotificationListResponse

    @GET("notifications/unread-count")
    suspend fun getUnreadNotificationCount(): UnreadCountResponse

    @GET("notifications/{id}")
    suspend fun getNotification(
        @Path("id") id: Long
    ): NotificationDetailResponse

    @POST("notifications")
    suspend fun createNotification(
        @Body body: NotificationCreateRequest
    ): NotificationDetailResponse

    @POST("notifications/read-all")
    suspend fun markAllNotificationsRead(): MarkAllReadResponse

    @PATCH("notifications/{id}")
    suspend fun updateNotification(
        @Path("id") id: Long,
        @Body body: NotificationReadRequest
    ): NotificationDetailResponse

    @DELETE("notifications/{id}")
    suspend fun deleteNotification(
        @Path("id") id: Long
    ): NotificationDeleteResponse

    // --------------------------------------------------------- Offline sync ---

    @POST("sync")
    suspend fun submitSync(
        @Body body: SyncSubmitRequest
    ): SyncSubmitResponse

    @GET("sync")
    suspend fun getSyncActions(
        @Query("status") status: String? = null,
        @Query("entityType") entityType: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null,
    ): SyncActionListResponse

    @GET("sync/{id}")
    suspend fun getSyncAction(
        @Path("id") id: String
    ): SyncActionDetailResponse

    @POST("sync/{id}/retry")
    suspend fun retrySyncAction(
        @Path("id") id: String
    ): SyncActionDetailResponse
}

// Body for POST auth/login.
data class LoginRequest(
    val idToken: String,
)

data class LoginResponse(
    val token: String,
    val user: UserResponse,
)

data class UserResponse(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    // "admin" | "driver" | "client"
    val role: String,
)

// Envelope returned by GET jobs.
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

    // ISO 8601 date/time string.
    @SerializedName("ScheduledDate") val scheduledDate: String,

    // "Pending" | "Assigned" | "EnRoute" | "Completed" | "Cancelled"
    @SerializedName("Status") val status: String,

    // "Low" | "Normal" | "High" | "Urgent"
    @SerializedName("Priority") val priority: String,

    @SerializedName("Notes") val notes: String?,
    @SerializedName("EstimatedDistanceKm") val estimatedDistanceKm: Double?,
    @SerializedName("EstimatedDurationMinutes") val estimatedDurationMinutes: Int?,
    @SerializedName("CreatedByUserId") val createdByUserId: Long,
    @SerializedName("CreatedAt") val createdAt: String,
)