package com.example.localmiles.Screens.Dispatcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.localmiles.Core.Data.Repository.DepotRepository
import com.example.localmiles.Core.Data.Repository.DriverRepository
import com.example.localmiles.Core.Data.Repository.UserRepository

class UserManagementViewModelFactory(
    private val userRepository: UserRepository,
    private val driverRepository: DriverRepository,
    private val depotRepository: DepotRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (modelClass.isAssignableFrom(UserManagementViewModel::class.java)) {
            return UserManagementViewModel(
                userRepository = userRepository,
                driverRepository = driverRepository,
                depotRepository = depotRepository,
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}