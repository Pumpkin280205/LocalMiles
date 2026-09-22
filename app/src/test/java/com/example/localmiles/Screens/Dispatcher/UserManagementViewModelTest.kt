package com.example.localmiles.Screens.Dispatcher

import com.example.localmiles.Core.Data.Repository.DepotRepository
import com.example.localmiles.Core.Data.Repository.DriverRepository
import com.example.localmiles.Core.Data.Repository.UserRepository
import com.example.localmiles.Core.Network.Dto.DepotResponse
import com.example.localmiles.Core.Network.Dto.DriverResponse
import com.example.localmiles.Core.Network.Dto.UserAccountResponse
import com.example.localmiles.Core.Utils.Result
import com.example.localmiles.Core.Utils.Role
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserManagementViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var userRepository: UserRepository
    private lateinit var driverRepository: DriverRepository
    private lateinit var depotRepository: DepotRepository
    private lateinit var viewModel: UserManagementViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        userRepository = mockk()
        driverRepository = mockk()
        depotRepository = mockk()
        // beginPromoteToDriver loads active depots for the dialog's picker whenever
        // it's opened — stub it once here so every test doesn't need to repeat it.
        coEvery { depotRepository.getDepots(active = true) } returns
            Result.Success(listOf(DepotResponse(1, "Main Depot", "1 Main Rd", 0.0, 0.0, null, true)))
        viewModel = UserManagementViewModel(userRepository, driverRepository, depotRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleUser(
        userId: Long = 1,
        role: String = Role.CLIENT,
    ) = UserAccountResponse(
        userId = userId,
        email = "user$userId@localmiles.test",
        firstName = "Test",
        lastName = "User",
        role = role,
        isActive = true,
        createdAt = "2026-01-01T00:00:00",
        lastLoginAt = null,
    )

    @Test
    fun `loadUsers sorts by userId and clears loading`() = runTest(testDispatcher) {
        coEvery { userRepository.getUsers() } returns
            Result.Success(listOf(sampleUser(userId = 2), sampleUser(userId = 1)))

        viewModel.loadUsers()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(1L, 2L), state.users.map { it.userId })
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `setRole to driver opens the promotion dialog instead of patching the role directly`() = runTest(testDispatcher) {
        coEvery { userRepository.getUsers() } returns Result.Success(listOf(sampleUser(userId = 7)))
        viewModel.loadUsers()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setRole(userId = 7, role = Role.DRIVER)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.pendingDriverPromotion)
        assertEquals(7L, viewModel.uiState.value.pendingDriverPromotion?.userId)
        coVerify(exactly = 0) { userRepository.updateUserRole(any(), any()) }
    }

    @Test
    fun `setRole to a non-driver role patches immediately`() = runTest(testDispatcher) {
        coEvery { userRepository.getUsers() } returns Result.Success(listOf(sampleUser(userId = 3, role = Role.CLIENT)))
        viewModel.loadUsers()
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { userRepository.updateUserRole(3, Role.ADMIN) } returns
            Result.Success(sampleUser(userId = 3, role = Role.ADMIN))

        viewModel.setRole(userId = 3, role = Role.ADMIN)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(Role.ADMIN, viewModel.uiState.value.users.first { it.userId == 3L }.role)
        assertNull(viewModel.uiState.value.updatingUserId)
    }

    @Test
    fun `confirmPromoteToDriver leaves the role untouched when the driver record fails to create`() = runTest(testDispatcher) {
        coEvery { userRepository.getUsers() } returns Result.Success(listOf(sampleUser(userId = 7)))
        viewModel.loadUsers()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.setRole(userId = 7, role = Role.DRIVER)
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { driverRepository.createDriver(any()) } returns Result.Error("Conflict: UserId already exists")

        viewModel.confirmPromoteToDriver(
            depotId = 1,
            employeeNumber = "E1",
            phoneNumber = "0000000000",
            licenceNumber = "LIC1",
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Conflict: UserId already exists", viewModel.uiState.value.errorMessage)
        coVerify(exactly = 0) { userRepository.updateUserRole(any(), any()) }
        // Dialog stays open so the dispatcher can see the error and retry.
        assertNotNull(viewModel.uiState.value.pendingDriverPromotion)
    }

    @Test
    fun `confirmPromoteToDriver flips the role once the driver record is created`() = runTest(testDispatcher) {
        val user = sampleUser(userId = 7, role = Role.CLIENT)
        coEvery { userRepository.getUsers() } returns Result.Success(listOf(user))
        viewModel.loadUsers()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.setRole(userId = 7, role = Role.DRIVER)
        testDispatcher.scheduler.advanceUntilIdle()

        val driver = DriverResponse(
            driverId = 17,
            userId = 7,
            depotId = 1,
            employeeNumber = "E1",
            phoneNumber = "0000000000",
            licenceNumber = "LIC1",
            isAvailable = true,
            isActive = true,
        )
        coEvery { driverRepository.createDriver(any()) } returns Result.Success(driver)
        coEvery { userRepository.updateUserRole(7, Role.DRIVER) } returns
            Result.Success(sampleUser(userId = 7, role = Role.DRIVER))

        viewModel.confirmPromoteToDriver(
            depotId = 1,
            employeeNumber = "E1",
            phoneNumber = "0000000000",
            licenceNumber = "LIC1",
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(Role.DRIVER, viewModel.uiState.value.users.first { it.userId == 7L }.role)
        assertNull(viewModel.uiState.value.pendingDriverPromotion)
        assertTrue(viewModel.uiState.value.errorMessage == null)
    }
}
