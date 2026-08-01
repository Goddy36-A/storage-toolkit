package com.goddy.storagetoolkit.scanner

import android.content.Context
import android.content.pm.PackageManager
import androidx.documentfile.provider.DocumentFile
import com.goddy.storagetoolkit.models.ApkFileInfo
import com.goddy.storagetoolkit.utils.FolderSkipRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Recursively finds .apk files across a folder tree and reads their install metadata.
 * Skips hidden folders, "Android", and the user's Settings ignore list, same as the
 * other scanners.
 *
 * PackageManager.getPackageArchiveInfo requires a real filesystem path. That's always
 * true now (All Files Access gives file:// paths directly), but we still copy into the
 * app's private cache first rather than pass the original path — getPackageArchiveInfo
 * can leave the archive briefly locked/cached by the system, and we don't want that
 * touching the user's original file.
 */
class ApkScanner(private val context: Context) {

    suspend fun scan(root: DocumentFile, ignoredFolders: Set<String> = emptySet()): List<ApkFileInfo> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<ApkFileInfo>()
            walk(root, results, ignoredFolders)
            results
        }

    private suspend fun walk(folder: DocumentFile, results: MutableList<ApkFileInfo>, ignoredFolders: Set<String>) {
        currentCoroutineContext().ensureActive()
        val pm = context.packageManager

        for (child in folder.listFiles()) {
            currentCoroutineContext().ensureActive()
            val name = child.name ?: continue

            if (child.isDirectory) {
                if (FolderSkipRules.shouldSkip(name, ignoredFolders)) continue
                walk(child, results, ignoredFolders)
                continue
            }

            if (!name.endsWith(".apk", ignoreCase = true)) continue

            var versionName: String? = null
            var appLabel: String? = null
            val tempFile = File(context.cacheDir, "scan_${child.uri.toString().hashCode()}.apk")
            try {
                context.contentResolver.openInputStream(child.uri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }
                val packageInfo = pm.getPackageArchiveInfo(tempFile.absolutePath, 0)
                if (packageInfo != null) {
                    versionName = packageInfo.versionName
                    packageInfo.applicationInfo?.let { appInfo ->
                        appInfo.sourceDir = tempFile.absolutePath
                        appInfo.publicSourceDir = tempFile.absolutePath
                        appLabel = pm.getApplicationLabel(appInfo).toString()
                    }
                }
            } catch (e: Exception) {
                // Unreadable or corrupt APK — still list it, just without metadata.
            } finally {
                tempFile.delete()
            }

            results += ApkFileInfo(
                documentId = child.uri.toString(),
                uriString = child.uri.toString(),
                name = name,
                sizeBytes = child.length(),
                installedDate = child.lastModified(),
                versionName = versionName,
                appLabel = appLabel
            )
        }
    }
}
