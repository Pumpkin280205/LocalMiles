package com.example.localmiles.Core.Data.Local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Local Room database for storing offline driver actions.
 *
 * Reference:
 * Android Developers (2026) Save data in a local database using Room.
 * Available at: https://developer.android.com/training/data-storage/room
 */
@Database(
    entities = [OfflineActionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun offlineActionDao(): OfflineActionDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "localmiles_offline_database"
                ).build()

                INSTANCE = instance
                instance
            }
        }
    }
}