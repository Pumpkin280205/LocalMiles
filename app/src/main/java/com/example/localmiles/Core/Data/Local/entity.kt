package com.example.localmiles.Core.Data.Local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Stores driver actions locally until they can be synchronised.
 *
 * References:
 * Android Developers (2026) Save data in a local database using Room.
 * Available at: https://developer.android.com/training/data-storage/room
 */
@Entity(tableName = "offline_actions")
data class OfflineActionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val entityType: String,
    val entityId: Long,
    val actionType: String,
    val payload: String,

    // who made the action, so another account doesn't send it
    @ColumnInfo(defaultValue = "''")
    val userId: String = "",

    val status: String = "Pending",
    val createdAt: Long = System.currentTimeMillis()
)