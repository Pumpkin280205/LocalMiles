package com.example.localmiles.Core.Network.Dto

import com.google.gson.annotations.SerializedName

//Body for POST / DELETE devices/token — this phone's FCM registration token.
data class DeviceTokenRequest(
    @SerializedName("Token") val token: String,
    @SerializedName("Platform") val platform: String = "android",
)

data class DeviceTokenRegisterResponse(
    val registered: Boolean,
)

data class DeviceTokenUnregisterResponse(
    val unregistered: Boolean,
)
