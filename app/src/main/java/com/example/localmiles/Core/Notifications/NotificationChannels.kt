package com.example.localmiles.Core.Notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationChannels {

    // Must match ANDROID_CHANNEL_ID in the API's src/lib/push.ts.
    const val UPDATES = "localmiles_updates"

    // The ongoing "sharing your location" notification while a job is En Route.
    const val TRACKING = "localmiles_tracking"

    /** Safe to call repeatedly; Android ignores channels that already exist. */
    fun create(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(
            NotificationChannel(
                UPDATES,
                "Delivery updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "New jobs, completed deliveries and sync problems."
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                TRACKING,
                "Location sharing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shown while your location is shared with your dispatcher."
            }
        )
    }
}
