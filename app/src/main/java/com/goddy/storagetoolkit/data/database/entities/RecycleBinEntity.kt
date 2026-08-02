package com.goddy.storagetoolkit.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A file that's been moved to the app's recycle bin rather than deleted outright.
 * [binPath] is where the real file currently sits (app-private external storage);
 * [originalPath] is where it came from, used to restore it back to the same location.
 */
@Entity(tableName = "recycle_bin")
data class RecycleBinEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalName: String,
    val originalPath: String,
    val binPath: String,
    val sizeBytes: Long,
    val sourceFeature: String,
    val deletedAt: Long
)
