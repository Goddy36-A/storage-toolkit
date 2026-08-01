package com.goddy.storagetoolkit.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.goddy.storagetoolkit.models.ApkFileInfo
import com.goddy.storagetoolkit.scanner.ApkScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ApkRepository(
    private val context: Context,
    private val scanner: ApkScanner
) {

    suspend fun scan(root: DocumentFile): List<ApkFileInfo> = scanner.scan(root)

    suspend fun delete(files: List<ApkFileInfo>): Int = withContext(Dispatchers.IO) {
        var deletedCount = 0
        for (file in files) {
            val doc = DocumentFile.fromSingleUri(context, Uri.parse(file.uriString))
            if (doc?.delete() == true) deletedCount++
        }
        deletedCount
    }
}
