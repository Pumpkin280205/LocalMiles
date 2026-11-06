package com.example.localmiles.Core.Network.Dto

import com.google.gson.annotations.SerializedName

data class DriverResponse(
    @SerializedName("DriverId") val driverId: Long,
    @SerializedName("UserId") val userId: Long,
    @SerializedName("DepotId") val depotId: Long,
    @SerializedName("EmployeeNumber") val employeeNumber: String,
    @SerializedName("PhoneNumber") val phoneNumber: String,
    @SerializedName("LicenceNumber") val licenceNumber: String,
    @SerializedName("IsAvailable") val isAvailable: Boolean,
    @SerializedName("IsActive") val isActive: Boolean,
)

data class DriverListResponse(
    val drivers: List<DriverResponse>,
)

data class DriverDetailResponse(
    val driver: DriverResponse,
)

data class DriverUpsertRequest(
    @SerializedName("UserId") val userId: Long? = null,
    @SerializedName("DepotId") val depotId: Long? = null,
    @SerializedName("EmployeeNumber") val employeeNumber: String? = null,
    @SerializedName("PhoneNumber") val phoneNumber: String? = null,
    @SerializedName("LicenceNumber") val licenceNumber: String? = null,
    @SerializedName("IsAvailable") val isAvailable: Boolean? = null,
    @SerializedName("IsActive") val isActive: Boolean? = null,
)
