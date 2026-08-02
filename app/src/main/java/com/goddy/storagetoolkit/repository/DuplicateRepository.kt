package com.goddy.storagetoolkit.repository

import androidx.documentfile.provider.DocumentFile
import com.goddy.storagetoolkit.models.DuplicateGroup
import com.goddy.storagetoolkit.models.FileItem
import com.goddy.storagetoolkit.scanner.DuplicateScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DuplicateRepository(
    private val scanner: DuplicateScanner,
    private val recycleBinRepository: RecycleBinRepository
) {

    suspend fun scan(root: DocumentFile, ignoredFolders: Set<String> = emptySet()): List<DuplicateGroup> =
        scanner.scan(root, ignoredFolders)

    /** Moves files to the recycle bin rather than deleting them outright. */
    suspend fun delete(files: List<FileItem>): Int = withContext(Dispatchers.IO) {
        var movedCount = 0
        for (file in files) {
            val moved = recycleBinRepository.moveToBin(file.uriString, file.name, file.sizeBytes, "Duplicate Files")
            if (moved) movedCount++
        }
        movedCount
    }
}
