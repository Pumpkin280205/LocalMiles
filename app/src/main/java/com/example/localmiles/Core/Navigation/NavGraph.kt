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
        composable(Routes.CLIENT_DASHBOARD) { PlaceholderDashboard("Client", nav) }
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
