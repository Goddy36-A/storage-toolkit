package com.goddy.storagetoolkit.utils

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.text.DateFormat
import java.util.Date
import kotlin.math.ln
import kotlin.math.pow

object FileUtils {

    /** Formats a byte count into a human-readable string, e.g. "1.24 GB". */
    fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return String.format("%.2f %s", value, units[digitGroups])
    }

    fun formatDate(millis: Long): String {
        if (millis <= 0) return "Unknown"
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))
    }

    fun extensionOf(fileName: String): String {
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex >= 0 && dotIndex < fileName.length - 1) fileName.substring(dotIndex + 1) else ""
    }

    /**
     * Reconstructs a [DocumentFile] from a stored URI string for delete/move operations.
     * `file://` URIs (from [StorageRoots], i.e. All Files Access) need [DocumentFile.fromFile]
     * — [DocumentFile.fromSingleUri] only works for `content://` tree-document URIs and
     * silently fails (or throws) on a plain file path.
     */
    fun resolveDocumentFile(context: Context, uriString: String): DocumentFile? {
        val uri = Uri.parse(uriString)
        return if (uri.scheme == "file") {
            uri.path?.let { DocumentFile.fromFile(File(it)) }
        } else {
            DocumentFile.fromSingleUri(context, uri)
        }
    }
}

