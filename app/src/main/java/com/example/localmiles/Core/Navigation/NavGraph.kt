package com.example.localmiles.Core.Navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.localmiles.Screens.Auth.AuthViewModel
import com.example.localmiles.Screens.Auth.LoginScreen
import com.example.localmiles.Screens.Auth.SplashScreen
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.localmiles.Core.Data.Repository.JobRepository
import com.example.localmiles.Core.Data.Repository.StopRepository
import com.example.localmiles.Core.Utils.SessionManager
import com.example.localmiles.Screens.Client.ClientDashboardScreen
import com.example.localmiles.Screens.Client.ClientViewModel
import com.example.localmiles.Screens.Client.SettingsScreen
import com.example.localmiles.Screens.Client.ClientViewModelFactory
import com.example.localmiles.Screens.Client.TrackShipmentScreen
import com.example.localmiles.Screens.Driver.DriverDashboardScreen
import com.example.localmiles.Screens.Driver.DriverViewModel
import com.example.localmiles.Screens.Driver.MyJobsScreen
import com.example.localmiles.Screens.Driver.PodCaptureScreen
import com.example.localmiles.Screens.Driver.StopDetailsScreen

@Composable
fun NavGraph(nav: NavHostController = rememberNavController()) {

    NavHost(navController = nav, startDestination = Routes.SPLASH) {

        composable(Routes.SPLASH) {
            SplashScreen(
                onResolved = { role -> nav.goToHome(role) },
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onSignedIn = { role -> nav.goToHome(role) },
            )
        }

        // Placeholder dashboards — replace with the real screens.
        composable(Routes.DISPATCHER_DASHBOARD) { PlaceholderDashboard("Dispatcher (admin)", nav) }

        // --- Driver Flow ---
        composable(Routes.DRIVER_DASHBOARD) {
            val authViewModel: AuthViewModel = viewModel()
            DriverDashboardScreen(
                onViewJobs = { nav.navigate(Routes.DRIVER_MY_JOBS) },
                onLogout = {
                    authViewModel.logout()
                    nav.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.DRIVER_MY_JOBS) {
            val driverViewModel: DriverViewModel = viewModel()

            MyJobsScreen(
                viewModel = driverViewModel,
                onJobClick = { jobId -> nav.navigate(Routes.driverJobStops(jobId)) },
                onBack = { nav.popBackStack() }
            )
        }

        composable(
            route = Routes.DRIVER_JOB_STOPS,
            arguments = listOf(navArgument("jobId") { type = NavType.LongType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getLong("jobId") ?: return@composable
            val driverViewModel: DriverViewModel = viewModel()

            StopDetailsScreen(
                jobId = jobId,
                viewModel = driverViewModel,
                onStopClick = { stopId -> nav.navigate(Routes.driverCapturePod(stopId, jobId)) },
                onBack = { nav.popBackStack() }
            )
        }

        composable(
            route = Routes.DRIVER_CAPTURE_POD,
            arguments = listOf(
                navArgument("stopId") { type = NavType.LongType },
                navArgument("jobId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val stopId = backStackEntry.arguments?.getLong("stopId") ?: return@composable
            val jobId = backStackEntry.arguments?.getLong("jobId") ?: return@composable
            val driverViewModel: DriverViewModel = viewModel()

            PodCaptureScreen(
                stopId = stopId,
                jobId = jobId,
                viewModel = driverViewModel,
                onSuccess = { nav.popBackStack() },
                onBack = { nav.popBackStack() }
            )
        }

        // --- Client Flow ---
        composable(Routes.CLIENT_DASHBOARD) {

            val context = LocalContext.current

            // ViewModel used to load deliveries belonging to the logged-in client
            val clientViewModel: ClientViewModel = viewModel(
                factory = ClientViewModelFactory(
                    jobRepository = JobRepository(
                        session = SessionManager(context)
                    )
                )
            )

            // Existing authentication ViewModel handles logout and clears the session
            val authViewModel: AuthViewModel = viewModel()

            ClientDashboardScreen(
                viewModel = clientViewModel,

                // Open tracking for the delivery selected by the client
                onTrackShipment = { jobId ->
                    nav.navigate(
                        Routes.clientTrackShipment(jobId)
                    )
                },

                // Open the client's settings
                onOpenSettings = {
                    nav.navigate(Routes.CLIENT_SETTINGS)
                },

                // Clear the authenticated session and return to the login screen
                onLogout = {
                    authViewModel.logout()

                    nav.navigate(Routes.LOGIN) {
                        popUpTo(0) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        // Displays tracking information for the delivery selected from the client dashboard.
        composable(
            route = Routes.CLIENT_TRACK_SHIPMENT,
            arguments = listOf(
                navArgument("jobId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val context = LocalContext.current
            val jobId = backStackEntry.arguments?.getLong("jobId") ?: return@composable

            val clientViewModel: ClientViewModel = viewModel(
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

        // Client preferences screen.
        composable(Routes.CLIENT_SETTINGS) {
            SettingsScreen(
                onBack = {
                    nav.popBackStack()
                }
            )
        }
    }
}


/** Navigate to the role's home, clearing the auth screens from the back stack. */
private fun NavHostController.goToHome(role: String?) {
    navigate(homeRouteForRole(role)) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
private fun PlaceholderDashboard(
    label: String,
    nav: NavHostController,
    authViewModel: AuthViewModel = viewModel(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("$label dashboard", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = {
            authViewModel.logout()
            nav.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
        }) {
            Text("Log out")
        }
    }
}
