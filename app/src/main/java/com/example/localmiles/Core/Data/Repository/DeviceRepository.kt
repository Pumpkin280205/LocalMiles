package com.example.localmiles.Core.Data.Repository

import com.example.localmiles.Core.Network.ApiService
import com.example.localmiles.Core.Network.Dto.DeviceTokenRequest
import com.example.localmiles.Core.Network.RetrofitClient
import com.example.localmiles.Core.Utils.Result
import com.example.localmiles.Core.Utils.SessionManager

class DeviceRepository(
    session: SessionManager,
    private val api: ApiService = RetrofitClient.getApiService(session),
) {

    //POST devices/token — links this phone's FCM token to the signed-in user.
    suspend fun registerToken(token: String): Result<Boolean> =
        apiCall { api.registerDeviceToken(DeviceTokenRequest(token)).registered }

    //DELETE devices/token — [sessionToken] is the JWT captured before logout cleared it.
    suspend fun unregisterToken(token: String, sessionToken: String): Result<Boolean> =
        apiCall { api.unregisterDeviceToken("Bearer $sessionToken", DeviceTokenRequest(token)).unregistered }
}
