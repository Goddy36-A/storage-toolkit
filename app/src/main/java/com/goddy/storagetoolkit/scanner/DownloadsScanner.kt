package com.goddy.storagetoolkit.scanner

import androidx.documentfile.provider.DocumentFile
import com.goddy.storagetoolkit.models.FileCategory
import com.goddy.storagetoolkit.models.FileItem
import com.goddy.storagetoolkit.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Scans a user-granted SAF folder (typically Downloads) and classifies its files by
 * extension. Runs on Dispatchers.IO and is cooperatively cancellable via coroutine scope.
 */
class DownloadsScanner {

    suspend fun scan(root: DocumentFile): List<FileItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<FileItem>()
        for (child in root.listFiles()) {
            currentCoroutineContext().ensureActive()
            if (child.isDirectory) continue
            val name = child.name ?: continue
            val extension = FileUtils.extensionOf(name)
            results += FileItem(
                documentId = child.uri.toString(),
                uriString = child.uri.toString(),
                name = name,
                extension = extension,
                sizeBytes = child.length(),
                lastModified = child.lastModified(),
                category = FileCategory.fromExtension(extension)
            )
        }
        results
    }
}
