package com.example.localmiles.Screens.Client

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.localmiles.Core.Data.Repository.JobRepository
import com.example.localmiles.Core.Network.JobResponse
import com.example.localmiles.Core.Utils.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.localmiles.Core.Network.Dto.StopResponse
import android.util.Log

// Represents all the information needed by the client screens
data class ClientDashboardUiState(
    val isLoading: Boolean = false,
    val jobs: List<JobResponse> = emptyList(),
    val selectedJob: JobResponse? = null,
    val stops: List<StopResponse> = emptyList(),
    val errorMessage: String? = null
)

class ClientViewModel(
    private val jobRepository: JobRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClientDashboardUiState())

    val uiState: StateFlow<ClientDashboardUiState> =
        _uiState.asStateFlow()

    // Loads jobs belonging to the currently authenticated client
    fun loadClientJobs() {
        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            when (val result = jobRepository.getJobs(mine = true)) {

                is Result.Success -> {
                    Log.d("ClientViewModel", "Client jobs loaded: ${result.data.size}")

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        jobs = result.data,
                        errorMessage = null
                    )
                }

                is Result.Error -> { // Update the dashboard when the jobs are returned successfully
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun loadJob(jobId: Long) {
        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            when (val result = jobRepository.getJob(jobId)) {

                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedJob = result.data,
                        errorMessage = null
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

    // Loads the selected delivery together with its stops for the client shipment-tracking screen
    fun loadShipmentTracking(jobId: Long) {
        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                stops = emptyList()
            )

            // First retrieve the selected delivery.
            when (val jobResult = jobRepository.getJob(jobId)) {

                is Result.Success -> {

                    // Once the job is available, retrieve its delivery stops.
                    when (val stopsResult = jobRepository.getStopsForJob(jobId)) {

                        is Result.Success -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                selectedJob = jobResult.data,
                                stops = stopsResult.data.sortedBy {
                                    it.sequenceNumber
                                },
                                errorMessage = null
                            )
                        }

                        is Result.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                selectedJob = jobResult.data,
                                errorMessage = stopsResult.message
                            )
                        }
                    }
                }

                is Result.Error -> {

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = jobResult.message
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
}
class ClientViewModelFactory(
    private val jobRepository: JobRepository
) : androidx.lifecycle.ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClientViewModel::class.java)) {
            return ClientViewModel(jobRepository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
