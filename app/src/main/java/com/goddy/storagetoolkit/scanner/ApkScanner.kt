package com.goddy.storagetoolkit.scanner

import android.content.Context
import android.content.pm.PackageManager
import androidx.documentfile.provider.DocumentFile
import com.goddy.storagetoolkit.models.ApkFileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Finds .apk files in a SAF-granted folder and reads install metadata.
 *
 * PackageManager.getPackageArchiveInfo requires a real filesystem path, but SAF only
 * gives us content:// URIs. We copy each APK into the app's private cache directory
 * (no extra permission needed) just long enough to read its metadata, then delete it.
 */
class ApkScanner(private val context: Context) {

    suspend fun scan(root: DocumentFile): List<ApkFileInfo> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ApkFileInfo>()
        val pm = context.packageManager

        for (child in root.listFiles()) {
            currentCoroutineContext().ensureActive()
            if (child.isDirectory) continue
            val name = child.name ?: continue
            if (!name.endsWith(".apk", ignoreCase = true)) continue

            var versionName: String? = null
            var appLabel: String? = null
            val tempFile = File(context.cacheDir, "scan_${child.uri.lastPathSegment?.hashCode()}.apk")
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
                documentId = child.uri.lastPathSegment ?: name,
                uriString = child.uri.toString(),
                name = name,
                sizeBytes = child.length(),
                installedDate = child.lastModified(),
                versionName = versionName,
                appLabel = appLabel
            )
        }
        results
    }
}
