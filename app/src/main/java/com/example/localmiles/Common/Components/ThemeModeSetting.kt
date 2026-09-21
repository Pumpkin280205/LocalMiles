package com.example.localmiles.Common.Components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localmiles.Core.Utils.ThemeMode
import com.example.localmiles.Core.Utils.ThemePreferences

/**
 * Reusable dark-mode picker for the Dispatcher/Driver settings screens, which
 * already read colors from MaterialTheme. Drop into any settings screen that
 * uses M3 components as-is.
 */
@Composable
fun ThemeModeSetting(
    modifier: Modifier = Modifier
) {
    val themeMode by ThemePreferences.themeMode.collectAsState()

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Choose how LocalMiles looks on this device.",
                style = MaterialTheme.typography.bodySmall
            )

            ThemeModeOption(
                label = "System default",
                selected = themeMode == ThemeMode.SYSTEM,
                onSelected = { ThemePreferences.setThemeMode(ThemeMode.SYSTEM) }
            )

            ThemeModeOption(
                label = "Light",
                selected = themeMode == ThemeMode.LIGHT,
                onSelected = { ThemePreferences.setThemeMode(ThemeMode.LIGHT) }
            )

            ThemeModeOption(
                label = "Dark",
                selected = themeMode == ThemeMode.DARK,
                onSelected = { ThemePreferences.setThemeMode(ThemeMode.DARK) }
            )
        }
    }
}

@Composable
private fun ThemeModeOption(
    label: String,
    selected: Boolean,
    onSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        RadioButton(
            selected = selected,
            onClick = onSelected
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
