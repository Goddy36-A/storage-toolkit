package com.goddy.storagetoolkit.repository

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.goddy.storagetoolkit.models.DuplicateGroup
import com.goddy.storagetoolkit.models.FileItem
import com.goddy.storagetoolkit.scanner.DuplicateScanner
import com.goddy.storagetoolkit.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DuplicateRepository(
    private val context: Context,
    private val scanner: DuplicateScanner
) {

    suspend fun scan(root: DocumentFile, ignoredFolders: Set<String> = emptySet()): List<DuplicateGroup> =
        scanner.scan(root, ignoredFolders)

    suspend fun delete(files: List<FileItem>): Int = withContext(Dispatchers.IO) {
        var deletedCount = 0
        for (file in files) {
            val doc = FileUtils.resolveDocumentFile(context, file.uriString)
            if (doc?.delete() == true) deletedCount++
        }
        deletedCount
    }
}
