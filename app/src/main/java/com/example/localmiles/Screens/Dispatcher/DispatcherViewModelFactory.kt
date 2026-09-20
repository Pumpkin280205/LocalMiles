package com.example.localmiles.Screens.Dispatcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.localmiles.Core.Data.Repository.JobRepository
import com.example.localmiles.Core.Network.ApiService

class DispatcherViewModelFactory(
    private val jobRepository: JobRepository,
    private val api: ApiService
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (modelClass.isAssignableFrom(DispatcherViewModel::class.java)) {
            return DispatcherViewModel(
                jobRepository = jobRepository,
                api = api
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}