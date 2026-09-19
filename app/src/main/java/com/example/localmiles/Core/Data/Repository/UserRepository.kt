package com.example.localmiles.Core.Data.Repository

import com.example.localmiles.Core.Network.ApiService
import com.example.localmiles.Core.Network.Dto.UserAccountResponse
import com.example.localmiles.Core.Network.Dto.UserRoleUpdateRequest
import com.example.localmiles.Core.Network.RetrofitClient
import com.example.localmiles.Core.Utils.Result
import com.example.localmiles.Core.Utils.SessionManager

class UserRepository(
    session: SessionManager,
    private val api: ApiService = RetrofitClient.getApiService(session),
) {

    //GET users — admin only.
    suspend fun getUsers(): Result<List<UserAccountResponse>> =
        apiCall { api.getUsers().users }

    //PATCH users/{id}/role — admin only.
    suspend fun updateUserRole(id: Long, role: String): Result<UserAccountResponse> =
        apiCall { api.updateUserRole(id, UserRoleUpdateRequest(role = role)).user }
}
