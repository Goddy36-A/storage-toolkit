package com.goddy.storagetoolkit.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.goddy.storagetoolkit.data.database.entities.RecycleBinEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecycleBinDao {

    @Insert
    suspend fun insert(entry: RecycleBinEntity): Long

    @Query("SELECT * FROM recycle_bin ORDER BY deletedAt DESC")
    fun observeAll(): Flow<List<RecycleBinEntity>>

    @Query("SELECT * FROM recycle_bin WHERE deletedAt < :cutoffMillis")
    suspend fun findOlderThan(cutoffMillis: Long): List<RecycleBinEntity>

    @Delete
    suspend fun delete(entry: RecycleBinEntity)
}
