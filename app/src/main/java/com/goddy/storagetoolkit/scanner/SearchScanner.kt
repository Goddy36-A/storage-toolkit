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
 * Builds a flat index of every file under a root, once. Search itself is just an
 * in-memory filter over this list (see SearchViewModel) rather than a filesystem
 * walk per keystroke -- that's what keeps search feeling instant. The tradeoff is
 * that the index can go stale if files change outside the app; the UI surfaces a
 * "Rebuild Index" action for that.
 */
class SearchScanner {

    suspend fun buildIndex(root: DocumentFile, ignoredFolders: Set<String> = emptySet()): List<FileItem> =
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

            val extension = FileUtils.extensionOf(name)
            results += FileItem(
                documentId = child.uri.toString(),
                uriString = child.uri.toString(),
                name = name,
                extension = extension,
                sizeBytes = child.length(),
                lastModified = child.lastModified(),
                category = FileCategory.fromExtension(extension),
                relativePath = relativePath
            )
        }
    }
}
