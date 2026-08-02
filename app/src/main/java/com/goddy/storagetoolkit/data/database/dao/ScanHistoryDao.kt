package com.goddy.storagetoolkit.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.goddy.storagetoolkit.data.database.entities.ScanHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {

    @Insert
    suspend fun insert(entry: ScanHistoryEntity)

    @Query("SELECT * FROM scan_history ORDER BY scanDate DESC")
    fun observeAll(): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history WHERE scanType = :scanType ORDER BY scanDate DESC LIMIT 1")
    suspend fun lastScanFor(scanType: String): ScanHistoryEntity?

    @Query("DELETE FROM scan_history")
    suspend fun deleteAll()
}
