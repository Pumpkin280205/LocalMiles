
package com.example.localmiles.Core.Utils

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AppLanguageManagerTest {

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
                "localmiles_client_settings",
                Context.MODE_PRIVATE
            )
        } returns preferences

        every { preferences.edit() } returns editor
    }

    @Test
    fun englishLanguageIsSaved() {
        AppLanguageManager.saveSelectedLanguage(context, "English")

        verify { editor.putString("language", "English") }
    }

    @Test
    fun afrikaansLanguageIsSaved() {
        AppLanguageManager.saveSelectedLanguage(context, "Afrikaans")

        verify { editor.putString("language", "Afrikaans") }
    }

    @Test
    fun isiZuluLanguageIsSaved() {
        AppLanguageManager.saveSelectedLanguage(context, "isiZulu")

        verify { editor.putString("language", "isiZulu") }
    }

    @Test
    fun selectedLanguageIsRetrieved() {
        every {
            preferences.getString("language", "English")
        } returns "Afrikaans"

        assertEquals(
            "Afrikaans",
            AppLanguageManager.getSelectedLanguage(context)
        )
    }

    @Test
    fun englishIsDefaultLanguage() {
        every {
            preferences.getString("language", "English")
        } returns "English"

        assertEquals(
            "English",
            AppLanguageManager.getSelectedLanguage(context)
        )
    }
}
