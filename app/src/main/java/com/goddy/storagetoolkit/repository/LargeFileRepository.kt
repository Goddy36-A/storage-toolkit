package com.goddy.storagetoolkit.repository

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.goddy.storagetoolkit.models.FileItem
import com.goddy.storagetoolkit.scanner.LargeFileScanner
import com.goddy.storagetoolkit.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LargeFileRepository(
    private val context: Context,
    private val scanner: LargeFileScanner
) {

    suspend fun scan(root: DocumentFile): List<FileItem> = scanner.scan(root)

    suspend fun delete(files: List<FileItem>): Int = withContext(Dispatchers.IO) {
        var deletedCount = 0
        for (file in files) {
            val doc = FileUtils.resolveDocumentFile(context, file.uriString)
            if (doc?.delete() == true) deletedCount++
        }
        deletedCount
    }
}
