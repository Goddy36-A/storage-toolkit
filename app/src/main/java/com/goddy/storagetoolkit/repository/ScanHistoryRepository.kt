package com.goddy.storagetoolkit.repository

import com.goddy.storagetoolkit.data.database.dao.ScanHistoryDao
import com.goddy.storagetoolkit.data.database.entities.ScanHistoryEntity
import kotlinx.coroutines.flow.Flow

enum class ScanType(val key: String) {
    DOWNLOADS_ORGANIZER("downloads_organizer"),
    APK_MANAGER("apk_manager"),
    ZERO_BYTE("zero_byte"),
    EMPTY_FOLDER("empty_folder")
}

class ScanHistoryRepository(private val dao: ScanHistoryDao) {

    val history: Flow<List<ScanHistoryEntity>> = dao.observeAll()

    suspend fun record(scanType: ScanType, filesFound: Int, filesRemoved: Int, spaceSavedBytes: Long) {
        dao.insert(
            ScanHistoryEntity(
                scanType = scanType.key,
                scanDate = System.currentTimeMillis(),
                filesFound = filesFound,
                filesRemoved = filesRemoved,
                spaceSavedBytes = spaceSavedBytes
            )
        )
    }

    suspend fun lastScanTime(scanType: ScanType): Long? = dao.lastScanFor(scanType.key)?.scanDate
}
