package com.example.localmiles.Screens.Dispatcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.localmiles.Core.Data.Repository.JobRepository
import com.example.localmiles.Core.Data.Repository.friendlyErrorMessage
import com.example.localmiles.Core.Network.ApiService
import com.example.localmiles.Core.Network.Dto.ClientResponse
import com.example.localmiles.Core.Network.Dto.DepotResponse
import com.example.localmiles.Core.Network.Dto.DriverResponse
import com.example.localmiles.Core.Network.Dto.JobDriverLocationResponse
import com.example.localmiles.Core.Network.Dto.JobUpsertRequest
import com.example.localmiles.Core.Network.Dto.StopResponse
import com.example.localmiles.Core.Network.Dto.StopUpsertRequest
import com.example.localmiles.Core.Network.Dto.UserAccountResponse
import com.example.localmiles.Core.Network.Dto.VehicleResponse
import com.example.localmiles.Core.Network.Dto.VehicleUpsertRequest
import com.example.localmiles.Core.Network.JobResponse
import com.example.localmiles.Core.Utils.JobStatus
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

    // Every account, so drivers/clients can be shown by name instead of "#12".
    val users: List<UserAccountResponse> = emptyList(),

    val selectedJob: JobResponse? = null,
    val saveSuccessful: Boolean = false,

    // Set when createJob succeeds so the screen can carry the dispatcher
    // straight on to adding that job's delivery stops.
    val lastCreatedJobId: Long? = null,

    val stopCounts: Map<Long, Int> = emptyMap(),
    val stopsByJob: Map<Long, List<StopResponse>> = emptyMap(),
    // Latest GPS ping (from Firestore, via the API) per active job, keyed by JobId.
    val driverLocations: Map<Long, JobDriverLocationResponse> = emptyMap(),
    // Short-lived confirmation shown after a successful action.
    val infoMessage: String? = null
) {

    fun driverName(driverId: Long?): String {
        if (driverId == null) return "Unassigned"
        val driver = drivers.firstOrNull { it.driverId == driverId }
        val user = driver?.let { d -> users.firstOrNull { it.userId == d.userId } }
        return when {
            user != null -> "${user.firstName} ${user.lastName}".trim()
            driver != null -> "Driver ${driver.employeeNumber}"
            else -> "Driver #$driverId"
        }
    }

    fun clientName(clientId: Long): String =
        clients.firstOrNull { it.clientId == clientId }
            ?.let { "${it.companyName} (${it.contactPerson})" }
            ?: "Client #$clientId"

    fun vehicleName(vehicleId: Long?): String {
        if (vehicleId == null) return "Unassigned"
        return vehicles.firstOrNull { it.vehicleId == vehicleId }
            ?.let { "${it.registrationNumber} · ${it.make} ${it.model}" }
            ?: "Vehicle #$vehicleId"
    }

    fun depotName(depotId: Long): String =
        depots.firstOrNull { it.depotId == depotId }?.name ?: "Depot #$depotId"

    /** Next free sequence number, so "Add stop" never asks the dispatcher to count. */
    fun nextStopSequence(jobId: Long): Int =
        (stopsByJob[jobId]?.maxOfOrNull { it.sequenceNumber } ?: (stopCounts[jobId] ?: 0)) + 1

    /**
     * Why a job can't move to [target] yet, or null if it can. Mirrors the
     * server's transition table and adds the business rules a dispatcher would
     * otherwise only discover from a failed request.
     */
    fun statusBlocker(job: JobResponse, target: String): String? {
        if (!JobStatus.canTransition(job.status, target)) {
            return "A ${JobStatus.label(job.status)} job can't be moved to ${JobStatus.label(target)}."
        }
        if (target == JobStatus.ASSIGNED && job.driverId == null) {
            return "Assign a driver to Job #${job.jobId} (Edit) before marking it Assigned."
        }
        if (target == JobStatus.EN_ROUTE) {
            if (job.driverId == null) return "Job #${job.jobId} has no driver yet."
            if ((stopCounts[job.jobId] ?: 0) == 0) {
                return "Add at least one delivery stop before dispatching Job #${job.jobId}."
            }
        }
        return null
    }
}

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

                // Names only — the screens fall back to IDs if this fails.
                val users = runCatching { api.getUsers().users }
                    .getOrDefault(_uiState.value.users)

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
                    users = users,
                    errorMessage = jobError
                )

                loadStopCounts(jobs.map { it.jobId })
                loadDriverLocations(jobs)

            } catch (e: Exception) {

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = friendlyErrorMessage(e)
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

            val stops = jobIds.associateWith { jobId ->
                when (val result = jobRepository.getStopsForJob(jobId)) {
                    is Result.Success -> result.data.sortedBy { it.sequenceNumber }
                    is Result.Error -> emptyList()
                }
            }

            _uiState.value = _uiState.value.copy(
                stopsByJob = stops,
                stopCounts = stops.mapValues { it.value.size }
            )
        }
    }

    /**
     * Each on-the-road job's real driver position, for the Live Map. Best
     * effort: a job whose driver hasn't sent a ping yet (404), or a server
     * without Firestore configured (503), just falls back to the estimate.
     */
    private fun loadDriverLocations(jobs: List<JobResponse>) {

        viewModelScope.launch {

            val locations = jobs
                .filter {
                    it.driverId != null &&
                        (it.status == JobStatus.EN_ROUTE || it.status == JobStatus.ASSIGNED)
                }
                .mapNotNull { job ->
                    runCatching { api.getJobDriverLocation(job.jobId) }
                        .getOrNull()
                        ?.let { job.jobId to it }
                }
                .toMap()

            _uiState.value = _uiState.value.copy(
                driverLocations = locations
            )
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
                        lastCreatedJobId = result.data.jobId,
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

        val job = _uiState.value.jobs.firstOrNull { it.jobId == jobId }

        if (job != null) {
            _uiState.value.statusBlocker(job, status)?.let { reason ->
                _uiState.value = _uiState.value.copy(errorMessage = reason)
                return
            }
        }

        viewModelScope.launch {

            when (
                val result = jobRepository.updateStatus(
                    id = jobId,
                    status = status
                )
            ) {

                is Result.Success -> {

                    _uiState.value = _uiState.value.copy(
                        infoMessage = "Job #$jobId is now ${JobStatus.label(result.data.status)}.",
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

    fun clearInfoMessage() {

        _uiState.value = _uiState.value.copy(
            infoMessage = null
        )
    }

    fun clearLastCreatedJob() {

        _uiState.value = _uiState.value.copy(
            lastCreatedJobId = null
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
                    errorMessage = friendlyErrorMessage(e)
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
                    errorMessage = friendlyErrorMessage(e)
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
                    errorMessage = friendlyErrorMessage(e)
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

                val created = api.createStop(
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

                val stops = (_uiState.value.stopsByJob[jobId].orEmpty() + created.stop)
                    .sortedBy { it.sequenceNumber }

                _uiState.value = _uiState.value.copy(
                    isSavingStop = false,
                    infoMessage = "Stop ${created.stop.sequenceNumber} added to Job #$jobId.",
                    stopsByJob = _uiState.value.stopsByJob + (jobId to stops),
                    stopCounts = _uiState.value.stopCounts +
                        (jobId to stops.size)
                )
                onSuccess()

            } catch (e: Exception) {

                _uiState.value = _uiState.value.copy(
                    isSavingStop = false,
                    errorMessage = friendlyErrorMessage(e)
                )
            }
        }
    }
}