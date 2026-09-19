package com.example.localmiles.Core.Network.Dto

import com.google.gson.annotations.SerializedName

data class UserAccountResponse(
    @SerializedName("UserId") val userId: Long,
    @SerializedName("Email") val email: String,
    @SerializedName("FirstName") val firstName: String,
    @SerializedName("LastName") val lastName: String,
    //"admin" | "driver" | "client"
    @SerializedName("Role") val role: String,
    @SerializedName("IsActive") val isActive: Boolean,
    @SerializedName("CreatedAt") val createdAt: String,
    @SerializedName("LastLoginAt") val lastLoginAt: String?,
)

data class UserListResponse(
    val users: List<UserAccountResponse>,
)

data class UserDetailResponse(
    val user: UserAccountResponse,
)

data class UserRoleUpdateRequest(
    @SerializedName("Role") val role: String,
)
