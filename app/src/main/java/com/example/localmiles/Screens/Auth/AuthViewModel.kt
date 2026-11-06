package com.example.localmiles.Screens.Auth

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.localmiles.Core.Auth.GoogleAuthClient
import com.example.localmiles.Core.Auth.SignInCancelled
import com.example.localmiles.Core.Data.Repository.AuthRepository
import com.example.localmiles.Core.Location.DriverLocationService
import com.example.localmiles.Core.Notifications.PushTokenRegistrar
import com.example.localmiles.Core.Utils.Result
import com.example.localmiles.Core.Utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Success(val role: String) : AuthUiState   // "admin" | "driver" | "client"
    data class Error(val message: String) : AuthUiState
}

/**
 * Owns the sign-in flow. AndroidViewModel so it can build its own dependencies
 * without a DI framework; swap to constructor injection when Hilt is wired up.
 */
class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val session = SessionManager(app)
    private val authRepo = AuthRepository(session)
    private val googleAuth = GoogleAuthClient()

    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    /** Cached role for the splash screen; null if not logged in. */
    fun currentRoleOrNull(): String? = session.getRole().takeIf { session.isLoggedIn() }

    /** [activityContext] must be an Activity — Credential Manager shows UI. */
    fun signIn(activityContext: Context) {
        if (_state.value == AuthUiState.Loading) return
        _state.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val firebaseIdToken = googleAuth.signInAndGetFirebaseIdToken(activityContext)
                when (val r = authRepo.login(firebaseIdToken)) {
                    is Result.Success -> {
                        // Link this phone to the account so the API's pushes reach it.
                        PushTokenRegistrar.register(getApplication())
                        _state.value = AuthUiState.Success(r.data.role)
                    }
                    is Result.Error -> _state.value = AuthUiState.Error(r.message)
                }
            } catch (e: SignInCancelled) {
                _state.value = AuthUiState.Idle
            } catch (e: Exception) {
                _state.value = AuthUiState.Error(e.message ?: "Sign-in failed")
            }
        }
    }

    fun consumeError() {
        if (_state.value is AuthUiState.Error) _state.value = AuthUiState.Idle
    }

    fun logout() {
        // Both need the session, so they run before it's cleared.
        PushTokenRegistrar.unregister(getApplication())
        DriverLocationService.stop(getApplication())
        googleAuth.signOut()
        authRepo.logout()
        _state.value = AuthUiState.Idle
    }
}
