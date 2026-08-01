package com.goddy.storagetoolkit.scanner

import androidx.documentfile.provider.DocumentFile
import com.goddy.storagetoolkit.models.FileCategory
import com.goddy.storagetoolkit.models.FileItem
import com.goddy.storagetoolkit.utils.FileUtils
import com.goddy.storagetoolkit.utils.FolderSkipRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Recursively scans a folder tree for files whose size is exactly 0 bytes.
 * Skips hidden folders (leading '.'), "Android" (protected storage regardless), and
 * anything the user has added to their ignore list in Settings.
 */
class ZeroByteScanner {

    suspend fun scan(root: DocumentFile, ignoredFolders: Set<String> = emptySet()): List<FileItem> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<FileItem>()
            walk(root, "", results, ignoredFolders)
            results
        }

    private suspend fun walk(
        folder: DocumentFile,
        relativePath: String,
        results: MutableList<FileItem>,
        ignoredFolders: Set<String>
    ) {
        currentCoroutineContext().ensureActive()
        for (child in folder.listFiles()) {
            currentCoroutineContext().ensureActive()
            val name = child.name ?: continue

            if (child.isDirectory) {
                if (FolderSkipRules.shouldSkip(name, ignoredFolders)) continue
                val childPath = if (relativePath.isEmpty()) name else "$relativePath/$name"
                walk(child, childPath, results, ignoredFolders)
                continue
            }

            if (child.length() == 0L) {
                val extension = FileUtils.extensionOf(name)
                results += FileItem(
                    documentId = child.uri.toString(),
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
