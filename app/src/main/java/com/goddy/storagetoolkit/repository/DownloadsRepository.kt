package com.goddy.storagetoolkit.repository

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.goddy.storagetoolkit.models.FileCategory
import com.goddy.storagetoolkit.models.FileItem
import com.goddy.storagetoolkit.scanner.DownloadsScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DownloadsRepository(
    private val scanner: DownloadsScanner
) {

    suspend fun scan(root: DocumentFile): List<FileItem> = scanner.scan(root)

    /**
     * Moves each given file into a same-named subfolder of [root] based on its category,
     * e.g. Downloads/Images, Downloads/Documents. With All Files Access, everything here
     * is a real filesystem path, so this is a plain File move -- File.renameTo first
     * (instant, same volume), falling back to copy+delete only if that fails (e.g. a
     * cross-filesystem move, which renameTo can't do).
     */
    suspend fun organize(root: DocumentFile, files: List<FileItem>): Int = withContext(Dispatchers.IO) {
        var movedCount = 0
        val rootPath = Uri.parse(root.uri.toString()).path ?: return@withContext 0
        val folderCache = mutableMapOf<FileCategory, File>()

        for (item in files) {
            val targetDir = folderCache.getOrPut(item.category) {
                File(rootPath, item.category.label).apply { if (!exists()) mkdirs() }
            }

            val sourcePath = Uri.parse(item.uriString).path ?: continue
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists()) continue

            val targetFile = File(targetDir, sourceFile.name)
            val moved = try {
                sourceFile.renameTo(targetFile) || run {
                    sourceFile.copyTo(targetFile, overwrite = true)
                    sourceFile.delete()
                }
            } catch (e: Exception) {
                false
            }
            if (moved) movedCount++
        }
        movedCount
    }
}
