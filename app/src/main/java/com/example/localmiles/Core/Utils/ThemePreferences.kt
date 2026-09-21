package com.example.localmiles.Core.Utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

// Device-level display preference — deliberately separate from SessionManager
// so it survives logout instead of being wiped with the rest of the session.
object ThemePreferences {

    private lateinit var prefs: SharedPreferences

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun init(context: Context) {
        if (::prefs.isInitialized) return

        prefs = context.applicationContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        val stored = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)

        _themeMode.value = runCatching {
            ThemeMode.valueOf(stored ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    private const val PREFS_NAME = "localmiles_theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
}
