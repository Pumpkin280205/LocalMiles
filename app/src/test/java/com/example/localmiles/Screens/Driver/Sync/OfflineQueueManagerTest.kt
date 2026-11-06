package com.example.localmiles.Screens.Driver.Sync

import com.example.localmiles.Core.Data.Local.OfflineActionDao
import com.example.localmiles.Core.Data.Local.OfflineActionEntity
import com.example.localmiles.Core.Network.Dto.SyncActionRequest
import com.example.localmiles.Core.Network.Dto.SyncResultItem
import com.example.localmiles.Core.Utils.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

// Fake DAO that keeps everything in memory
private class FakeDao : OfflineActionDao {
    val rows = MutableStateFlow<List<OfflineActionEntity>>(emptyList())

    override suspend fun insert(action: OfflineActionEntity) {
        rows.value = rows.value.filter { it.id != action.id } + action
    }

    override suspend fun getPendingActions(userId: String) =
        rows.value.filter { it.status == OfflineStatus.PENDING && it.userId == userId }
            .sortedBy { it.createdAt }

    override suspend fun getFailedActions(userId: String) =
        rows.value.filter { it.status == OfflineStatus.FAILED && it.userId == userId }

    override fun observeActions(userId: String): Flow<List<OfflineActionEntity>> =
        rows.map { list -> list.filter { it.userId == userId } }

    override suspend fun updateStatus(actionId: String, status: String) {
        rows.value = rows.value.map { if (it.id == actionId) it.copy(status = status) else it }
    }

    override fun observePendingCount(userId: String): Flow<Int> =
        rows.map { list -> list.count { it.status == OfflineStatus.PENDING && it.userId == userId } }

    override suspend fun deleteByStatus(status: String, userId: String) {
        rows.value = rows.value.filter { !(it.status == status && it.userId == userId) }
    }

    override suspend fun delete(actionId: String) {
        rows.value = rows.value.filter { it.id != actionId }
    }
}

class OfflineQueueManagerTest {

    private val dao = FakeDao()
    private val sent = mutableListOf<List<SyncActionRequest>>()
    private var scheduled = 0
    private var currentUser = "driver-1"

    // fake server: says every action was applied
    private var server: (List<SyncActionRequest>) -> Result<List<SyncResultItem>> = { batch ->
        Result.Success(batch.map { SyncResultItem(it.syncActionId, "applied", "Completed") })
    }

    private val queue = OfflineQueueManager(
        dao = dao,
        submit = { batch -> sent.add(batch); server(batch) },
        schedule = { scheduled++ },
        userId = { currentUser },
    )

    private suspend fun enqueueStop(id: Long = 1L) =
        queue.enqueue("Stop", id, "UpdateStopStatus", mapOf("Status" to "Arrived"))

    @Test
    fun `enqueue stores a pending action and schedules a sync`() = runTest {
        enqueueStop(7)

        val row = dao.rows.value.single()
        assertEquals(OfflineStatus.PENDING, row.status)
        assertEquals("UpdateStopStatus", row.actionType)
        assertEquals(7L, row.entityId)
        assertEquals("driver-1", row.userId)
        assertEquals(1, scheduled)
    }

    @Test
    fun `sync sends the stored payload and marks applied actions as synced`() = runTest {
        enqueueStop(7)

        assertTrue(queue.syncPending())

        val request = sent.single().single()
        assertEquals("7", request.entityId)
        assertEquals("Stop", request.entityType)
        assertEquals("Arrived", request.payload["Status"])
        assertEquals(OfflineStatus.SYNCED, dao.rows.value.single().status)
    }

    @Test
    fun `duplicate counts as synced, failed and invalid as failed`() = runTest {
        enqueueStop(1); enqueueStop(2); enqueueStop(3)
        val outcomes = listOf("duplicate", "failed", "invalid")
        server = { batch ->
            Result.Success(batch.mapIndexed { i, a -> SyncResultItem(a.syncActionId, outcomes[i]) })
        }

        queue.syncPending()

        assertEquals(
            listOf(OfflineStatus.SYNCED, OfflineStatus.FAILED, OfflineStatus.FAILED),
            dao.rows.value.sortedBy { it.entityId }.map { it.status },
        )
    }

