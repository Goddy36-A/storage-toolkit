package com.goddy.storagetoolkit.models

/** Categories used to classify files in the Downloads Organizer and Storage Analyzer. */
enum class FileCategory(val label: String) {
    IMAGES("Images"),
    VIDEOS("Videos"),
    AUDIO("Audio"),
    DOCUMENTS("Documents"),
    ARCHIVES("Archives"),
    APKS("APKs"),
    OTHERS("Others");

    companion object {
        private val extensionMap: Map<String, FileCategory> = buildMap {
            listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif").forEach { put(it, IMAGES) }
            listOf("mp4", "mkv", "mov", "avi", "webm", "3gp", "m4v").forEach { put(it, VIDEOS) }
            listOf("mp3", "wav", "ogg", "m4a", "flac", "aac", "opus").forEach { put(it, AUDIO) }
            listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "odt").forEach { put(it, DOCUMENTS) }
            listOf("zip", "rar", "7z", "tar", "gz", "bz2").forEach { put(it, ARCHIVES) }
            put("apk", APKS)
        }

        fun fromExtension(extension: String): FileCategory =
            extensionMap[extension.lowercase()] ?: OTHERS
    }
}

/** A single file discovered during a scan, referenced by its SAF document URI. */
data class FileItem(
    val documentId: String,
    val uriString: String,
    val name: String,
    val extension: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val category: FileCategory,
    val relativePath: String = ""
)

/** An empty directory discovered during a scan, referenced by its SAF document URI. */
data class FolderItem(
    val documentId: String,
    val uriString: String,
    val name: String,
    val relativePath: String,
    val lastModified: Long
)

/** A group of files sharing the same SHA-256 hash. files[0] is the suggested keeper. */
data class DuplicateGroup(
    val hash: String,
    val files: List<FileItem>
) {
    val wastedBytes: Long
        get() = if (files.size <= 1) 0L else files.drop(1).sumOf { it.sizeBytes }
}

/** Per-category totals shown in the Storage Analyzer. */
data class CategoryBreakdown(
    val category: FileCategory,
    val fileCount: Int,
    val totalSizeBytes: Long
)

data class StorageStats(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long
) {
    val usedFraction: Float
        get() = if (totalBytes == 0L) 0f else (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
}

data class ApkFileInfo(
    val documentId: String,
    val uriString: String,
    val name: String,
    val sizeBytes: Long,
    val installedDate: Long,
    val versionName: String?,
    val appLabel: String?
)

/** Aggregated stats shown on a Home Dashboard quick-action card. */
data class CategoryStats(
    val fileCount: Int,
    val totalSizeBytes: Long,
    val lastScanTime: Long?
)
