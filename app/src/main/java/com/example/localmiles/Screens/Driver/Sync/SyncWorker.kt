package com.example.localmiles.Screens.Driver.Sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.localmiles.Core.Utils.SessionManager

// Sends the queued offline actions to the API (WorkManager only runs this when online)
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // not signed in, so stop here instead of retrying forever
        if (!SessionManager(applicationContext).isLoggedIn()) return Result.success()
        return if (OfflineQueueManager.create(applicationContext).syncPending()) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
