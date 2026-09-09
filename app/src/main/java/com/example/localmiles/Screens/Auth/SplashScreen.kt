package com.example.localmiles.Screens.Auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Decides the first destination from the persisted session — no network call.
 * [onResolved] receives the cached role, or null when not logged in
 * (homeRouteForRole maps null -> LOGIN).
 */
@Composable
fun SplashScreen(
    onResolved: (role: String?) -> Unit,
    viewModel: AuthViewModel = viewModel(),
) {
    LaunchedEffect(Unit) {
        onResolved(viewModel.currentRoleOrNull())
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
