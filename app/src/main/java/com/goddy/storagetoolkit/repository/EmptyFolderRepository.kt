package com.goddy.storagetoolkit.repository

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.goddy.storagetoolkit.models.FolderItem
import com.goddy.storagetoolkit.scanner.EmptyFolderScanner
import com.goddy.storagetoolkit.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EmptyFolderRepository(
    private val context: Context,
    private val scanner: EmptyFolderScanner
) {

    suspend fun scan(root: DocumentFile): List<FolderItem> = scanner.scan(root)

    suspend fun delete(folders: List<FolderItem>): Int = withContext(Dispatchers.IO) {
        var deletedCount = 0
        for (folder in folders) {
            val doc = FileUtils.resolveDocumentFile(context, folder.uriString)
            // delete() on a directory removes its contents along with it.
            if (doc?.delete() == true) deletedCount++
        }
        deletedCount
    }
}
