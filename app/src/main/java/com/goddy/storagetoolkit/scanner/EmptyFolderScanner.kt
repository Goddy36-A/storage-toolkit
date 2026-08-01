package com.goddy.storagetoolkit.scanner

import androidx.documentfile.provider.DocumentFile
import com.goddy.storagetoolkit.models.FolderItem
import com.goddy.storagetoolkit.utils.FolderSkipRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Recursively finds directories that contain no real files anywhere in their subtree
 * (a folder full of only other empty folders still counts as empty). Skips hidden
 * folders, "Android", and the user's Settings ignore list, same as [ZeroByteScanner].
 *
 * Only the outermost empty folder in any given branch is reported — if "A/B" is
 * empty and "A" is also empty as a result, only "A" is listed, since deleting it
 * removes "B" along with it. If "A" contains other real content alongside an
 * empty "B", then "B" alone is reported as a reclaimable pocket.
 */
class EmptyFolderScanner {

    suspend fun scan(root: DocumentFile, ignoredFolders: Set<String> = emptySet()): List<FolderItem> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<FolderItem>()
            walk(root, "", results, ignoredFolders)
            results
        }

    /** Returns true if [folder]'s entire subtree (after skipping ignored names) has no real files. */
    private suspend fun walk(
        folder: DocumentFile,
        relativePath: String,
        results: MutableList<FolderItem>,
        ignoredFolders: Set<String>
    ): Boolean {
        currentCoroutineContext().ensureActive()

        val children = folder.listFiles().filter { child ->
            val name = child.name
            !(name == null || (child.isDirectory && FolderSkipRules.shouldSkip(name, ignoredFolders)))
        }

        if (children.isEmpty()) return true

        var allEmpty = true
        val emptyChildFolders = mutableListOf<Pair<DocumentFile, String>>()

        for (child in children) {
            currentCoroutineContext().ensureActive()
            val name = child.name ?: continue

            if (child.isDirectory) {
                val childPath = if (relativePath.isEmpty()) name else "$relativePath/$name"
                val childEmpty = walk(child, childPath, results, ignoredFolders)
                if (childEmpty) emptyChildFolders += child to childPath else allEmpty = false
            } else {
                allEmpty = false
            }
        }

        if (allEmpty) {
            // Let the parent decide whether to report this folder — don't add it yet,
            // otherwise a fully-empty tree would list every level instead of just the top.
            return true
        }

        for ((child, path) in emptyChildFolders) {
            results += FolderItem(
                documentId = child.uri.toString(),
                uriString = child.uri.toString(),
                name = child.name ?: path,
                relativePath = path,
                lastModified = child.lastModified()
            )
        }
        return false
    }
}
