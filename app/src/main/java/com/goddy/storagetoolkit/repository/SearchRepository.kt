package com.goddy.storagetoolkit.repository

import androidx.documentfile.provider.DocumentFile
import com.goddy.storagetoolkit.models.FileItem
import com.goddy.storagetoolkit.scanner.SearchScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SearchRepository(
    private val scanner: SearchScanner,
    private val recycleBinRepository: RecycleBinRepository
) {

    suspend fun buildIndex(root: DocumentFile, ignoredFolders: Set<String> = emptySet()): List<FileItem> =
        scanner.buildIndex(root, ignoredFolders)

    /** Moves files to the recycle bin rather than deleting them outright, same as every other scanner. */
    suspend fun delete(files: List<FileItem>): Int = withContext(Dispatchers.IO) {
        var movedCount = 0
        for (file in files) {
            val moved = recycleBinRepository.moveToBin(file.uriString, file.name, file.sizeBytes, "Search")
            if (moved) movedCount++
        }
        movedCount
    }
}
