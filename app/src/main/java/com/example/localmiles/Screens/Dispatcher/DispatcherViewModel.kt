package com.example.localmiles.Screens.Dispatcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.localmiles.Core.Data.Repository.JobRepository
import com.example.localmiles.Core.Network.ApiService
import com.example.localmiles.Core.Network.Dto.ClientResponse
import com.example.localmiles.Core.Network.Dto.DepotResponse
import com.example.localmiles.Core.Network.Dto.DriverResponse
import com.example.localmiles.Core.Network.Dto.JobUpsertRequest
import com.example.localmiles.Core.Network.Dto.StopUpsertRequest
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
    val isSavingVehicle: Boolean = false,
    val isSavingStop: Boolean = false,
    val errorMessage: String? = null,

    val jobs: List<JobResponse> = emptyList(),
    val drivers: List<DriverResponse> = emptyList(),
    val vehicles: List<VehicleResponse> = emptyList(),
    val depots: List<DepotResponse> = emptyList(),
    val clients: List<ClientResponse> = emptyList(),

    val selectedJob: JobResponse? = null,
    val saveSuccessful: Boolean = false,

    val stopCounts: Map<Long, Int> = emptyMap()
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

                val clients = api.getClients().clients

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
                    clients = clients,
                    errorMessage = jobError
                )

                loadStopCounts(jobs.map { it.jobId })

            } catch (e: Exception) {

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Unable to load dispatcher data."
                )
            }
        }
    }

    /**
     * How many delivery stops each job has, shown as a badge on its card so a
     * dispatcher can see whether a job is still missing stops.
     */
    private fun loadStopCounts(jobIds: List<Long>) {

        viewModelScope.launch {

            val counts = jobIds.associateWith { jobId ->
                when (val result = jobRepository.getStopsForJob(jobId)) {
                    is Result.Success -> result.data.size
                    is Result.Error -> 0
                }
            }

            _uiState.value = _uiState.value.copy(stopCounts = counts)
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


    /**
     * Creates a new vehicle from Fleet Management.
     */
    fun createVehicle(
        depotId: Long,
        registrationNumber: String,
        make: String,
        model: String,
        vehicleType: String,
        capacityKg: Double,
        onSuccess: () -> Unit
    ) {

        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(
                isSavingVehicle = true,
                errorMessage = null
            )

            try {

                val response = api.createVehicle(
                    VehicleUpsertRequest(
                        depotId = depotId,
                        registrationNumber = registrationNumber,
                        make = make,
                        model = model,
                        vehicleType = vehicleType,
                        capacityKg = capacityKg,
                        isAvailable = true,
                        isActive = true
                    )
                )

                _uiState.value = _uiState.value.copy(
                    isSavingVehicle = false,
                    vehicles = _uiState.value.vehicles + response.vehicle
                )

                onSuccess()

            } catch (e: Exception) {

                _uiState.value = _uiState.value.copy(
                    isSavingVehicle = false,
                    errorMessage = e.message ?: "Unable to create vehicle."
                )
            }
        }
    }


    /**
     * Updates an existing vehicle's details from Fleet Management.
     */
    fun updateVehicleDetails(
        vehicleId: Long,
        depotId: Long,
        registrationNumber: String,
        make: String,
        model: String,
        vehicleType: String,
        capacityKg: Double,
        onSuccess: () -> Unit
    ) {

        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(
                isSavingVehicle = true,
                errorMessage = null
            )

            try {

                val response = api.updateVehicle(
                    id = vehicleId,
                    body = VehicleUpsertRequest(
                        depotId = depotId,
                        registrationNumber = registrationNumber,
                        make = make,
                        model = model,
                        vehicleType = vehicleType,
                        capacityKg = capacityKg
                    )
                )

                _uiState.value = _uiState.value.copy(
                    isSavingVehicle = false,
                    vehicles = _uiState.value.vehicles.map { vehicle ->

                        if (vehicle.vehicleId == vehicleId) {
                            response.vehicle
                        } else {
                            vehicle
                        }
                    }
                )

                onSuccess()

            } catch (e: Exception) {

                _uiState.value = _uiState.value.copy(
                    isSavingVehicle = false,
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

    /**
     * Adds a delivery stop to a job so a driver has something to mark Arrived /
     * Completed against on the Stop Details screen.
     */
    fun createStop(
        jobId: Long,
        sequenceNumber: Int,
        recipientName: String,
        address: String,
        latitude: Double,
        longitude: Double,
        instructions: String?,
        onSuccess: () -> Unit
    ) {

        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(
                isSavingStop = true,
                errorMessage = null
            )

            try {

                api.createStop(
                    jobId,
                    StopUpsertRequest(
                        sequenceNumber = sequenceNumber,
                        recipientName = recipientName,
                        address = address,
                        latitude = latitude,
                        longitude = longitude,
                        instructions = instructions
                    )
                )

                _uiState.value = _uiState.value.copy(
                    isSavingStop = false,
                    stopCounts = _uiState.value.stopCounts +
                        (jobId to ((_uiState.value.stopCounts[jobId] ?: 0) + 1))
                )
                onSuccess()

            } catch (e: Exception) {

                _uiState.value = _uiState.value.copy(
                    isSavingStop = false,
                    errorMessage = e.message ?: "Unable to add stop."
                )
            }
        }
    }
}