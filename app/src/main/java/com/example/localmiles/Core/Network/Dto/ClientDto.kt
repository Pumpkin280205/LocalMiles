package com.example.localmiles.Core.Network.Dto

import com.google.gson.annotations.SerializedName

data class ClientResponse(
    @SerializedName("ClientId") val clientId: Long,
    @SerializedName("UserId") val userId: Long,
    @SerializedName("CompanyName") val companyName: String,
    @SerializedName("ContactPerson") val contactPerson: String,
    @SerializedName("Email") val email: String,
    @SerializedName("PhoneNumber") val phoneNumber: String? = null,
    @SerializedName("Address") val address: String,
    @SerializedName("IsActive") val isActive: Boolean = true,
)

data class ClientListResponse(
    val clients: List<ClientResponse>,
)
