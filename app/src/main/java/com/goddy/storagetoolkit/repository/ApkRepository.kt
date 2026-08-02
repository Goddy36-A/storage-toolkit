package com.goddy.storagetoolkit.repository

import androidx.documentfile.provider.DocumentFile
import com.goddy.storagetoolkit.models.ApkFileInfo
import com.goddy.storagetoolkit.scanner.ApkScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ApkRepository(
    private val scanner: ApkScanner,
    private val recycleBinRepository: RecycleBinRepository
) {

    suspend fun scan(root: DocumentFile, ignoredFolders: Set<String> = emptySet()): List<ApkFileInfo> =
        scanner.scan(root, ignoredFolders)

    /** Moves files to the recycle bin rather than deleting them outright. */
    suspend fun delete(files: List<ApkFileInfo>): Int = withContext(Dispatchers.IO) {
        var movedCount = 0
        for (file in files) {
            val moved = recycleBinRepository.moveToBin(file.uriString, file.name, file.sizeBytes, "APK Manager")
            if (moved) movedCount++
        }
        movedCount
    }
}
