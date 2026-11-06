
package com.example.localmiles.Core.Utils

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BiometricPreferencesTest {

    private lateinit var context: Context
    private lateinit var preferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @Before
    fun setUp() {
        context = mockk()
        preferences = mockk()
        editor = mockk(relaxed = true)

        every { context.applicationContext } returns context

        every {
            context.getSharedPreferences(
                "localmiles_biometric_settings",
                Context.MODE_PRIVATE
            )
        } returns preferences

        every {
            preferences.edit()
        } returns editor

        every {
            preferences.getBoolean("biometric_enabled", false)
        } returns false
    }

    @Test
    fun biometricIsDisabledByDefault() {
        assertFalse(BiometricPreferences.isEnabled(context))
    }

    @Test
    fun enablingBiometricSavesTrue() {
        BiometricPreferences.setEnabled(context, true)

        verify {
            editor.putBoolean("biometric_enabled", true)
        }
    }

    @Test
    fun disablingBiometricSavesFalse() {
        BiometricPreferences.setEnabled(context, false)

        verify {
            editor.putBoolean("biometric_enabled", false)
        }
    }

    @Test
    fun disableMethodTurnsBiometricOff() {
        BiometricPreferences.disable(context)

        verify {
            editor.putBoolean("biometric_enabled", false)
        }
    }

    @Test
    fun biometricPreferenceCanBeEnabled() {
        every {
            preferences.getBoolean("biometric_enabled", false)
        } returns true

        assertTrue(BiometricPreferences.isEnabled(context))
    }
}
