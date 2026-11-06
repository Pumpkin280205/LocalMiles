package com.example.localmiles.Core.Data.Local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Database operations for offline synchronisation.
 *
 * Reference:
 * Android Developers (2026) Accessing data using Room DAOs.
 * https://developer.android.com/training/data-storage/room/accessing-data
 */
@Dao
interface OfflineActionDao {

    // Save an action locally when the device is offline.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(action: OfflineActionEntity)

    // Retrieve actions waiting to be synchronised.
    @Query("""
        SELECT * FROM offline_actions
        WHERE status = 'Pending' AND userId = :userId
        ORDER BY createdAt ASC
    """)
    suspend fun getPendingActions(userId: String): List<OfflineActionEntity>

    // Actions the server rejected (so they can be retried)
    @Query("SELECT * FROM offline_actions WHERE status = 'Failed' AND userId = :userId")
    suspend fun getFailedActions(userId: String): List<OfflineActionEntity>

    // Observe all actions so the UI can display their sync status.
    @Query("""
        SELECT * FROM offline_actions
        WHERE userId = :userId
        ORDER BY createdAt DESC
    """)
    fun observeActions(userId: String): Flow<List<OfflineActionEntity>>

    // Update an action's synchronisation status.
    @Query("""
        UPDATE offline_actions
        SET status = :status
        WHERE id = :actionId
    """)
    suspend fun updateStatus(actionId: String, status: String)

    // Count pending actions.
    @Query("""
        SELECT COUNT(*) FROM offline_actions
        WHERE status = 'Pending' AND userId = :userId
    """)
    fun observePendingCount(userId: String): Flow<Int>

    // Delete rows with a given status (used by Dismiss)
    @Query("DELETE FROM offline_actions WHERE status = :status AND userId = :userId")
    suspend fun deleteByStatus(status: String, userId: String)

    @Query("DELETE FROM offline_actions WHERE id = :actionId")
    suspend fun delete(actionId: String)
}