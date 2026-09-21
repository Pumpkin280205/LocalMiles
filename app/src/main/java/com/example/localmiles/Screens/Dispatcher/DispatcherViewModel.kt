package com.example.localmiles.Screens.Dispatcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.localmiles.Core.Data.Repository.JobRepository
import com.example.localmiles.Core.Network.ApiService
import com.example.localmiles.Core.Network.Dto.DepotResponse
import com.example.localmiles.Core.Network.Dto.DriverResponse
import com.example.localmiles.Core.Network.Dto.JobUpsertRequest
import com.example.localmiles.Core.Network.Dto.VehicleResponse
import com.example.localmiles.Core.Network.Dto.VehicleUpsertRequest
import com.example.localmiles.Core.Network.JobResponse
import com.example.localmiles.Core.Utils.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DispatcherUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,

    val jobs: List<JobResponse> = emptyList(),
    val drivers: List<DriverResponse> = emptyList(),
    val vehicles: List<VehicleResponse> = emptyList(),
    val depots: List<DepotResponse> = emptyList(),

    val selectedJob: JobResponse? = null,
    val saveSuccessful: Boolean = false
)

class DispatcherViewModel(
    private val jobRepository: JobRepository,
    private val api: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DispatcherUiState()
    )

    val uiState: StateFlow<DispatcherUiState> =
        _uiState.asStateFlow()


    /**
     * Loads jobs, drivers, vehicles and depots.
     */
    fun loadDispatcherData() {

        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {

                val jobsResult = jobRepository.getJobs(
                    limit = 100,
                    offset = 0
                )

                val driversResponse = api.getDrivers(
                    active = true
                )

                // Unfiltered so Fleet Management can also show (and reactivate)
                // inactive vehicles — CreateJobScreen filters isActive itself.
                val vehiclesResponse = api.getVehicles()

                val depotsResponse = api.getDepots(
                    active = true
                )

                val jobs = when (jobsResult) {

                    is Result.Success ->
                        jobsResult.data

                    is Result.Error ->
                        emptyList()
                }

                val jobError = when (jobsResult) {

                    is Result.Success ->
                        null

                    is Result.Error ->
                        jobsResult.message
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    jobs = jobs,
                    drivers = driversResponse.drivers,
                    vehicles = vehiclesResponse.vehicles,
                    depots = depotsResponse.depots,
                    errorMessage = jobError
                )

            } catch (e: Exception) {

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Unable to load dispatcher data."
                )
            }
        }
    }


    /**
     * Loads a single job for editing.
     */
    fun loadJob(jobId: Long) {

        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            when (
                val result = jobRepository.getJob(jobId)
            ) {

                is Result.Success -> {

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedJob = result.data
                    )
                }

                is Result.Error -> {

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }


    /**
     * Creates a new delivery job.
     */
    fun createJob(
        clientId: Long?,
        depotId: Long?,
        driverId: Long?,
        vehicleId: Long?,
        scheduledDate: String?,
        status: String?,
        priority: String?,
        notes: String?,
        estimatedDistanceKm: Double?,
        estimatedDurationMinutes: Int?
    ) {

        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(
                isSaving = true,
                errorMessage = null,
                saveSuccessful = false
            )

            val request = JobUpsertRequest(
                clientId = clientId,
                depotId = depotId,
                driverId = driverId,
                vehicleId = vehicleId,
                scheduledDate = scheduledDate,
                status = status,
                priority = priority,
                notes = notes,
                estimatedDistanceKm = estimatedDistanceKm,
                estimatedDurationMinutes = estimatedDurationMinutes
            )

            when (
                val result = jobRepository.createJob(request)
            ) {

                is Result.Success -> {

                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        saveSuccessful = true,
                        jobs = _uiState.value.jobs + result.data
                    )
                }

                is Result.Error -> {

                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }


    /**
     * Updates an existing delivery job.
     */
    fun updateJob(
        jobId: Long,
        clientId: Long?,
        depotId: Long?,
        driverId: Long?,
        vehicleId: Long?,
        scheduledDate: String?,
        status: String?,
        priority: String?,
        notes: String?,
        estimatedDistanceKm: Double?,
        estimatedDurationMinutes: Int?
    ) {

        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(
                isSaving = true,
                errorMessage = null,
                saveSuccessful = false
            )

            val request = JobUpsertRequest(
                clientId = clientId,
                depotId = depotId,
                driverId = driverId,
                vehicleId = vehicleId,
                scheduledDate = scheduledDate,
                status = status,
                priority = priority,
                notes = notes,
                estimatedDistanceKm = estimatedDistanceKm,
                estimatedDurationMinutes = estimatedDurationMinutes
            )

            when (
                val result = jobRepository.updateJob(
                    id = jobId,
                    body = request
                )
            ) {

                is Result.Success -> {

                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        saveSuccessful = true,
                        jobs = _uiState.value.jobs.map { job ->

                            if (job.jobId == jobId) {
                                result.data
                            } else {
                                job
                            }
                        }
                    )
                }

                is Result.Error -> {

                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }


    /**
     * Updates the status of a job.
     */
    fun updateJobStatus(
        jobId: Long,
        status: String
    ) {

        viewModelScope.launch {

            when (
                val result = jobRepository.updateStatus(
                    id = jobId,
                    status = status
                )
            ) {

                is Result.Success -> {

                    _uiState.value = _uiState.value.copy(
                        jobs = _uiState.value.jobs.map { job ->

                            if (job.jobId == jobId) {
                                result.data
                            } else {
                                job
                            }
                        }
                    )
                }

                is Result.Error -> {

                    _uiState.value = _uiState.value.copy(
                        errorMessage = result.message
                    )
                }
            }
        }
    }


    /**
     * Cancels a job.
     */
    fun cancelJob(
        jobId: Long
    ) {

        viewModelScope.launch {

            when (
                val result = jobRepository.cancelJob(jobId)
            ) {

                is Result.Success -> {

                    _uiState.value = _uiState.value.copy(
                        jobs = _uiState.value.jobs.map { job ->

                            if (job.jobId == jobId) {
                                result.data
                            } else {
                                job
                            }
                        }
                    )
                }

                is Result.Error -> {

                    _uiState.value = _uiState.value.copy(
                        errorMessage = result.message
                    )
                }
            }
        }
    }


    fun clearError() {

        _uiState.value = _uiState.value.copy(
            errorMessage = null
        )
    }


    /**
     * Activates or deactivates a vehicle from Fleet Management.
     */
    fun setVehicleActive(
        vehicleId: Long,
        active: Boolean
    ) {

        viewModelScope.launch {

            try {

                val response = api.updateVehicle(
                    id = vehicleId,
                    body = VehicleUpsertRequest(isActive = active)
                )

                _uiState.value = _uiState.value.copy(
                    vehicles = _uiState.value.vehicles.map { vehicle ->

                        if (vehicle.vehicleId == vehicleId) {
                            response.vehicle
                        } else {
                            vehicle
                        }
                    }
                )

            } catch (e: Exception) {

                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Unable to update vehicle."
                )
            }
        }
    }


    fun clearSaveSuccessful() {

        _uiState.value = _uiState.value.copy(
            saveSuccessful = false
        )
    }
}