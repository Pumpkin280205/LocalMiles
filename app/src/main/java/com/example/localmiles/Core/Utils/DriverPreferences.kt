package com.example.localmiles.Core.Utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Which app the stop screen's "Navigate" button opens. */
enum class NavigationApp(val label: String) {
    GOOGLE_MAPS("Google Maps"),
    WAZE("Waze"),
}

data class DriverSettings(
    // Stops the screen sleeping on the stop list / POD screens mid-delivery.
    val keepScreenOn: Boolean = true,
    val navigationApp: NavigationApp = NavigationApp.GOOGLE_MAPS,
)

/**
 * Driver-only preferences set on the Driver Settings screen and read by Stop
 * Details and POD Capture. Device-level like ThemePreferences.
 */
object DriverPreferences {

    private var prefs: SharedPreferences? = null

    private val _settings = MutableStateFlow(DriverSettings())
    val settings: StateFlow<DriverSettings> = _settings.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return

        val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = p

        _settings.value = DriverSettings(
            keepScreenOn = p.getBoolean(KEY_KEEP_SCREEN_ON, true),
            navigationApp = runCatching {
                NavigationApp.valueOf(p.getString(KEY_NAVIGATION_APP, null) ?: "")
            }.getOrDefault(NavigationApp.GOOGLE_MAPS),
        )
    }

    fun update(transform: (DriverSettings) -> DriverSettings) {
        val updated = transform(_settings.value)
        _settings.value = updated

        prefs?.edit()
            ?.putBoolean(KEY_KEEP_SCREEN_ON, updated.keepScreenOn)
            ?.putString(KEY_NAVIGATION_APP, updated.navigationApp.name)
            ?.apply()
    }

    private const val PREFS_NAME = "localmiles_driver_prefs"
    private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
    private const val KEY_NAVIGATION_APP = "navigation_app"
}
