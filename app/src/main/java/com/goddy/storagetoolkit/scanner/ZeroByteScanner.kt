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
 * Recursively scans a SAF-granted folder tree for files whose size is exactly 0 bytes.
 * Skips hidden folders (leading '.') and "Android", which holds per-app protected
 * storage that scoped storage restricts access to anyway.
 */
class ZeroByteScanner {

    private val skippedFolderNames = setOf("Android")

    suspend fun scan(root: DocumentFile): List<FileItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<FileItem>()
        walk(root, "", results)
        results
    }

    private suspend fun walk(folder: DocumentFile, relativePath: String, results: MutableList<FileItem>) {
        currentCoroutineContext().ensureActive()
        for (child in folder.listFiles()) {
            currentCoroutineContext().ensureActive()
            val name = child.name ?: continue

            if (child.isDirectory) {
                if (name.startsWith(".") || name in skippedFolderNames) continue
                val childPath = if (relativePath.isEmpty()) name else "$relativePath/$name"
                walk(child, childPath, results)
                continue
            }

            if (child.length() == 0L) {
                val extension = FileUtils.extensionOf(name)
                results += FileItem(
                    documentId = child.uri.lastPathSegment ?: name,
                    uriString = child.uri.toString(),
                    name = name,
                    extension = extension,
                    sizeBytes = 0L,
                    lastModified = child.lastModified(),
                    category = FileCategory.fromExtension(extension),
                    relativePath = relativePath
                )
            }
        }
    }
}
