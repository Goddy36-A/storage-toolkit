package com.goddy.storagetoolkit.scanner

import androidx.documentfile.provider.DocumentFile
import com.goddy.storagetoolkit.models.CategoryBreakdown
import com.goddy.storagetoolkit.models.FileCategory
import com.goddy.storagetoolkit.utils.FileUtils
import com.goddy.storagetoolkit.utils.FolderSkipRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Recursively walks a folder tree once and totals file count + size per [FileCategory].
 * Skips hidden folders, "Android", and the user's Settings ignore list, same as the
 * other scanners.
 */
class StorageAnalyzerScanner {

    suspend fun scan(root: DocumentFile, ignoredFolders: Set<String> = emptySet()): List<CategoryBreakdown> =
        withContext(Dispatchers.IO) {
            val counts = mutableMapOf<FileCategory, Int>()
            val sizes = mutableMapOf<FileCategory, Long>()
            walk(root, ignoredFolders, counts, sizes)

            FileCategory.values()
                .map { category ->
                    CategoryBreakdown(
                        category = category,
                        fileCount = counts[category] ?: 0,
                        totalSizeBytes = sizes[category] ?: 0L
                    )
                }
                .filter { it.fileCount > 0 }
                .sortedByDescending { it.totalSizeBytes }
        }

    private suspend fun walk(
        folder: DocumentFile,
        ignoredFolders: Set<String>,
        counts: MutableMap<FileCategory, Int>,
        sizes: MutableMap<FileCategory, Long>
    ) {
        currentCoroutineContext().ensureActive()
        for (child in folder.listFiles()) {
            currentCoroutineContext().ensureActive()
            val name = child.name ?: continue

            if (child.isDirectory) {
                if (FolderSkipRules.shouldSkip(name, ignoredFolders)) continue
                walk(child, ignoredFolders, counts, sizes)
                continue
            }

            val category = FileCategory.fromExtension(FileUtils.extensionOf(name))
            counts[category] = (counts[category] ?: 0) + 1
            sizes[category] = (sizes[category] ?: 0L) + child.length()
        }
    }
}
