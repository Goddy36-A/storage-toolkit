package com.goddy.storagetoolkit.repository

import androidx.documentfile.provider.DocumentFile
import com.goddy.storagetoolkit.models.FileItem
import com.goddy.storagetoolkit.scanner.ZeroByteScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ZeroByteRepository(
    private val scanner: ZeroByteScanner,
    private val recycleBinRepository: RecycleBinRepository
) {

    suspend fun scan(root: DocumentFile, ignoredFolders: Set<String> = emptySet()): List<FileItem> =
        scanner.scan(root, ignoredFolders)

    /** Moves files to the recycle bin rather than deleting them outright. */
    suspend fun delete(files: List<FileItem>): Int = withContext(Dispatchers.IO) {
        var movedCount = 0
        for (file in files) {
            val moved = recycleBinRepository.moveToBin(file.uriString, file.name, file.sizeBytes, "Zero-byte Files")
            if (moved) movedCount++
        }
        movedCount
    }
}
