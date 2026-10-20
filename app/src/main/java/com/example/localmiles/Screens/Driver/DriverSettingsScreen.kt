package com.example.localmiles.Screens.Driver

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.localmiles.Common.Components.RoleSettingsScreen
import com.example.localmiles.Common.Components.SettingSwitchRow
import com.example.localmiles.Common.Components.SettingsDivider
import com.example.localmiles.Common.Components.SettingsInfoRow
import com.example.localmiles.Common.Components.SettingsRadioRow
import com.example.localmiles.Common.Components.SettingsSection
import com.example.localmiles.Common.Components.SettingsSpacer
import com.example.localmiles.Core.Utils.DriverPreferences
import com.example.localmiles.Core.Utils.NavigationApp

/**
 * Driver settings: the shared settings sections every role has, plus the
 * driver's own profile and delivery preferences (screen awake, navigation app).
 */
@Composable
fun DriverSettingsScreen(
    onBack: () -> Unit,
    viewModel: DriverSettingsViewModel = viewModel()
) {
    val settings by DriverPreferences.settings.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    RoleSettingsScreen(
        subtitle = "Driver preferences",
        onBack = onBack,
        notificationsDescription = "Receive new job assignments and route changes.",
        profileContent = {
            SettingsSection(
                title = "Driver Profile",
                description = "Ask your dispatcher to change these details."
            ) {
                val driver = viewModel.driver
                when {
                    viewModel.isLoading && driver == null ->
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))

                    driver == null ->
                        Text(
                            text = viewModel.error ?: "No driver profile found.",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )

                    else -> {
                        SettingsInfoRow("Employee number", driver.employeeNumber)
                        SettingsDivider()
                        SettingsInfoRow("Depot", viewModel.depotName ?: "Depot #${driver.depotId}")
                        SettingsDivider()
                        SettingsInfoRow("Phone", driver.phoneNumber)
                        SettingsDivider()
                        SettingsInfoRow("Licence", driver.licenceNumber)
                        SettingsDivider()
                        SettingsInfoRow("Status", if (driver.isAvailable) "Available" else "Unavailable")
                    }
                }
            }
        },
        roleContent = {
            SettingsSection(
                title = "Deliveries",
                description = "How the stop and proof-of-delivery screens behave."
            ) {
                SettingSwitchRow(
                    title = "Keep screen awake",
                    description = "Stops the screen sleeping on the stop list and POD screens.",
                    checked = settings.keepScreenOn,
                    onCheckedChange = { checked -> DriverPreferences.update { it.copy(keepScreenOn = checked) } }
                )
            }

            SettingsSpacer()

            SettingsSection(
                title = "Navigation App",
                description = "Opened by the Navigate button on your next stop."
            ) {
                NavigationApp.entries.forEachIndexed { index, app ->
                    if (index > 0) SettingsDivider()
                    SettingsRadioRow(
                        label = app.label,
                        selected = settings.navigationApp == app,
                        onSelected = { DriverPreferences.update { it.copy(navigationApp = app) } }
                    )
                }
            }
        }
    )
}
