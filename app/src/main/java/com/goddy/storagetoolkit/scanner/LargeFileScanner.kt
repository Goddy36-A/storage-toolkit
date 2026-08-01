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
 * Recursively finds files at or above [MIN_SIZE_BYTES] (the smallest filter tier the
 * UI offers, 100 MB) across a folder tree. Scanning once at the lowest threshold and
 * filtering further in the ViewModel means switching between the 100MB/500MB/1GB
 * filters doesn't require a rescan. Skips hidden folders, "Android", and the user's
 * Settings ignore list, same as the other scanners.
 */
class LargeFileScanner {

    companion object {
        const val MIN_SIZE_BYTES = 100L * 1024 * 1024
    }

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

            val size = child.length()
            if (size < MIN_SIZE_BYTES) continue

            val extension = FileUtils.extensionOf(name)
            results += FileItem(
                documentId = child.uri.toString(),
                uriString = child.uri.toString(),
                name = name,
                extension = extension,
                sizeBytes = size,
                lastModified = child.lastModified(),
                category = FileCategory.fromExtension(extension),
                relativePath = relativePath
            )
        }
    }
}
