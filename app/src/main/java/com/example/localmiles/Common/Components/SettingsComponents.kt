
package com.example.localmiles.Common.Components

import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.localmiles.Core.Auth.BiometricAuthManager
import com.example.localmiles.Core.Utils.AppLanguageManager
import com.example.localmiles.Core.Utils.BiometricPreferences
import com.example.localmiles.Core.Utils.ThemeMode
import com.example.localmiles.Core.Utils.ThemePreferences
import com.example.localmiles.R

private const val SHARED_SETTINGS_PREFS = "localmiles_client_settings"

@Composable
fun RoleSettingsScreen(
    subtitle: String,
    onBack: () -> Unit,
    notificationsDescription: String = "Receive shipment status and delivery updates.",
    profileContent: (@Composable () -> Unit)? = null,
    roleContent: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current

    val preferences = remember {
        context.getSharedPreferences(
            SHARED_SETTINGS_PREFS,
            Context.MODE_PRIVATE
        )
    }

    var selectedLanguage by remember {
        mutableStateOf(
            AppLanguageManager.getSelectedLanguage(context)
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

    var biometricEnabled by remember {
        mutableStateOf(
            BiometricPreferences.isEnabled(context)
        )
    }

    val themeMode by ThemePreferences.themeMode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        AppTopBar(
            title = stringResource(R.string.settings),
            subtitle = subtitle,
            showBack = true,
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

            Text(
                text = stringResource(R.string.preferences),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stringResource(R.string.manage_experience),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )

            profileContent?.let {
                SettingsSpacer()
                it()
            }

            // LANGUAGE
            SettingsSpacer()

            SettingsSection(
                title = stringResource(R.string.language),
                description = stringResource(R.string.choose_language)
            ) {
                listOf(
                    "English" to R.string.english,
                    "Afrikaans" to R.string.afrikaans,
                    "isiZulu" to R.string.isizulu
                ).forEachIndexed { index, (language, labelResource) ->

                    if (index > 0) {
                        SettingsDivider()
                    }

                    SettingsRadioRow(
                        label = stringResource(labelResource),
                        selected = selectedLanguage == language,
                        onSelected = {
                            selectedLanguage = language

                            AppLanguageManager.setLanguage(
                                context,
                                language
                            )
                        }
                    )
                }
            }

            // SECURITY AND BIOMETRICS
            SettingsSpacer()

            SettingsSection(
                title = stringResource(R.string.security),
                description = stringResource(R.string.security_description)
            ) {
                SettingSwitchRow(
                    title = stringResource(R.string.biometric_login),
                    description = stringResource(R.string.biometric_description),
                    checked = biometricEnabled,
                    onCheckedChange = { enabled ->

                        if (!enabled) {
                            BiometricPreferences.disable(context)
                            biometricEnabled = false

                        } else {
                            val activity = context as? FragmentActivity

                            if (activity == null) {
                                Toast.makeText(
                                    context,
                                    context.getString(
                                        R.string.biometric_unavailable
                                    ),
                                    Toast.LENGTH_LONG
                                ).show()

                            } else {
                                val biometricManager =
                                    BiometricAuthManager(activity)

                                if (!biometricManager.canAuthenticate()) {
                                    Toast.makeText(
                                        context,
                                        context.getString(
                                            R.string.enrol_biometric
                                        ),
                                        Toast.LENGTH_LONG
                                    ).show()

                                } else {
                                    biometricManager.authenticate {
                                            success,
                                            errorMessage ->

                                        if (success) {
                                            BiometricPreferences.setEnabled(
                                                context,
                                                true
                                            )

                                            biometricEnabled = true

                                            Toast.makeText(
                                                context,
                                                context.getString(
                                                    R.string.biometric_enabled
                                                ),
                                                Toast.LENGTH_SHORT
                                            ).show()

                                        } else if (errorMessage != null) {
                                            Toast.makeText(
                                                context,
                                                errorMessage,
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }

            // NOTIFICATIONS
            SettingsSpacer()

            SettingsSection(
                title = stringResource(R.string.notifications),
                description = stringResource(R.string.notifications_description)
            ) {
                SettingSwitchRow(
                    title = stringResource(R.string.delivery_notifications),
                    description = notificationsDescription,
                    checked = notificationsEnabled,
                    onCheckedChange = { enabled ->
                        notificationsEnabled = enabled

                        preferences.edit()
                            .putBoolean("notifications_enabled", enabled)
                            .apply()
                    }
                )
            }

            // APPEARANCE
            SettingsSpacer()

            SettingsSection(
                title = stringResource(R.string.appearance),
                description = stringResource(R.string.appearance_description)
            ) {
                listOf(
                    R.string.system_default to ThemeMode.SYSTEM,
                    R.string.light to ThemeMode.LIGHT,
                    R.string.dark to ThemeMode.DARK
                ).forEachIndexed { index, (labelResource, mode) ->

                    if (index > 0) {
                        SettingsDivider()
                    }

                    SettingsRadioRow(
                        label = stringResource(labelResource),
                        selected = themeMode == mode,
                        onSelected = {
                            ThemePreferences.setThemeMode(mode)
                        }
                    )
                }
            }

            // DATA USAGE
            SettingsSpacer()

            SettingsSection(
                title = stringResource(R.string.data_usage),
                description = stringResource(R.string.data_usage_description)
            ) {
                SettingSwitchRow(
                    title = stringResource(R.string.data_saver),
                    description = stringResource(R.string.data_saver_description),
                    checked = dataSaverEnabled,
                    onCheckedChange = { enabled ->
                        dataSaverEnabled = enabled

                        preferences.edit()
                            .putBoolean("data_saver_enabled", enabled)
                            .apply()
                    }
                )
            }

            // ROLE-SPECIFIC SETTINGS
            roleContent?.let {
                SettingsSpacer()
                it()
            }

            Spacer(modifier = Modifier.height(28.dp))

            AppInfoCard()
        }
    }
}

// SETTINGS SECTION
@Composable
fun SettingsSection(
    title: String,
    description: String? = null,
    content: @Composable () -> Unit
) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold
    )

    if (description != null) {
        Text(
            text = description,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }

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
            content()
        }
    }
}

// SETTINGS CARD BODY
@Composable
fun SettingsCardBody(
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        content()
    }
}

// SETTINGS DIVIDER
@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    )
}

// SETTINGS RADIO ROW
@Composable
fun SettingsRadioRow(
    label: String,
    selected: Boolean,
    onSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelected,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )

        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontWeight = if (selected) {
                FontWeight.SemiBold
            } else {
                FontWeight.Normal
            }
        )
    }
}

// SETTINGS SWITCH ROW
@Composable
fun SettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                onCheckedChange(!checked)
            }
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
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                uncheckedTrackColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

// SETTINGS LINK ROW
@Composable
fun SettingsLinkRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null
        )
    }
}

// SETTINGS INFO ROW
@Composable
fun SettingsInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )

        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// SETTINGS SPACER
@Composable
fun SettingsSpacer() {
    Spacer(modifier = Modifier.height(24.dp))
}

// APP INFORMATION CARD
@Composable
private fun AppInfoCard() {
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
                text = stringResource(R.string.local_miles_tagline),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                    alpha = 0.8f
                ),
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.local_miles_features),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
