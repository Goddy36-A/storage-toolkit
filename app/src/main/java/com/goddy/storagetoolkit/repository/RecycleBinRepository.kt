package com.goddy.storagetoolkit.repository

import android.content.Context
import android.net.Uri
import com.goddy.storagetoolkit.data.database.dao.RecycleBinDao
import com.goddy.storagetoolkit.data.database.entities.RecycleBinEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Central place every scanner's delete flow now goes through, instead of calling
 * DocumentFile.delete() directly. A "delete" moves the real file into this app's
 * private external-files recycle bin folder and records where it came from, so it
 * can be restored later or auto-purged after the user's configured retention period.
 *
 * Files land in getExternalFilesDir(null)/recycle_bin -- under Android/data/<package>/,
 * which every scanner already skips by name ("Android"), so recycled files can't get
 * re-discovered by their own source scanner.
 */
class RecycleBinRepository(
    private val context: Context,
    private val dao: RecycleBinDao
) {
    private val binDir: File
        get() = File(context.getExternalFilesDir(null), "recycle_bin").apply { if (!exists()) mkdirs() }

    val itemsFlow: Flow<List<RecycleBinEntity>> = dao.observeAll()

    /** Moves a file into the recycle bin. Returns true if the file was actually moved. */
    suspend fun moveToBin(uriString: String, originalName: String, sizeBytes: Long, sourceFeature: String): Boolean =
        withContext(Dispatchers.IO) {
            val sourcePath = Uri.parse(uriString).path ?: return@withContext false
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists()) return@withContext false

            // Prefix with a timestamp so two files named the same thing from different
            // folders don't collide once they're both sitting in the flat bin folder.
            val binFile = File(binDir, "${System.currentTimeMillis()}_$originalName")

            val moved = try {
                sourceFile.renameTo(binFile) || run {
                    sourceFile.copyTo(binFile, overwrite = true)
                    sourceFile.delete()
                }
            } catch (e: Exception) {
                false
            }

            if (moved) {
                dao.insert(
                    RecycleBinEntity(
                        originalName = originalName,
                        originalPath = sourcePath,
                        binPath = binFile.absolutePath,
                        sizeBytes = sizeBytes,
                        sourceFeature = sourceFeature,
                        deletedAt = System.currentTimeMillis()
                    )
                )
            }
            moved
        }

    /** Moves a file back to where it came from. Fails gracefully if the original folder is gone. */
    suspend fun restore(entry: RecycleBinEntity): Boolean = withContext(Dispatchers.IO) {
        val binFile = File(entry.binPath)
        if (!binFile.exists()) {
            dao.delete(entry)
            return@withContext false
        }

        val originalFile = File(entry.originalPath)
        originalFile.parentFile?.let { if (!it.exists()) it.mkdirs() }

        val restored = try {
            binFile.renameTo(originalFile) || run {
                binFile.copyTo(originalFile, overwrite = true)
                binFile.delete()
            }
        } catch (e: Exception) {
            false
        }

        if (restored) dao.delete(entry)
        restored
    }

    /** Permanently deletes a single recycle bin entry -- no further undo past this point. */
    suspend fun permanentlyDelete(entry: RecycleBinEntity) = withContext(Dispatchers.IO) {
        File(entry.binPath).delete()
        dao.delete(entry)
    }

    /** Removes anything older than [retentionDays]. Called opportunistically when the Recycle Bin screen opens. */
    suspend fun purgeOlderThan(retentionDays: Int) = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionDays.toLong())
        for (entry in dao.findOlderThan(cutoff)) {
            File(entry.binPath).delete()
            dao.delete(entry)
        }
    }
}
