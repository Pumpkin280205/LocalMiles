package com.example.localmiles.Screens.Client

import androidx.compose.runtime.Composable
import com.example.localmiles.Common.Components.RoleSettingsScreen

// Client settings are the shared core sections with nothing role-specific
// added — the Driver and Dispatcher settings build on the same screen.
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    RoleSettingsScreen(
        subtitle = "Client preferences",
        onBack = onBack
    )
}
