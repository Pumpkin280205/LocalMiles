package com.example.localmiles.Core.Data.Repository

import com.example.localmiles.Core.Network.ApiService
import com.example.localmiles.Core.Network.LoginRequest
import com.example.localmiles.Core.Network.RetrofitClient
import com.example.localmiles.Core.Network.UserResponse
import com.example.localmiles.Core.Utils.Result
import com.example.localmiles.Core.Utils.SessionManager

/**
 * Owns the login/logout flow and the persisted session.
 *
 * Construct with the shared SessionManager; until Hilt is wired up, the simplest
 * call site is:
 *     val session = SessionManager(context)
 *     val authRepo = AuthRepository(session)
 */
class AuthRepository(
    private val session: SessionManager,
    private val api: ApiService = RetrofitClient.getApiService(session),
) {

    /**
     * Exchanges a Google ID token (from Credential Manager) for a session JWT,
     * persists the token + identity, and returns the user profile.
     */
    suspend fun login(googleIdToken: String): Result<UserResponse> =
        apiCall { api.login(LoginRequest(idToken = googleIdToken)) }.let { result ->
            when (result) {
                is Result.Success -> {
                    val resp = result.data
                    session.saveToken(resp.token)
                    session.saveUser(resp.user.id, resp.user.role)
                    Result.Success(resp.user)
                }
                is Result.Error -> result
            }
        }

    fun isLoggedIn(): Boolean = session.isLoggedIn()

    fun currentRole(): String? = session.getRole()

    fun logout() = session.clear()
}
