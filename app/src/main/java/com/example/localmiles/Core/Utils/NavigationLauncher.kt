package com.example.localmiles.Core.Utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Opens turn-by-turn directions to a stop in the driver's chosen app
 * (Driver Settings → Navigation App), falling back to any maps app.
 */
object NavigationLauncher {

    fun navigateTo(context: Context, latitude: Double, longitude: Double, label: String, app: NavigationApp) {
        val preferred = when (app) {
            NavigationApp.GOOGLE_MAPS ->
                Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$latitude,$longitude"))
                    .setPackage("com.google.android.apps.maps")
            NavigationApp.WAZE ->
                Intent(Intent.ACTION_VIEW, Uri.parse("https://waze.com/ul?ll=$latitude,$longitude&navigate=yes"))
                    .setPackage("com.waze")
        }

        val fallback = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(label)})")
        )

        try {
            context.startActivity(preferred.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            runCatching { context.startActivity(fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }
}
