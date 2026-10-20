package com.example.localmiles.Screens.Dispatcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.localmiles.Core.Data.Repository.DepotRepository
import com.example.localmiles.Core.Data.Repository.UserRepository

class DispatcherSettingsViewModelFactory(
    private val userRepository: UserRepository,
    private val depotRepository: DepotRepository? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (
            modelClass.isAssignableFrom(
                DispatcherSettingsViewModel::class.java
            )
        ) {
            return DispatcherSettingsViewModel(
                userRepository = userRepository,
                depotRepository = depotRepository
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}