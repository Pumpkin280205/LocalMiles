package com.example.localmiles.Core.Data.Repository

import com.example.localmiles.Core.Network.ApiService
import com.example.localmiles.Core.Network.Dto.UserAccountResponse
import com.example.localmiles.Core.Network.Dto.UserProfileUpdateRequest
import com.example.localmiles.Core.Network.Dto.UserRoleUpdateRequest
import com.example.localmiles.Core.Network.RetrofitClient
import com.example.localmiles.Core.Utils.Result
import com.example.localmiles.Core.Utils.SessionManager

class UserRepository(
    private val session: SessionManager,
    private val api: ApiService = RetrofitClient.getApiService(session),
) {

    // GET users — admin only.
    suspend fun getUsers(): Result<List<UserAccountResponse>> =
        apiCall {
            api.getUsers().users
        }

    // PATCH users/{id}/role — admin only.
    suspend fun updateUserRole(
        id: Long,
        role: String
    ): Result<UserAccountResponse> =
        apiCall {
            api.updateUserRole(
                id,
                body = UserRoleUpdateRequest(role = role)
            ).user
        }

    /**
     * Get the currently logged-in user's profile.
     *
     * The dispatcher is an admin account, so the existing
     * GET /users endpoint can be used to locate the current
     * account using the UserId stored in SessionManager.
     */
    suspend fun getCurrentUser(): Result<UserAccountResponse> {

        val currentUserId = session.getUserId()?.toLongOrNull()
            ?: return Result.Error("Unable to determine the current user.")

        return apiCall {
            api.getUsers()
                .users
                .firstOrNull { it.userId == currentUserId }
                ?: throw IllegalStateException("Current user was not found.")
        }
    }

    /**
     * Update the currently logged-in user's own profile.
     *
     * The backend allows FirstName and LastName to be changed.
     */
    suspend fun updateCurrentUser(
        firstName: String,
        lastName: String
    ): Result<UserAccountResponse> =
        apiCall {
            api.updateCurrentUser(
                UserProfileUpdateRequest(
                    firstName = firstName,
                    lastName = lastName
                )
            ).user
        }
}