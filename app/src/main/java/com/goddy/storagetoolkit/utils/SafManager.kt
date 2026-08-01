package com.goddy.storagetoolkit.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.goddy.storagetoolkit.data.datastore.PreferencesManager
import kotlinx.coroutines.flow.Flow

/**
 * Wraps the Storage Access Framework so the rest of the app never touches raw file paths.
 * The user grants access to a folder (e.g. Downloads) once; we persist that permission
 * across app restarts using [PreferencesManager], as recommended for scoped storage on
 * Android 11+ instead of requesting broad filesystem permissions.
 */
class SafManager(
    private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    val treeUriFlow: Flow<String?> = preferencesManager.safTreeUriFlow

    fun buildOpenTreeIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }
    }

    suspend fun persistTreeUri(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        preferencesManager.setSafTreeUri(uri.toString())
    }

    fun getDocumentTree(uriString: String): DocumentFile? {
        val uri = Uri.parse(uriString)
        return DocumentFile.fromTreeUri(context, uri)
    }
}
