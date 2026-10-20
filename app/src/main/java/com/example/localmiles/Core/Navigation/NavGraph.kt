package com.example.localmiles.Core.Navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

import com.example.localmiles.Core.Data.Repository.DepotRepository
import com.example.localmiles.Core.Data.Repository.DriverRepository
import com.example.localmiles.Core.Data.Repository.JobRepository
import com.example.localmiles.Core.Data.Repository.UserRepository
import com.example.localmiles.Core.Network.RetrofitClient
import com.example.localmiles.Core.Utils.SessionManager

import com.example.localmiles.Screens.Auth.AuthViewModel
import com.example.localmiles.Screens.Auth.LoginScreen
import com.example.localmiles.Screens.Auth.SplashScreen

import com.example.localmiles.Screens.Client.ClientDashboardScreen
import com.example.localmiles.Screens.Client.ClientViewModel
import com.example.localmiles.Screens.Client.ClientViewModelFactory
import com.example.localmiles.Screens.Client.SettingsScreen
import com.example.localmiles.Screens.Client.TrackShipmentScreen

import com.example.localmiles.Screens.Dispatcher.CreateJobScreen
import com.example.localmiles.Screens.Dispatcher.DashboardScreen
import com.example.localmiles.Screens.Dispatcher.DeliveryHistoryScreen
import com.example.localmiles.Screens.Dispatcher.DispatcherSettingsScreen
import com.example.localmiles.Core.Utils.DispatcherPreferences
import com.example.localmiles.Core.Utils.JobStatus
import com.example.localmiles.Screens.Dispatcher.DispatcherSettingsViewModel
import com.example.localmiles.Screens.Dispatcher.DispatcherSettingsViewModelFactory
import com.example.localmiles.Screens.Dispatcher.DispatcherViewModel
import com.example.localmiles.Screens.Dispatcher.DispatcherViewModelFactory
import com.example.localmiles.Screens.Dispatcher.FleetManagementScreen
import com.example.localmiles.Screens.Dispatcher.JobListScreen
import com.example.localmiles.Screens.Dispatcher.LiveMapScreen
import com.example.localmiles.Screens.Dispatcher.UserManagementScreen
import com.example.localmiles.Screens.Dispatcher.UserManagementViewModel
import com.example.localmiles.Screens.Dispatcher.UserManagementViewModelFactory

import com.example.localmiles.Screens.Driver.DriverDashboardScreen
import com.example.localmiles.Screens.Driver.DriverSettingsScreen
import com.example.localmiles.Screens.Driver.DriverViewModel
import com.example.localmiles.Screens.Driver.MyJobsScreen
import com.example.localmiles.Screens.Driver.PodCaptureScreen
import com.example.localmiles.Screens.Driver.StopDetailsScreen


