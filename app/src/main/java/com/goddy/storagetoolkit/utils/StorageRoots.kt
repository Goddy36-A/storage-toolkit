package com.goddy.storagetoolkit.utils

import android.os.Environment
import androidx.documentfile.provider.DocumentFile

/**
 * With All Files Access granted, every scanner reads real filesystem paths directly —
 * no SAF tree URIs, no per-feature folder picking. [DocumentFile.fromFile] wraps a
 * plain [java.io.File] so the existing scanner/repository code (written against the
 * DocumentFile API) keeps working unchanged; only where the root comes from changes.
 */
object StorageRoots {

    /** The whole accessible shared-storage volume — used by scanners that should cover everything. */
    fun primary(): DocumentFile = DocumentFile.fromFile(Environment.getExternalStorageDirectory())

    /** Just the Downloads folder — used by features that are specifically about Downloads. */
    fun downloads(): DocumentFile = DocumentFile.fromFile(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    )
}
