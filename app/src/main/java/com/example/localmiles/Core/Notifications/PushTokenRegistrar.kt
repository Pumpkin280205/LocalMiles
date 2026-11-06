package com.example.localmiles.Core.Notifications

import android.content.Context
import android.util.Log
import com.example.localmiles.Core.Data.Repository.DeviceRepository
import com.example.localmiles.Core.Utils.Result
import com.example.localmiles.Core.Utils.SessionManager
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Keeps the API's list of this phone's FCM tokens current, so pushes the API
 * sends reach the signed-in user. Best effort: a failure is logged and the
 * next app start / login tries again.
 */
object PushTokenRegistrar {

    private const val TAG = "PushTokenRegistrar"

    // Outlives the screen that triggered it (logout navigates away immediately).
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** After login and on every app start while signed in. */
    fun register(context: Context) {
        val session = SessionManager(context)
        if (!session.isLoggedIn()) return

        scope.launch {
            val token = runCatching { FirebaseMessaging.getInstance().token.await() }
                .onFailure { Log.w(TAG, "Could not get FCM token", it) }
                .getOrNull() ?: return@launch
            send(session, token)
        }
    }

    /** From FirebaseMessagingService.onNewToken. */
    fun register(context: Context, token: String) {
        val session = SessionManager(context)
        if (!session.isLoggedIn()) return
        scope.launch { send(session, token) }
    }

    /**
     * Call on logout BEFORE the session is cleared: the JWT is captured here and
     * sent explicitly, since the request runs after the session is gone.
     */
    fun unregister(context: Context) {
        val session = SessionManager(context)
        val jwt = session.getToken() ?: return

        scope.launch {
            val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
                ?: return@launch
            when (val result = DeviceRepository(session).unregisterToken(token, jwt)) {
                is Result.Success -> Log.d(TAG, "FCM token unregistered")
                is Result.Error -> Log.w(TAG, "FCM token unregister failed: ${result.message}")
            }
        }
    }

    private suspend fun send(session: SessionManager, token: String) {
        when (val result = DeviceRepository(session).registerToken(token)) {
            is Result.Success -> Log.d(TAG, "FCM token registered")
            is Result.Error -> Log.w(TAG, "FCM token register failed: ${result.message}")
        }
    }
}
