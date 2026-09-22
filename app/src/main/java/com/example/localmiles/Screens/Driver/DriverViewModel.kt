package com.example.localmiles.Screens.Driver

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.localmiles.Core.Data.Repository.JobRepository
import com.example.localmiles.Core.Data.Repository.PodRepository
import com.example.localmiles.Core.Data.Repository.StopRepository
import com.example.localmiles.Core.Network.Dto.StopResponse
import com.example.localmiles.Core.Network.JobResponse
import com.example.localmiles.Core.Utils.Result
import com.example.localmiles.Core.Utils.SessionManager
import kotlinx.coroutines.launch
import java.io.File

class DriverViewModel(app: Application) : AndroidViewModel(app) {
    private val session = SessionManager(app)
    private val jobRepository = JobRepository(session)
    private val stopRepository = StopRepository(session)
    private val podRepository = PodRepository(session)

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
                    jobs = result.data
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
                is Result.Success -> currentJob = jobResult.data
                is Result.Error -> error = jobResult.message
            }

            when (val result = stopRepository.getStopsForJob(jobId)) {
                is Result.Success -> currentJobStops = result.data
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
                    // Refresh jobs list
                    loadMyJobs()
                }
                is Result.Error -> error = result.message
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
                            jobRepository.updateStatus(jobId, "Completed")
                        }
                    }
                    // Refresh stops (and job) for current job
                    loadStopsForJob(jobId)
                }
                is Result.Error -> error = result.message
            }
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
                is Result.Error -> {
                    error = result.message
                    isLoading = false
                    onError(result.message)
                }
            }
        }
    }
}
