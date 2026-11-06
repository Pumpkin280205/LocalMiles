package com.example.localmiles.Screens.Driver

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.localmiles.Core.Data.Repository.DepotRepository
import com.example.localmiles.Core.Data.Repository.DriverRepository
import com.example.localmiles.Core.Network.Dto.DriverResponse
import com.example.localmiles.Core.Utils.Result
import com.example.localmiles.Core.Utils.SessionManager
import kotlinx.coroutines.launch

class DriverSettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val session = SessionManager(app)
    private val driverRepository = DriverRepository(session)
    private val depotRepository = DepotRepository(session)

    var driver by mutableStateOf<DriverResponse?>(null)
        private set

    var depotName by mutableStateOf<String?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    fun loadProfile() {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = driverRepository.getMyDriver()) {
                is Result.Success -> {
                    driver = result.data
                    val depot = depotRepository.getDepot(result.data.depotId)
                    if (depot is Result.Success) depotName = depot.data.name
                }
                is Result.Error -> error = result.message
            }
            isLoading = false
        }
    }
}