@Composable
fun NavGraph(
    nav: NavHostController = rememberNavController()
) {

    NavHost(
        navController = nav,
        startDestination = Routes.SPLASH
    ) {

        // ========================================================
        // AUTHENTICATION
        // ========================================================

        composable(Routes.SPLASH) {

            SplashScreen(
                onResolved = { role ->
                    nav.goToHome(role)
                }
            )
        }

        composable(Routes.LOGIN) {

            LoginScreen(
                onSignedIn = { role ->
                    nav.goToHome(role)
                }
            )
        }


        // ========================================================
        // DISPATCHER DASHBOARD
        // ========================================================

        composable(Routes.DISPATCHER_DASHBOARD) {

            val context = LocalContext.current

            val sessionManager = remember {
                SessionManager(context)
            }

            val apiService = remember {
                RetrofitClient.getApiService(sessionManager)
            }

            val jobRepository = remember {
                JobRepository(
                    session = sessionManager,
                    api = apiService
                )
            }

            val dispatcherViewModel: DispatcherViewModel = viewModel(
                factory = DispatcherViewModelFactory(
                    jobRepository = jobRepository,
                    api = apiService
                )
            )

            val dispatcherUiState by dispatcherViewModel.uiState.collectAsState()

            LaunchedEffect(Unit) {
                dispatcherViewModel.loadDispatcherData()
            }

            DashboardScreen(

                // ------------------------------------------------
                // REAL API DATA FOR DASHBOARD CARDS
                // ------------------------------------------------

                activeJobsCount = dispatcherUiState.jobs.count { job ->
                    !job.status.equals(
                        "Completed",
                        ignoreCase = true
                    ) &&
                            !job.status.equals(
                                "Cancelled",
                                ignoreCase = true
                            )
                },

                driversCount = dispatcherUiState.drivers.count { driver ->
                    driver.isActive
                },

                inTransitCount = dispatcherUiState.jobs.count { job ->
                    job.status.equals(
                        "EnRoute",
                        ignoreCase = true
                    )
                },

                // Open jobs that can't progress yet: no driver, or no stops to deliver.
                issuesCount = dispatcherUiState.jobs.count { job ->
                    (job.status == JobStatus.PENDING || job.status == JobStatus.ASSIGNED) &&
                        (job.driverId == null || (dispatcherUiState.stopCounts[job.jobId] ?: 0) == 0)
                },

                // ------------------------------------------------
                // DASHBOARD NAVIGATION
                // ------------------------------------------------

                onJobsClick = {
                    nav.navigate(
                        Routes.DISPATCHER_JOBS
                    )
                },

                onFleetClick = {
                    nav.navigate(
                        Routes.DISPATCHER_FLEET
                    )
                },

                onMapClick = {
                    nav.navigate(
                        Routes.DISPATCHER_LIVE_MAP
                    )
                },

                onHistoryClick = {
                    nav.navigate(
                        Routes.DISPATCHER_HISTORY
                    )
                },

                onUsersClick = {
                    nav.navigate(
                        Routes.DISPATCHER_USERS
                    )
                },

                // ------------------------------------------------
                // DISPATCHER SETTINGS
                // ------------------------------------------------

                onSettingsClick = {
                    nav.navigate(
                        Routes.DISPATCHER_SETTINGS
                    )
                },

                // ------------------------------------------------
                // DISPATCHER LOGOUT
                // ------------------------------------------------

                onLogout = {

                    // Clear the saved authentication token,
                    // user ID and role.
                    sessionManager.clear()

                    // Return to login and remove the
                    // previous navigation history.
                    nav.navigate(
                        Routes.LOGIN
                    ) {

                        popUpTo(0) {
                            inclusive = true
                        }

                        launchSingleTop = true
                    }
                },

                // ------------------------------------------------
                // REFRESH
                // ------------------------------------------------

                onRefresh = {
                    dispatcherViewModel.loadDispatcherData()
                },

                onCreateJob = {
                    nav.navigate(
                        Routes.DISPATCHER_CREATE_JOB
                    )
                }
            )
        }


        // ========================================================
        // DISPATCHER - SETTINGS
        // ========================================================

        composable(Routes.DISPATCHER_SETTINGS) {

            val context = LocalContext.current

            val sessionManager = remember {
                SessionManager(context)
            }

            val userRepository = remember {
                UserRepository(
                    session = sessionManager
                )
            }

            val settingsViewModel: DispatcherSettingsViewModel =
                viewModel(
                    factory = DispatcherSettingsViewModelFactory(
                        userRepository = userRepository,
                        depotRepository = DepotRepository(
                            session = sessionManager
                        )
                    )
                )

            DispatcherSettingsScreen(
                viewModel = settingsViewModel,

                onBack = {
                    nav.popBackStack()
                },

                onManageUsers = {
                    nav.navigate(Routes.DISPATCHER_USERS)
                },

                onFleet = {
                    nav.navigate(Routes.DISPATCHER_FLEET)
                },

                onLiveMap = {
                    nav.navigate(Routes.DISPATCHER_LIVE_MAP)
                },

                onHistory = {
                    nav.navigate(Routes.DISPATCHER_HISTORY)
                },

                onLogout = {
                    sessionManager.clear()

                    nav.navigate(
                        Routes.LOGIN
                    ) {
                        popUpTo(0) {
                            inclusive = true
                        }

                        launchSingleTop = true
                    }
                }
            )
        }


        // ========================================================
        // DISPATCHER - JOB LIST
        // ========================================================

        composable(Routes.DISPATCHER_JOBS) { backStackEntry ->

            val context = LocalContext.current

            // Set by Create Job so the list opens Add Stop for the new job.
            val addStopsForJobId by backStackEntry.savedStateHandle
                .getStateFlow<Long?>(ADD_STOPS_FOR_JOB_KEY, null)
                .collectAsState()

            val sessionManager = remember {
                SessionManager(context)
            }

            val apiService = remember {
                RetrofitClient.getApiService(sessionManager)
            }

            val jobRepository = remember {
                JobRepository(
                    session = sessionManager,
                    api = apiService
                )
            }

            val dispatcherViewModel: DispatcherViewModel = viewModel(
                factory = DispatcherViewModelFactory(
                    jobRepository = jobRepository,
                    api = apiService
                )
            )

            JobListScreen(
                viewModel = dispatcherViewModel,

                onBack = {
                    nav.popBackStack()
                },

                onCreateJob = {
                    nav.navigate(
                        Routes.DISPATCHER_CREATE_JOB
                    )
                },

                onEditJob = { job ->

                    nav.navigate(
                        Routes.dispatcherEditJob(
                            job.jobId
                        )
                    )
                },

                addStopsForJobId = addStopsForJobId,

                onAddStopsPromptHandled = {
                    backStackEntry.savedStateHandle[ADD_STOPS_FOR_JOB_KEY] = null
                }
            )
        }


        // ========================================================
        // DISPATCHER - EDIT JOB
        // ========================================================

        composable(
            route = Routes.DISPATCHER_EDIT_JOB,
            arguments = listOf(
                navArgument("jobId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->

            val context = LocalContext.current

            val jobId =
                backStackEntry.arguments?.getLong("jobId")
                    ?: return@composable

            val sessionManager = remember {
                SessionManager(context)
            }

            val apiService = remember {
                RetrofitClient.getApiService(sessionManager)
            }

            val jobRepository = remember {
                JobRepository(
                    session = sessionManager,
                    api = apiService
                )
            }

            val dispatcherViewModel: DispatcherViewModel = viewModel(
                factory = DispatcherViewModelFactory(
                    jobRepository = jobRepository,
                    api = apiService
                )
            )

            CreateJobScreen(
                viewModel = dispatcherViewModel,

                jobId = jobId,

                onBack = {
                    nav.popBackStack()
                },

                onJobSaved = {
                    nav.popBackStack()
                }
            )
        }


        // ========================================================
        // DISPATCHER - CREATE JOB
        // ========================================================

        composable(Routes.DISPATCHER_CREATE_JOB) {

            val context = LocalContext.current

            val sessionManager = remember {
                SessionManager(context)
            }

            val apiService = remember {
                RetrofitClient.getApiService(sessionManager)
            }

            val jobRepository = remember {
                JobRepository(
                    session = sessionManager,
                    api = apiService
                )
            }

            val dispatcherViewModel: DispatcherViewModel = viewModel(
                factory = DispatcherViewModelFactory(
                    jobRepository = jobRepository,
                    api = apiService
                )
            )

            CreateJobScreen(
                viewModel = dispatcherViewModel,

                jobId = null,

                onBack = {
                    nav.popBackStack()
                },

                onJobSaved = { createdJobId ->

                    // Land on the Jobs list (reusing it if we came from there)
                    // and, if enabled in Settings, open Add Stop for the new job.
                    nav.navigate(Routes.DISPATCHER_JOBS) {
                        popUpTo(Routes.DISPATCHER_CREATE_JOB) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }

                    if (createdJobId != null &&
                        DispatcherPreferences.settings.value.promptForStopsAfterCreate
                    ) {
                        nav.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set(ADD_STOPS_FOR_JOB_KEY, createdJobId)
                    }
                }
            )
        }


        // ========================================================
        // DISPATCHER - FLEET MONITORING
        // ========================================================

        composable(Routes.DISPATCHER_FLEET) {

            val context = LocalContext.current

            val sessionManager = remember {
                SessionManager(context)
            }

            val apiService = remember {
                RetrofitClient.getApiService(sessionManager)
            }

            val jobRepository = remember {
                JobRepository(
                    session = sessionManager,
                    api = apiService
                )
            }

            val dispatcherViewModel: DispatcherViewModel = viewModel(
                factory = DispatcherViewModelFactory(
                    jobRepository = jobRepository,
                    api = apiService
                )
            )

            FleetManagementScreen(
                viewModel = dispatcherViewModel,

                onBack = {
                    nav.popBackStack()
                }
            )
        }


        // ========================================================
        // DISPATCHER - LIVE MAP
        // ========================================================

        composable(Routes.DISPATCHER_LIVE_MAP) {

            val context = LocalContext.current

            val sessionManager = remember {
                SessionManager(context)
            }

            val apiService = remember {
                RetrofitClient.getApiService(sessionManager)
            }

            val jobRepository = remember {
                JobRepository(
                    session = sessionManager,
                    api = apiService
                )
            }

            val dispatcherViewModel: DispatcherViewModel = viewModel(
                factory = DispatcherViewModelFactory(
                    jobRepository = jobRepository,
                    api = apiService
                )
            )

            LiveMapScreen(
                viewModel = dispatcherViewModel,

                onBack = {
                    nav.popBackStack()
                }
            )
        }


        // ========================================================
        // DISPATCHER - DELIVERY HISTORY
        // ========================================================

        composable(Routes.DISPATCHER_HISTORY) {

            val context = LocalContext.current

            val sessionManager = remember {
                SessionManager(context)
            }

            val apiService = remember {
                RetrofitClient.getApiService(sessionManager)
            }

            val jobRepository = remember {
                JobRepository(
                    session = sessionManager,
                    api = apiService
                )
            }

            val dispatcherViewModel: DispatcherViewModel = viewModel(
                factory = DispatcherViewModelFactory(
                    jobRepository = jobRepository,
                    api = apiService
                )
            )

            DeliveryHistoryScreen(
                viewModel = dispatcherViewModel,

                onBack = {
                    nav.popBackStack()
                }
            )
        }


        // ========================================================
        // DISPATCHER - USER MANAGEMENT
        // ========================================================

        composable(Routes.DISPATCHER_USERS) {

            val context = LocalContext.current

            val userManagementViewModel: UserManagementViewModel =
                viewModel(
                    factory = UserManagementViewModelFactory(
                        userRepository = UserRepository(
                            session = SessionManager(context)
                        ),
                        driverRepository = DriverRepository(
                            session = SessionManager(context)
                        ),
                        depotRepository = DepotRepository(
                            session = SessionManager(context)
                        )
                    )
                )

            UserManagementScreen(
                viewModel = userManagementViewModel,

                onBack = {
                    nav.popBackStack()
                }
            )
        }


        // ========================================================
        // DRIVER FLOW
        // ========================================================

        composable(Routes.DRIVER_DASHBOARD) {

            val authViewModel: AuthViewModel = viewModel()

            DriverDashboardScreen(

                onViewJobs = {
                    nav.navigate(
                        Routes.DRIVER_MY_JOBS
                    )
                },

                onSettings = {
                    nav.navigate(
                        Routes.DRIVER_SETTINGS
                    )
                },

                onLogout = {

                    authViewModel.logout()

                    nav.navigate(
                        Routes.LOGIN
                    ) {

                        popUpTo(0) {
                            inclusive = true
                        }
                    }
                }
            )
        }


        // ========================================================
        // DRIVER - SETTINGS
        // ========================================================

        composable(Routes.DRIVER_SETTINGS) {

            DriverSettingsScreen(
                onBack = {
                    nav.popBackStack()
                }
            )
        }


        composable(Routes.DRIVER_MY_JOBS) {

            val driverViewModel: DriverViewModel = viewModel()

            MyJobsScreen(

                viewModel = driverViewModel,

                onJobClick = { jobId ->

                    nav.navigate(
                        Routes.driverJobStops(jobId)
                    )
                },

                onBack = {
                    nav.popBackStack()
                }
            )
        }


        // ========================================================
        // DRIVER - STOP DETAILS
        // ========================================================

        composable(
            route = Routes.DRIVER_JOB_STOPS,
            arguments = listOf(
                navArgument("jobId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->

            val jobId =
                backStackEntry.arguments?.getLong("jobId")
                    ?: return@composable

            val driverViewModel: DriverViewModel = viewModel()

            StopDetailsScreen(

                jobId = jobId,

                viewModel = driverViewModel,

                onStopClick = { stopId ->

                    nav.navigate(
                        Routes.driverCapturePod(
                            stopId,
                            jobId
                        )
                    )
                },

                onBack = {
                    nav.popBackStack()
                }
            )
        }


        // ========================================================
        // DRIVER - POD CAPTURE
        // ========================================================

        composable(
            route = Routes.DRIVER_CAPTURE_POD,
            arguments = listOf(

                navArgument("stopId") {
                    type = NavType.LongType
                },

                navArgument("jobId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->

            val stopId =
                backStackEntry.arguments?.getLong("stopId")
                    ?: return@composable

            val jobId =
                backStackEntry.arguments?.getLong("jobId")
                    ?: return@composable

            val driverViewModel: DriverViewModel = viewModel()

            PodCaptureScreen(

                stopId = stopId,

                jobId = jobId,

                viewModel = driverViewModel,

                onSuccess = {
                    nav.popBackStack()
                },

                onBack = {
                    nav.popBackStack()
                }
            )
        }


        // ========================================================
        // CLIENT FLOW
        // ========================================================

        composable(Routes.CLIENT_DASHBOARD) {

            val context = LocalContext.current

            val clientViewModel: ClientViewModel =
                viewModel(
                    factory = ClientViewModelFactory(
                        jobRepository = JobRepository(
                            session = SessionManager(context)
                        )
                    )
                )

            val authViewModel: AuthViewModel = viewModel()

            ClientDashboardScreen(

                viewModel = clientViewModel,

                onTrackShipment = { jobId ->

                    nav.navigate(
                        Routes.clientTrackShipment(jobId)
                    )
                },

                onOpenSettings = {

                    nav.navigate(
                        Routes.CLIENT_SETTINGS
                    )
                },

                onLogout = {

                    authViewModel.logout()

                    nav.navigate(
                        Routes.LOGIN
                    ) {

                        popUpTo(0) {
                            inclusive = true
                        }

                        launchSingleTop = true
                    }
                }
            )
        }


        // ========================================================
        // CLIENT - TRACK SHIPMENT
        // ========================================================

        composable(
            route = Routes.CLIENT_TRACK_SHIPMENT,
            arguments = listOf(
                navArgument("jobId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->

            val context = LocalContext.current

            val jobId =
                backStackEntry.arguments?.getLong("jobId")
                    ?: return@composable

            val clientViewModel: ClientViewModel =
                viewModel(
                    factory = ClientViewModelFactory(
                        jobRepository = JobRepository(
                            session = SessionManager(context)
                        )
                    )
                )

            TrackShipmentScreen(

                jobId = jobId,

                viewModel = clientViewModel,

                onBack = {
                    nav.popBackStack()
                }
            )
        }


        // ========================================================
        // CLIENT - SETTINGS
        // ========================================================

        composable(Routes.CLIENT_SETTINGS) {

            SettingsScreen(
                onBack = {
                    nav.popBackStack()
                }
            )
        }
    }
}


/**
 * Navigate to the correct home screen based on
 * the authenticated user's role.
 */
private fun NavHostController.goToHome(
    role: String?
) {

    navigate(
        homeRouteForRole(role)
    ) {

        popUpTo(0) {
            inclusive = true
        }

        launchSingleTop = true
    }
}

// savedStateHandle key: Create Job -> Jobs list "add stops for this job".
private const val ADD_STOPS_FOR_JOB_KEY = "addStopsForJobId"
