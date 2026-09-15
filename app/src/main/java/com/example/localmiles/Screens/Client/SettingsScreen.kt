package com.example.localmiles.Screens.Client

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Settings available to LocalMiles clients.

@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Store client settings locally on the device.
    val preferences = remember {
        context.getSharedPreferences(
            "localmiles_client_settings",
            Context.MODE_PRIVATE
        )
    }

    // Load the previously selected language
    var selectedLanguage by remember {
        mutableStateOf(
            preferences.getString("language", "English") ?: "English"
        )
    }

    // Load the previously selected notification preference.
    var notificationsEnabled by remember {
        mutableStateOf(
            preferences.getBoolean("notifications_enabled", true)
        )
    }

    // Load the previously selected data saver preference.
    var dataSaverEnabled by remember {
        mutableStateOf(
            preferences.getBoolean("data_saver_enabled", false)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Return to the client dashboard.
        TextButton(
            onClick = onBack
        ) {
            Text("Back")
        }

        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Manage your LocalMiles preferences",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Language setting
        Text(
            text = "Language",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Choose your preferred app language.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {

                LanguageOption(
                    language = "English",
                    selectedLanguage = selectedLanguage,
                    onSelected = {
                        selectedLanguage = "English"

                        preferences.edit()
                            .putString("language", "English")
                            .apply()
                    }
                )

                LanguageOption(
                    language = "isiZulu",
                    selectedLanguage = selectedLanguage,
                    onSelected = {
                        selectedLanguage = "isiZulu"

                        preferences.edit()
                            .putString("language", "isiZulu")
                            .apply()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))


        // Notification setting
        Text(
            text = "Notifications",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            SettingSwitchRow(
                title = "Delivery notifications",
                description = "Receive shipment status and delivery updates.",
                checked = notificationsEnabled,
                onCheckedChange = { enabled ->

                    notificationsEnabled = enabled

                    preferences.edit()
                        .putBoolean("notifications_enabled", enabled)
                        .apply()
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))


        // Data saver setting
        Text(
            text = "Data Usage",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            SettingSwitchRow(
                title = "Data Saver",
                description = "Reduce background data usage on limited connections.",
                checked = dataSaverEnabled,
                onCheckedChange = { enabled ->

                    dataSaverEnabled = enabled

                    preferences.edit()
                        .putBoolean("data_saver_enabled", enabled)
                        .apply()
                }
            )
        }
    }
}

// Displays one selectable language optionn
@Composable
private fun LanguageOption(
    language: String,
    selectedLanguage: String,
    onSelected: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        RadioButton(
            selected = selectedLanguage == language,
            onClick = onSelected
        )

        Text(
            text = language,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}


@Composable
private fun SettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}