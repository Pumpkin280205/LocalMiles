package com.example.localmiles.Screens.Dispatcher

import com.example.localmiles.Core.Data.Repository.JobRepository
import com.example.localmiles.Core.Network.ApiService
import com.example.localmiles.Core.Network.Dto.DepotListResponse
import com.example.localmiles.Core.Network.Dto.DepotResponse
import com.example.localmiles.Core.Network.Dto.DriverListResponse
import com.example.localmiles.Core.Network.Dto.DriverResponse
import com.example.localmiles.Core.Network.Dto.VehicleDetailResponse
import com.example.localmiles.Core.Network.Dto.VehicleListResponse
import com.example.localmiles.Core.Network.Dto.VehicleResponse
import com.example.localmiles.Core.Network.Dto.VehicleUpsertRequest
import com.example.localmiles.Core.Network.JobResponse
import com.example.localmiles.Core.Utils.Result
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DispatcherViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var jobRepository: JobRepository
    private lateinit var api: ApiService
    private lateinit var viewModel: DispatcherViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        jobRepository = mockk()
        api = mockk()
        viewModel = DispatcherViewModel(jobRepository, api)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleJob(
        jobId: Long = 1,
        status: String = "Pending",
        vehicleId: Long? = null,
    ) = JobResponse(
        jobId = jobId,
        clientId = 1,
        driverId = null,
        vehicleId = vehicleId,
        depotId = 1,
        scheduledDate = "2026-01-01T10:00:00",
        status = status,
        priority = "Normal",
        notes = null,
        estimatedDistanceKm = null,
        estimatedDurationMinutes = null,
        createdByUserId = 1,
        createdAt = "2026-01-01T00:00:00",
    )

    private fun sampleDriver(driverId: Long = 1) = DriverResponse(
        driverId = driverId,
        userId = 1,
        depotId = 1,
        employeeNumber = "E1",
        phoneNumber = "0000000000",
        licenceNumber = "LIC1",
        isAvailable = true,
        isActive = true,
    )

    private fun sampleVehicle(
        vehicleId: Long = 1,
        isActive: Boolean = true,
    ) = VehicleResponse(
        vehicleId = vehicleId,
        depotId = 1,
        registrationNumber = "CA123456",
        make = "Toyota",
        model = "Hiace",
        vehicleType = "Van",
        capacityKg = 1000.0,
        isAvailable = true,
        isActive = isActive,
    )

    private fun sampleDepot(depotId: Long = 1) = DepotResponse(
        depotId = depotId,
        name = "Main Depot",
        address = "1 Main Rd",
        latitude = 0.0,
        longitude = 0.0,
        contactNumber = null,
        isActive = true,
    )

    @Test
    fun `loadDispatcherData populates jobs, drivers, vehicles and depots on success`() = runTest(testDispatcher) {
        coEvery { jobRepository.getJobs(limit = any(), offset = any()) } returns
            Result.Success(listOf(sampleJob()))
        coEvery { api.getDrivers(active = true) } returns DriverListResponse(listOf(sampleDriver()))
        coEvery { api.getVehicles() } returns VehicleListResponse(listOf(sampleVehicle()))
        coEvery { api.getDepots(active = true) } returns DepotListResponse(listOf(sampleDepot()))

        viewModel.loadDispatcherData()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.jobs.size)
        assertEquals(1, state.drivers.size)
        assertEquals(1, state.vehicles.size)
        assertEquals(1, state.depots.size)
        assertNull(state.errorMessage)
    }

    @Test
    fun `loadDispatcherData surfaces the job error but keeps other data`() = runTest(testDispatcher) {
        coEvery { jobRepository.getJobs(limit = any(), offset = any()) } returns
            Result.Error("Network error")
        coEvery { api.getDrivers(active = true) } returns DriverListResponse(emptyList())
        coEvery { api.getVehicles() } returns VehicleListResponse(emptyList())
        coEvery { api.getDepots(active = true) } returns DepotListResponse(emptyList())

        viewModel.loadDispatcherData()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.jobs.isEmpty())
        assertEquals("Network error", state.errorMessage)
    }

    @Test
    fun `createJob appends the new job and marks save successful`() = runTest(testDispatcher) {
        val created = sampleJob(jobId = 99)
        coEvery { jobRepository.createJob(any()) } returns Result.Success(created)

        viewModel.createJob(
            clientId = 1,
            depotId = 1,
            driverId = null,
            vehicleId = null,
            scheduledDate = "2026-01-01T10:00:00",
            status = "Pending",
            priority = "Normal",
            notes = null,
            estimatedDistanceKm = null,
            estimatedDurationMinutes = null,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.saveSuccessful)
        assertFalse(state.isSaving)
        assertTrue(state.jobs.contains(created))
    }

    @Test
    fun `createJob surfaces the error and does not mark save successful`() = runTest(testDispatcher) {
        coEvery { jobRepository.createJob(any()) } returns Result.Error("Validation failed")

        viewModel.createJob(
            clientId = 1,
            depotId = 1,
            driverId = null,
            vehicleId = null,
            scheduledDate = "2026-01-01T10:00:00",
            status = "Pending",
            priority = "Normal",
            notes = null,
            estimatedDistanceKm = null,
            estimatedDurationMinutes = null,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.saveSuccessful)
        assertEquals("Validation failed", state.errorMessage)
    }

    @Test
    fun `cancelJob replaces the matching job with the cancelled version`() = runTest(testDispatcher) {
        coEvery { jobRepository.getJobs(limit = any(), offset = any()) } returns
            Result.Success(listOf(sampleJob(jobId = 1, status = "Pending")))
        coEvery { api.getDrivers(active = true) } returns DriverListResponse(emptyList())
        coEvery { api.getVehicles() } returns VehicleListResponse(emptyList())
        coEvery { api.getDepots(active = true) } returns DepotListResponse(emptyList())
        viewModel.loadDispatcherData()
        testDispatcher.scheduler.advanceUntilIdle()

        val cancelled = sampleJob(jobId = 1, status = "Cancelled")
        coEvery { jobRepository.cancelJob(1) } returns Result.Success(cancelled)

        viewModel.cancelJob(1)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Cancelled", viewModel.uiState.value.jobs.first { it.jobId == 1L }.status)
    }

    @Test
    fun `setVehicleActive updates the matching vehicle in place`() = runTest(testDispatcher) {
        coEvery { jobRepository.getJobs(limit = any(), offset = any()) } returns Result.Success(emptyList())
        coEvery { api.getDrivers(active = true) } returns DriverListResponse(emptyList())
        coEvery { api.getVehicles() } returns VehicleListResponse(listOf(sampleVehicle(vehicleId = 5, isActive = false)))
        coEvery { api.getDepots(active = true) } returns DepotListResponse(emptyList())
        viewModel.loadDispatcherData()
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { api.updateVehicle(id = 5, body = VehicleUpsertRequest(isActive = true)) } returns
            VehicleDetailResponse(sampleVehicle(vehicleId = 5, isActive = true))

        viewModel.setVehicleActive(vehicleId = 5, active = true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.vehicles.first { it.vehicleId == 5L }.isActive)
    }

    @Test
    fun `setVehicleActive surfaces an error message on failure`() = runTest(testDispatcher) {
        coEvery { api.updateVehicle(id = any(), body = any()) } throws RuntimeException("boom")

        viewModel.setVehicleActive(vehicleId = 5, active = true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("boom", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `createVehicle appends the vehicle and invokes onSuccess`() = runTest(testDispatcher) {
        val created = sampleVehicle(vehicleId = 42)
        coEvery { api.createVehicle(any()) } returns VehicleDetailResponse(created)

        var successCalled = false
        viewModel.createVehicle(
            depotId = 1,
            registrationNumber = "CA123456",
            make = "Toyota",
            model = "Hiace",
            vehicleType = "Van",
            capacityKg = 1000.0,
            onSuccess = { successCalled = true },
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(successCalled)
        assertTrue(viewModel.uiState.value.vehicles.contains(created))
        assertFalse(viewModel.uiState.value.isSavingVehicle)
    }

    @Test
    fun `createVehicle does not invoke onSuccess when the call fails`() = runTest(testDispatcher) {
        coEvery { api.createVehicle(any()) } throws RuntimeException("Conflict")

        var successCalled = false
        viewModel.createVehicle(
            depotId = 1,
            registrationNumber = "CA123456",
            make = "Toyota",
            model = "Hiace",
            vehicleType = "Van",
            capacityKg = 1000.0,
            onSuccess = { successCalled = true },
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(successCalled)
        assertEquals("Conflict", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `updateVehicleDetails updates the matching vehicle and invokes onSuccess`() = runTest(testDispatcher) {
        coEvery { jobRepository.getJobs(limit = any(), offset = any()) } returns Result.Success(emptyList())
        coEvery { api.getDrivers(active = true) } returns DriverListResponse(emptyList())
        coEvery { api.getVehicles() } returns VehicleListResponse(listOf(sampleVehicle(vehicleId = 3)))
        coEvery { api.getDepots(active = true) } returns DepotListResponse(emptyList())
        viewModel.loadDispatcherData()
        testDispatcher.scheduler.advanceUntilIdle()

        val updated = sampleVehicle(vehicleId = 3).copy(make = "Isuzu")
        coEvery { api.updateVehicle(id = 3, body = any()) } returns VehicleDetailResponse(updated)

        var successCalled = false
        viewModel.updateVehicleDetails(
            vehicleId = 3,
            depotId = 1,
            registrationNumber = "CA123456",
            make = "Isuzu",
            model = "Hiace",
            vehicleType = "Van",
            capacityKg = 1000.0,
            onSuccess = { successCalled = true },
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(successCalled)
        assertEquals("Isuzu", viewModel.uiState.value.vehicles.first { it.vehicleId == 3L }.make)
    }
}
