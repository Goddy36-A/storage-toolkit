package com.goddy.storagetoolkit.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.goddy.storagetoolkit.data.database.dao.RecycleBinDao
import com.goddy.storagetoolkit.data.database.dao.ScanHistoryDao
import com.goddy.storagetoolkit.data.database.entities.RecycleBinEntity
import com.goddy.storagetoolkit.data.database.entities.ScanHistoryEntity

@Database(entities = [ScanHistoryEntity::class, RecycleBinEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun scanHistoryDao(): ScanHistoryDao
    abstract fun recycleBinDao(): RecycleBinDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "storage_toolkit.db"
                )
                    // No users have shipped data on version 1 yet (app isn't published),
                    // so a destructive migration is fine here rather than writing a real
                    // Migration for a table that didn't exist before.
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
        }
    }
}
