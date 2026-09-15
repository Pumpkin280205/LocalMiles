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

// Represents all the information needed by the client screens
data class ClientDashboardUiState(
    val isLoading: Boolean = false,
    val jobs: List<JobResponse> = emptyList(),
    val selectedJob: JobResponse? = null,
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

    fun clearError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null
        )
    }
}

