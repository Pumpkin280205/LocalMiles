package com.example.localmiles.Screens.Client

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Settings available to LocalMiles clients.
// These options allow the user to customise basic app behaviour.
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    // Settings are kept as local UI state for the current implementation.
    // Persistent preferences can be connected later when required.
    var selectedLanguage by remember { mutableStateOf("English") }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var dataSaverEnabled by remember { mutableStateOf(false) }

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

        // ---------------------------------------------------------
        // Language setting
        // ---------------------------------------------------------

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
                    }
                )

                LanguageOption(
                    language = "isiZulu",
                    selectedLanguage = selectedLanguage,
                    onSelected = {
                        selectedLanguage = "isiZulu"
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ---------------------------------------------------------
        // Notification setting
        // ---------------------------------------------------------

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
                onCheckedChange = {
                    notificationsEnabled = it
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ---------------------------------------------------------
        // Data saver setting
        // ---------------------------------------------------------

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
                onCheckedChange = {
                    dataSaverEnabled = it
                }
            )
        }
    }
}

// Displays one selectable language option.
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

// Reusable row used for settings that can be switched on or off.
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