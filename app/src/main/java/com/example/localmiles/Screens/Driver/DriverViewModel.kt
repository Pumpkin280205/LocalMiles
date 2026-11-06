package com.example.localmiles.Screens.Driver

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.util.Base64
import com.example.localmiles.Core.Data.Repository.JobRepository
import com.example.localmiles.Core.Data.Repository.PodRepository
import com.example.localmiles.Core.Data.Repository.StopRepository
import com.example.localmiles.Core.Location.DriverLocationService
import com.example.localmiles.Core.Network.Dto.StopResponse
import com.example.localmiles.Core.Network.JobResponse
import com.example.localmiles.Core.Utils.JobStatus
import com.example.localmiles.Core.Utils.Result
import com.example.localmiles.Core.Utils.SessionManager
import com.example.localmiles.Screens.Driver.Sync.OfflineQueueManager
import com.example.localmiles.Screens.Driver.Sync.SyncSummary
import com.example.localmiles.Screens.Driver.Sync.overlayJobs
import com.example.localmiles.Screens.Driver.Sync.overlayStops
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.time.Instant

class DriverViewModel(app: Application) : AndroidViewModel(app) {
    private val session = SessionManager(app)
    private val jobRepository = JobRepository(session)
    private val stopRepository = StopRepository(session)
    private val podRepository = PodRepository(session)
    private val queue = OfflineQueueManager.create(app)

