
package com.example.localmiles.Core.Utils

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList

object AppLanguageManager {

    private const val PREFS_NAME = "localmiles_client_settings"
    private const val LANGUAGE_KEY = "language"

    private val supportedLanguages = mapOf(
        "English" to "en",
        "Afrikaans" to "af",
        "isiZulu" to "zu"
    )

    fun setLanguage(context: Context, language: String) {
        val languageTag = supportedLanguages[language] ?: "en"

        saveSelectedLanguage(context, language)

        applyApplicationLocale(context, languageTag)
    }

    internal fun saveSelectedLanguage(context: Context, language: String) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(LANGUAGE_KEY, language)
            .apply()
    }

    internal fun applyApplicationLocale(context: Context, languageTag: String) {
        val localeManager =
            context.getSystemService(LocaleManager::class.java)

        localeManager.applicationLocales =
            LocaleList.forLanguageTags(languageTag)
    }

    fun getSelectedLanguage(context: Context): String {
        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(LANGUAGE_KEY, "English") ?: "English"
    }
}
