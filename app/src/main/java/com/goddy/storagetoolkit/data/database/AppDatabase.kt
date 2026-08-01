package com.goddy.storagetoolkit.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.goddy.storagetoolkit.data.database.dao.ScanHistoryDao
import com.goddy.storagetoolkit.data.database.entities.ScanHistoryEntity

@Database(entities = [ScanHistoryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun scanHistoryDao(): ScanHistoryDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "storage_toolkit.db"
                ).build().also { instance = it }
            }
        }
    }
}
