package com.goddy.storagetoolkit.repository

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.goddy.storagetoolkit.models.ApkFileInfo
import com.goddy.storagetoolkit.scanner.ApkScanner
import com.goddy.storagetoolkit.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ApkRepository(
    private val context: Context,
    private val scanner: ApkScanner
) {

    suspend fun scan(root: DocumentFile, ignoredFolders: Set<String> = emptySet()): List<ApkFileInfo> =
        scanner.scan(root, ignoredFolders)

    suspend fun delete(files: List<ApkFileInfo>): Int = withContext(Dispatchers.IO) {
        var deletedCount = 0
        for (file in files) {
            val doc = FileUtils.resolveDocumentFile(context, file.uriString)
            if (doc?.delete() == true) deletedCount++
        }
        deletedCount
    }
}
