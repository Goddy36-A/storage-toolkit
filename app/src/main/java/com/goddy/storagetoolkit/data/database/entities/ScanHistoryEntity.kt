package com.goddy.storagetoolkit.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scanType: String,
    val scanDate: Long,
    val filesFound: Int,
    val filesRemoved: Int,
    val spaceSavedBytes: Long
)
