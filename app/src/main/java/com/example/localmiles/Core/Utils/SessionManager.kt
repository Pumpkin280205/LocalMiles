package com.example.localmiles.Core.Utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val appContext = context.applicationContext

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    //Persist the bearer token returned by `auth/login`.
    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    //The current token, or null if the user has never logged in / has logged out.
    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    //Cache the identity so the UI can branch on role without a network call.
    fun saveUser(userId: String, role: String) {
        prefs.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_ROLE, role)
            .apply()
    }

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    //"dispatcher" | "driver" | "client" — used for the post-login navigation split.
    fun getRole(): String? = prefs.getString(KEY_ROLE, null)

    fun isLoggedIn(): Boolean = !getToken().isNullOrBlank()

    // Wipe everything on logout, unless biometric login is on: then the session stays on the
    // phone so the login screen can offer "Unlock with biometrics".
    fun clear() {
        if (BiometricPreferences.isEnabled(appContext)) return
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "localmiles_session"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_ROLE = "user_role"
    }
}
