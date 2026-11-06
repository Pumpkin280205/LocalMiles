package com.example.localmiles.Core.Data.Local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Runs the real Room queries on an in-memory database (needs a phone or emulator)
@RunWith(AndroidJUnit4::class)
class OfflineActionDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: OfflineActionDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.offlineActionDao()
    }

    private companion object {
        const val USER = "u1"
    }

    @After
    fun tearDown() = db.close()

    private fun action(id: String, createdAt: Long) = OfflineActionEntity(
        id = id, entityType = "Stop", entityId = 1, actionType = "UpdateStopStatus",
        payload = "{}", createdAt = createdAt, userId = USER,
    )

    @Test
    fun pendingActionsComeBackOldestFirst() = runBlocking {
        dao.insert(action("b", createdAt = 2))
        dao.insert(action("a", createdAt = 1))

        assertEquals(listOf("a", "b"), dao.getPendingActions(USER).map { it.id })
    }

    @Test
    fun syncedActionsLeaveThePendingList() = runBlocking {
        dao.insert(action("a", 1))
        dao.updateStatus("a", "Synced")

        assertEquals(0, dao.getPendingActions(USER).size)
        assertEquals(0, dao.observePendingCount(USER).first())
    }

    @Test
    fun insertingTheSameIdReplacesTheRow() = runBlocking {
        dao.insert(action("a", 1))
        dao.insert(action("a", 1).copy(payload = """{"Status":"Arrived"}"""))

        assertEquals(1, dao.observeActions(USER).first().size)
    }

    @Test
    fun deleteByStatusOnlyRemovesThatStatus() = runBlocking {
        dao.insert(action("a", 1))
        dao.insert(action("b", 2))
        dao.updateStatus("b", "Failed")

        dao.deleteByStatus("Failed", USER)

        assertEquals(listOf("a"), dao.observeActions(USER).first().map { it.id })
    }

    @Test
    fun otherUsersActionsAreInvisible() = runBlocking {
        dao.insert(action("a", 1))
        dao.insert(action("b", 2).copy(userId = "someone-else"))

        assertEquals(listOf("a"), dao.getPendingActions(USER).map { it.id })
        assertEquals(1, dao.observeActions(USER).first().size)
    }
}