    // Pending / failed / synced counts for the sync banner
    val syncSummary = queue.observeSummary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncSummary())

    init {
        // when queued actions finish syncing, reload so we show what the server has
        viewModelScope.launch {
            var lastPending = 0
            queue.observeSummary().collect {
                if (it.pending < lastPending) {
                    loadMyJobs()
                    currentJob?.let { job -> loadStopsForJob(job.jobId) }
                }
                lastPending = it.pending
            }
        }
    }

    fun dismissFailedSync() {
        viewModelScope.launch { queue.dismissFailed() }
    }

    fun retryFailedSync() {
        viewModelScope.launch { queue.retryFailed() }
    }

    // No internet (not a server error) means the action can wait in the offline queue
    private fun Result.Error.isOffline() = cause is IOException

    var jobs by mutableStateOf<List<JobResponse>>(emptyList())
        private set

    var stopCounts by mutableStateOf<Map<Long, Int>>(emptyMap())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var currentJobStops by mutableStateOf<List<StopResponse>>(emptyList())
        private set

    var currentJob by mutableStateOf<JobResponse?>(null)
        private set

    fun loadMyJobs() {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = jobRepository.getJobs(mine = true)) {
                is Result.Success -> {
                    jobs = overlayJobs(result.data, queue.pendingActions())
                    loadStopCounts(result.data.map { it.jobId })
                }
                is Result.Error -> error = result.message
            }
            isLoading = false
        }
    }

    private fun loadStopCounts(jobIds: List<Long>) {
        viewModelScope.launch {
            stopCounts = jobIds.associateWith { jobId ->
                when (val result = stopRepository.getStopsForJob(jobId)) {
                    is Result.Success -> result.data.size
                    is Result.Error -> 0
                }
            }
        }
    }

    fun loadStopsForJob(jobId: Long) {
        viewModelScope.launch {
            isLoading = true
            error = null

            when (val jobResult = jobRepository.getJob(jobId)) {
                is Result.Success -> currentJob = overlayJobs(listOf(jobResult.data), queue.pendingActions()).single()
                is Result.Error -> error = jobResult.message
            }

            when (val result = stopRepository.getStopsForJob(jobId)) {
                is Result.Success -> currentJobStops = overlayStops(result.data, queue.pendingActions())
                is Result.Error -> error = result.message
            }
            isLoading = false
        }
    }

    fun updateJobStatus(jobId: Long, status: String) {
        viewModelScope.launch {
            when (val result = jobRepository.updateStatus(jobId, status)) {
                is Result.Success -> {
                    currentJob = result.data
                    if (JobStatus.isClosed(result.data.status)) stopSharingLocation(jobId)
                    // Refresh jobs list
                    loadMyJobs()
                }
                is Result.Error -> if (result.isOffline()) {
                    queue.enqueue("Job", jobId, "UpdateJobStatus", mapOf("Status" to status))
                    currentJob = currentJob?.copy(status = status)
                    jobs = jobs.map { if (it.jobId == jobId) it.copy(status = status) else it }
                    if (JobStatus.isClosed(status)) stopSharingLocation(jobId)
                } else {
                    error = result.message
                }
            }
        }
    }

    fun updateStopStatus(stopId: Long, jobId: Long, status: String) {
        viewModelScope.launch {
            when (val result = stopRepository.updateStatus(stopId, status)) {
                is Result.Success -> {
                    // Once every stop on this job is Completed, flip the job itself to
                    // Completed too — the client's tracking screen reads job.status, not
                    // individual stop statuses, for its top-level progress display.
                    if (status == "Completed") {
                        val stopsResult = stopRepository.getStopsForJob(jobId)
                        if (stopsResult is Result.Success &&
                            stopsResult.data.isNotEmpty() &&
                            stopsResult.data.all { it.status == "Completed" }
                        ) {
                            if (jobRepository.updateStatus(jobId, "Completed") is Result.Success) {
                                stopSharingLocation(jobId)
                            }
                        }
                    }
                    // Refresh stops (and job) for current job
                    loadStopsForJob(jobId)
                }
                is Result.Error -> if (result.isOffline()) {
                    queue.enqueue("Stop", stopId, "UpdateStopStatus", mapOf("Status" to status))
                    markStopLocally(stopId, jobId, status)
                } else {
                    error = result.message
                }
            }
        }
    }

    // Shows an offline change straight away. When every stop is done the job is queued as
    // Completed too (same as when online)
    private suspend fun markStopLocally(stopId: Long, jobId: Long, status: String) {
        currentJobStops = currentJobStops.map { if (it.stopId == stopId) it.copy(status = status) else it }
        if (status == "Completed" && currentJobStops.isNotEmpty() &&
            currentJobStops.all { it.status == "Completed" }
        ) {
            queue.enqueue("Job", jobId, "UpdateJobStatus", mapOf("Status" to "Completed"))
            currentJob = currentJob?.copy(status = "Completed")
            stopSharingLocation(jobId)
        }
    }

    // The job is finished, so the dispatcher no longer needs the driver's position.
    private fun stopSharingLocation(jobId: Long) {
        if (DriverLocationService.trackingJobId.value == jobId) {
            DriverLocationService.stop(getApplication())
        }
    }

    fun submitPod(
        stopId: Long,
        jobId: Long,
        recipientName: String,
        latitude: Double,
        longitude: Double,
        notes: String?,
        photo: File?,
        signature: File?,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {},
    ) {
        viewModelScope.launch {
            isLoading = true
            val result = podRepository.submitPod(
                stopId = stopId,
                recipientName = recipientName,
                latitude = latitude,
                longitude = longitude,
                notes = notes,
                photo = photo,
                signature = signature
            )
            when (result) {
                is Result.Success -> {
                    // Mark stop as completed if POD is successful
                    updateStopStatus(stopId, jobId, "Completed")
                    isLoading = false
                    onSuccess()
                }
                is Result.Error -> if (result.isOffline()) {
                    // the API marks the stop Completed itself when it applies a SubmitPod
                    val payload = mutableMapOf<String, Any>(
                        "RecipientName" to recipientName,
                        "Latitude" to latitude,
                        "Longitude" to longitude,
                        "CapturedAt" to Instant.now().toString(),
                    )
                    notes?.takeIf { it.isNotBlank() }?.let { payload["Notes"] = it }
                    photo?.let { payload["Photo"] = it.toOfflineImage() }
                    signature?.let { payload["Signature"] = it.toOfflineImage() }
                    queue.enqueue("Stop", stopId, "SubmitPod", payload)
                    markStopLocally(stopId, jobId, "Completed")
                    isLoading = false
                    onSuccess()
                } else {
                    error = result.message
                    isLoading = false
                    onError(result.message)
                }
            }
        }
    }
}

// Offline POD images are sent inside the sync payload as { Base64, MimeType }
private fun File.toOfflineImage() = mapOf(
    "Base64" to Base64.encodeToString(readBytes(), Base64.NO_WRAP),
    "MimeType" to if (extension.equals("png", ignoreCase = true)) "image/png" else "image/jpeg",
)