    @Test
    fun `queued outcome leaves the action pending`() = runTest {
        enqueueStop()
        server = { batch -> Result.Success(batch.map { SyncResultItem(it.syncActionId, "queued") }) }

        queue.syncPending()

        assertEquals(OfflineStatus.PENDING, dao.rows.value.single().status)
    }

    @Test
    fun `network failure keeps actions pending and asks for a retry`() = runTest {
        enqueueStop()
        server = { Result.Error("Network error", IOException()) }

        assertFalse(queue.syncPending())

        assertEquals(OfflineStatus.PENDING, dao.rows.value.single().status)
    }

    @Test
    fun `nothing pending means no request is made`() = runTest {
        assertTrue(queue.syncPending())
        assertTrue(sent.isEmpty())
    }

    @Test
    fun `actions are sent oldest first in batches of 50`() = runTest {
        repeat(120) { i ->
            dao.insert(
                OfflineActionEntity(
                    entityType = "Stop", entityId = i.toLong(), actionType = "UpdateStopStatus",
                    payload = """{"Status":"Arrived"}""", createdAt = i.toLong(), userId = currentUser,
                )
            )
        }

        queue.syncPending()

        assertEquals(listOf(50, 50, 20), sent.map { it.size })
        assertEquals((0L..119L).map { it.toString() }, sent.flatten().map { it.entityId })
        assertTrue(dao.rows.value.all { it.status == OfflineStatus.SYNCED })
    }

    @Test
    fun `a second sync does not resend actions that already synced`() = runTest {
        enqueueStop()
        queue.syncPending()
        queue.syncPending()

        assertEquals(1, sent.size)
    }

    @Test
    fun `summary counts each status and dismiss removes failed rows`() = runTest {
        enqueueStop(1); enqueueStop(2); enqueueStop(3)
        val outcomes = listOf("applied", "failed")
        server = { batch ->
            // only answers the first two, so the third stays pending
            Result.Success(batch.take(2).mapIndexed { i, a -> SyncResultItem(a.syncActionId, outcomes[i]) })
        }
        queue.syncPending()

        assertEquals(SyncSummary(pending = 1, failed = 1, synced = 1), summarize(dao.rows.value))

        queue.dismissFailed()

        assertEquals(SyncSummary(pending = 1, failed = 0, synced = 1), summarize(dao.rows.value))
    }

    @Test
    fun `another account never sends or sees the first account's actions`() = runTest {
        enqueueStop(1)

        currentUser = "driver-2"
        assertTrue(queue.syncPending())
        assertTrue(sent.isEmpty())
        assertTrue(queue.pendingActions().isEmpty())

        currentUser = "driver-1"
        queue.syncPending()
        assertEquals(1, sent.size)
    }

    @Test
    fun `retry re-queues rejected actions under a new id and keeps their order`() = runTest {
        enqueueStop(1)
        server = { batch ->
            Result.Success(batch.map { SyncResultItem(it.syncActionId, "failed", error = "nope") })
        }
        queue.syncPending()
        val original = dao.rows.value.single()
        scheduled = 0

        queue.retryFailed()

        val retried = dao.rows.value.single()
        assertEquals(OfflineStatus.PENDING, retried.status)
        assertTrue(retried.id != original.id)
        assertEquals(original.createdAt, retried.createdAt)
        assertEquals(1, scheduled)
    }

    @Test
    fun `retry with nothing rejected does nothing`() = runTest {
        queue.retryFailed()
        assertEquals(0, scheduled)
    }

    @Test
    fun `status mapping follows the server outcomes`() {
        assertEquals(OfflineStatus.SYNCED, statusFor("applied"))
        assertEquals(OfflineStatus.SYNCED, statusFor("duplicate"))
        assertEquals(OfflineStatus.FAILED, statusFor("failed"))
        assertEquals(OfflineStatus.FAILED, statusFor("invalid"))
        assertEquals(null, statusFor("queued"))
    }
}
