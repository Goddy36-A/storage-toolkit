package com.goddy.storagetoolkit.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.goddy.storagetoolkit.models.FileItem
import com.goddy.storagetoolkit.scanner.ZeroByteScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ZeroByteRepository(
    private val context: Context,
    private val scanner: ZeroByteScanner
) {

    suspend fun scan(root: DocumentFile): List<FileItem> = scanner.scan(root)

    suspend fun delete(files: List<FileItem>): Int = withContext(Dispatchers.IO) {
        var deletedCount = 0
        for (file in files) {
            val doc = DocumentFile.fromSingleUri(context, Uri.parse(file.uriString))
            if (doc?.delete() == true) deletedCount++
        }
        deletedCount
    }
}
