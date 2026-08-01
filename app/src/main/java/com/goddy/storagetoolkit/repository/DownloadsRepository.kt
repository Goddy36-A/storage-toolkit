package com.goddy.storagetoolkit.repository

import android.content.Context
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.goddy.storagetoolkit.models.FileCategory
import com.goddy.storagetoolkit.models.FileItem
import com.goddy.storagetoolkit.scanner.DownloadsScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DownloadsRepository(
    private val context: Context,
    private val scanner: DownloadsScanner
) {

    suspend fun scan(root: DocumentFile): List<FileItem> = scanner.scan(root)

    /**
     * Moves each given file into a same-named subfolder of [root] based on its category,
     * e.g. Downloads/Images, Downloads/Documents. Uses DocumentsContract.moveDocument so
     * the move happens within the same SAF tree without a copy+delete round trip.
     */
    suspend fun organize(root: DocumentFile, files: List<FileItem>): Int = withContext(Dispatchers.IO) {
        var movedCount = 0
        val resolver = context.contentResolver
        val folderCache = mutableMapOf<FileCategory, DocumentFile>()

        for (item in files) {
            val cachedFolder = folderCache[item.category]
            val targetFolder: DocumentFile = if (cachedFolder != null) {
                cachedFolder
            } else {
                val resolvedFolder = root.findFile(item.category.label)
                    ?: root.createDirectory(item.category.label)
                if (resolvedFolder == null) continue
                folderCache[item.category] = resolvedFolder
                resolvedFolder
            }

            val sourceDoc = DocumentFile.fromSingleUri(context, android.net.Uri.parse(item.uriString)) ?: continue
            val sourceParentUri = root.uri
            try {
                DocumentsContract.moveDocument(
                    resolver,
                    sourceDoc.uri,
                    sourceParentUri,
                    targetFolder.uri
                )
                movedCount++
            } catch (e: Exception) {
                // Skip files that fail to move (e.g. already organized, permission edge case)
                // and continue with the rest of the batch.
            }
        }
        movedCount
    }
}
