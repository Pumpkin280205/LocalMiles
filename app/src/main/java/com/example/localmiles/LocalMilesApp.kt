package com.example.localmiles

import android.app.Application
import com.example.localmiles.Core.Notifications.NotificationChannels

class LocalMilesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Before any push can arrive: the API sends on the "localmiles_updates" channel.
        NotificationChannels.create(this)
        // send anything still queued from last time once there is internet
        com.example.localmiles.Screens.Driver.Sync.OfflineQueueManager.schedule(this)
    }
}
