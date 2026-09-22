package com.example.localmiles.Screens.Client

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localmiles.Core.Utils.ThemeMode
import com.example.localmiles.Core.Utils.ThemePreferences

@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val preferences = remember {
        context.getSharedPreferences(
            "localmiles_client_settings",
            Context.MODE_PRIVATE
        )
    }

    var selectedLanguage by remember {
        mutableStateOf(
            preferences.getString("language", "English") ?: "English"
        )
    }

    var notificationsEnabled by remember {
        mutableStateOf(
            preferences.getBoolean("notifications_enabled", true)
        )
    }

    var dataSaverEnabled by remember {
        mutableStateOf(
            preferences.getBoolean("data_saver_enabled", false)
        )
    }

    val themeMode by ThemePreferences.themeMode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // LocalMiles top bar
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onBack
                ) {
                    Text(
                        text = "‹",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 34.sp
                    )
                }

                Column {
                    Text(
                        text = "Settings",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Client preferences",
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

            Text(
                text = "Preferences",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Manage your LocalMiles experience.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // LANGUAGE
            SettingsSectionTitle(
                title = "Language",
                description = "Choose your preferred app language."
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
            ) {
                Column {

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

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
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

            // NOTIFICATIONS
            SettingsSectionTitle(
                title = "Notifications",
                description = "Choose which updates you want to receive."
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
            ) {
                SettingSwitchRow(
                    title = "Delivery notifications",
                    description = "Receive shipment status and delivery updates.",
                    checked = notificationsEnabled,
                    onCheckedChange = { enabled ->

                        notificationsEnabled = enabled

                        preferences.edit()
                            .putBoolean(
                                "notifications_enabled",
                                enabled
                            )
                            .apply()
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // APPEARANCE
            SettingsSectionTitle(
                title = "Appearance",
                description = "Choose how LocalMiles looks on this device."
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
            ) {
                Column {

                    ThemeOption(
                        label = "System default",
                        selectedMode = themeMode,
                        mode = ThemeMode.SYSTEM,
                        onSelected = {
                            ThemePreferences.setThemeMode(ThemeMode.SYSTEM)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )

                    ThemeOption(
                        label = "Light",
                        selectedMode = themeMode,
                        mode = ThemeMode.LIGHT,
                        onSelected = {
                            ThemePreferences.setThemeMode(ThemeMode.LIGHT)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )

                    ThemeOption(
                        label = "Dark",
                        selectedMode = themeMode,
                        mode = ThemeMode.DARK,
                        onSelected = {
                            ThemePreferences.setThemeMode(ThemeMode.DARK)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // DATA USAGE
            SettingsSectionTitle(
                title = "Data Usage",
                description = "Control how LocalMiles uses mobile data."
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
            ) {
                SettingSwitchRow(
                    title = "Data Saver",
                    description = "Reduce background data usage on limited connections.",
                    checked = dataSaverEnabled,
                    onCheckedChange = { enabled ->

                        dataSaverEnabled = enabled

                        preferences.edit()
                            .putBoolean(
                                "data_saver_enabled",
                                enabled
                            )
                            .apply()
                    }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Small app information section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = "LocalMiles",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Deliver locally. Drive further.",
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Secure • Reliable • Connected",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(
    title: String,
    description: String
) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold
    )

    Text(
        text = description,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp
    )
}

@Composable
private fun LanguageOption(
    language: String,
    selectedLanguage: String,
    onSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onSelected()
            }
            .padding(
                horizontal = 12.dp,
                vertical = 6.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        RadioButton(
            selected = selectedLanguage == language,
            onClick = onSelected,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )

        Text(
            text = language,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontWeight = if (selectedLanguage == language) {
                FontWeight.SemiBold
            } else {
                FontWeight.Normal
            }
        )
    }
}

@Composable
private fun ThemeOption(
    label: String,
    selectedMode: ThemeMode,
    mode: ThemeMode,
    onSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onSelected()
            }
            .padding(
                horizontal = 12.dp,
                vertical = 6.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        RadioButton(
            selected = selectedMode == mode,
            onClick = onSelected,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )

        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontWeight = if (selectedMode == mode) {
                FontWeight.SemiBold
            } else {
                FontWeight.Normal
            }
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
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {

            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                uncheckedTrackColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}
