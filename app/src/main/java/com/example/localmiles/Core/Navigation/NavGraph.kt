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
import com.example.localmiles.Core.Utils.SessionManager
import com.example.localmiles.Screens.Client.ClientDashboardScreen
import com.example.localmiles.Screens.Client.ClientViewModel
import com.example.localmiles.Screens.Client.SettingsScreen
import com.example.localmiles.Screens.Client.TrackShipmentScreen

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
        composable(Routes.DRIVER_DASHBOARD) { PlaceholderDashboard("Driver", nav) }

        // Client dashboard and client-specific navigation
        composable(Routes.CLIENT_DASHBOARD) {

            val context = LocalContext.current


            val clientViewModel = ClientViewModel(
                jobRepository = JobRepository(
                    session = SessionManager(context)
                )
            )

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

                    // Displays tracking information for the delivery selected from the client dashboard.
                    composable( route = Routes.CLIENT_TRACK_SHIPMENT,
                        arguments = listOf(
                            navArgument("jobId") {
                                type = NavType.LongType
                            }
                        )
                    ) { backStackEntry ->

                        val context = LocalContext.current
                        val jobId = backStackEntry.arguments?.getLong("jobId") ?: return@composable

                        val clientViewModel = ClientViewModel(
                            jobRepository = JobRepository(
                                session = SessionManager(context)
                            )
                        )

                        TrackShipmentScreen(
                            jobId = jobId,
                            viewModel = clientViewModel,
                            onBack = {
                                nav.popBackStack()

                                // Client preferences screen.
                                composable(Routes.CLIENT_SETTINGS) {
                                    SettingsScreen(
                                        onBack = {
                                            nav.popBackStack()
                                        }
                                    )
                                }
                            }
                        )
                    }
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
