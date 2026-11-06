
package com.example.localmiles.Core.Utils

import android.content.Context
import android.content.SharedPreferences

object BiometricPreferences {

    private const val PREFS_NAME = "localmiles_biometric_settings"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
    }

    /**
     * Checks whether biometric login has been enabled by the user.
     * Disabled by default.
     */
    fun isEnabled(context: Context): Boolean {
        return getPreferences(context).getBoolean(
            KEY_BIOMETRIC_ENABLED,
            false
        )
    }

    /**
     * Saves the user's biometric login preference.
     */
    fun setEnabled(context: Context, enabled: Boolean) {
        getPreferences(context).edit()
            .putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
            .apply()
    }

    /**
     * Disables biometric login.
     */
    fun disable(context: Context) {
        setEnabled(context, false)
    }
}
