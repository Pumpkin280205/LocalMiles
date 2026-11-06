package com.example.localmiles.Core.Utils

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

data class Coordinates(val latitude: Double, val longitude: Double)

/**
 * Turns a typed delivery address into coordinates with the platform Geocoder
 */
object AddressGeocoder {

    suspend fun geocode(context: Context, address: String): Coordinates? {
        if (address.isBlank() || !Geocoder.isPresent()) return null

        val geocoder = Geocoder(context, Locale.getDefault())

        return suspendCancellableCoroutine { continuation ->
            geocoder.getFromLocationName(
                address,
                1,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<android.location.Address>) {
                        val match = addresses.firstOrNull()
                        continuation.resume(match?.let { Coordinates(it.latitude, it.longitude) })
                    }

                    override fun onError(errorMessage: String?) {
                        continuation.resume(null)
                    }
                }
            )
        }
    }
}
