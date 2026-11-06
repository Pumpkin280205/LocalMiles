package com.example.localmiles.Screens.Driver.Sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.localmiles.Core.Data.Local.AppDatabase
import com.example.localmiles.Core.Data.Local.OfflineActionDao
import com.example.localmiles.Core.Data.Local.OfflineActionEntity
import com.example.localmiles.Core.Data.Repository.SyncRepository
import com.example.localmiles.Core.Network.Dto.SyncActionRequest
import com.example.localmiles.Core.Network.Dto.SyncResultItem
import com.example.localmiles.Core.Utils.Result
import com.example.localmiles.Core.Utils.SessionManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import java.util.concurrent.TimeUnit

object OfflineStatus {
    const val PENDING = "Pending"
    const val SYNCED = "Synced"
    const val FAILED = "Failed"
}

// How many actions are pending, failed and synced (shown in the banner)
data class SyncSummary(val pending: Int = 0, val failed: Int = 0, val synced: Int = 0)

fun summarize(actions: List<OfflineActionEntity>) = SyncSummary(
    pending = actions.count { it.status == OfflineStatus.PENDING },
    failed = actions.count { it.status == OfflineStatus.FAILED },
    synced = actions.count { it.status == OfflineStatus.SYNCED },
)

// Turns the server's answer for an action into our status.
// null means leave it as pending ("queued" = server has it but hasn't applied it yet)
fun statusFor(outcome: String): String? = when (outcome) {
    "applied", "duplicate" -> OfflineStatus.SYNCED
    "failed", "invalid" -> OfflineStatus.FAILED
    else -> null
}

// Offline queue: actions made without internet are saved in Room as Pending,
// then sent to POST /sync (oldest first, 50 at a time) when the network is back
class OfflineQueueManager(
    private val dao: OfflineActionDao,
    private val submit: suspend (List<SyncActionRequest>) -> Result<List<SyncResultItem>>,
    private val schedule: () -> Unit,
    private val userId: () -> String,
) {
    private val gson = Gson()

    // Saves the action locally and asks WorkManager to sync when there is a network
    suspend fun enqueue(
        entityType: String,
        entityId: Long,
        actionType: String,
        payload: Map<String, Any>,
    ) {
        dao.insert(
            OfflineActionEntity(
                entityType = entityType,
                entityId = entityId,
                actionType = actionType,
                payload = gson.toJson(payload),
                userId = userId(),
            )
        )
        schedule()
    }

    fun observeSummary(): Flow<SyncSummary> = dao.observeActions(userId()).map(::summarize)

    // This user's unsynced actions (used to show the right status on screen)
    suspend fun pendingActions(): List<OfflineActionEntity> = dao.getPendingActions(userId())

    suspend fun dismissFailed() = dao.deleteByStatus(OfflineStatus.FAILED, userId())

    // Puts rejected actions back in the queue. Needs a new id because the server remembers the old one
    suspend fun retryFailed() {
        val failed = dao.getFailedActions(userId())
        if (failed.isEmpty()) return
        failed.forEach {
            dao.insert(it.copy(id = UUID.randomUUID().toString(), status = OfflineStatus.PENDING))
            dao.delete(it.id)
        }
        schedule()
    }

    // Sends all pending actions. Returns false if we should try again later (no network / server down)
    suspend fun syncPending(): Boolean {
        for (batch in dao.getPendingActions(userId()).chunked(MAX_BATCH)) {
            val results = when (val r = submit(batch.map(::toRequest))) {
                is Result.Success -> r.data
                is Result.Error -> return false
            }
            val byId = results.associateBy { it.syncActionId.lowercase() }
            for (action in batch) {
                val status = byId[action.id.lowercase()]?.let { statusFor(it.outcome) } ?: continue
                dao.updateStatus(action.id, status)
            }
        }
        return true
    }

    private fun toRequest(a: OfflineActionEntity) = SyncActionRequest(
        syncActionId = a.id,
        actionType = a.actionType,
        entityType = a.entityType,
        entityId = a.entityId.toString(),
        payload = gson.fromJson(a.payload, MAP_TYPE),
    )

    companion object {
        // the API only takes 1 to 50 actions per request
        private const val MAX_BATCH = 50
        private const val WORK_NAME = "offline-sync"
        private val MAP_TYPE = object : TypeToken<Map<String, Any>>() {}.type

        fun create(context: Context): OfflineQueueManager {
            val app = context.applicationContext
            val sync = SyncRepository(SessionManager(app))
            return OfflineQueueManager(
                dao = AppDatabase.getDatabase(app).offlineActionDao(),
                submit = sync::submit,
                schedule = { schedule(app) },
                userId = { SessionManager(app).getUserId().orEmpty() },
            )
        }

        // Runs SyncWorker once there is internet. APPEND so a sync that is already running isn't dropped
        fun schedule(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }
    }
}
