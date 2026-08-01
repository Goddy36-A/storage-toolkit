package com.goddy.storagetoolkit.scanner

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.goddy.storagetoolkit.models.DuplicateGroup
import com.goddy.storagetoolkit.models.FileCategory
import com.goddy.storagetoolkit.models.FileItem
import com.goddy.storagetoolkit.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Finds duplicate files across a folder tree by content hash (SHA-256), not just
 * name/size. Skips hidden folders and "Android", same as the other scanners, and
 * skips zero-byte files (every empty file would hash identically and "duplicate"
 * with every other empty file, which isn't useful here -- that's what the
 * Zero-byte Scanner is for).
 *
 * Two-pass for performance: files are first grouped by size (cheap, no I/O), and
 * only files that share a size with at least one other file get actually hashed.
 * A file with a unique size in the whole tree can't have a duplicate, so there's
 * no reason to read its bytes.
 */
class DuplicateScanner(private val context: Context) {

    private val skippedFolderNames = setOf("Android")

    suspend fun scan(root: DocumentFile): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        val allFiles = mutableListOf<Pair<DocumentFile, String>>()
        collect(root, "", allFiles)

        val sizeCandidates = allFiles
            .filter { it.first.length() > 0L }
            .groupBy { it.first.length() }
            .values
            .filter { it.size > 1 }
            .flatten()

        val hashGroups = mutableMapOf<String, MutableList<FileItem>>()
        for ((doc, relativePath) in sizeCandidates) {
            currentCoroutineContext().ensureActive()
            val hash = hashFile(doc) ?: continue
            val name = doc.name ?: continue
            val extension = FileUtils.extensionOf(name)
            val item = FileItem(
                documentId = doc.uri.toString(),
                uriString = doc.uri.toString(),
                name = name,
                extension = extension,
                sizeBytes = doc.length(),
                lastModified = doc.lastModified(),
                category = FileCategory.fromExtension(extension),
                relativePath = relativePath
            )
            hashGroups.getOrPut(hash) { mutableListOf() }.add(item)
        }

        hashGroups.entries
            .filter { it.value.size > 1 }
            .map { (hash, files) -> DuplicateGroup(hash = hash, files = files.sortedBy { it.lastModified }) }
    }

    private suspend fun collect(
        folder: DocumentFile,
        relativePath: String,
        results: MutableList<Pair<DocumentFile, String>>
    ) {
        currentCoroutineContext().ensureActive()
        for (child in folder.listFiles()) {
            currentCoroutineContext().ensureActive()
            val name = child.name ?: continue

            if (child.isDirectory) {
                if (name.startsWith(".") || name in skippedFolderNames) continue
                val childPath = if (relativePath.isEmpty()) name else "$relativePath/$name"
                collect(child, childPath, results)
                continue
            }

            results += child to relativePath
        }
    }

    private fun hashFile(doc: DocumentFile): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            context.contentResolver.openInputStream(doc.uri)?.use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }
}
