package com.goddy.storagetoolkit.repository

import androidx.documentfile.provider.DocumentFile
import com.goddy.storagetoolkit.models.CategoryBreakdown
import com.goddy.storagetoolkit.scanner.StorageAnalyzerScanner

class StorageAnalyzerRepository(private val scanner: StorageAnalyzerScanner) {

    suspend fun scan(root: DocumentFile, ignoredFolders: Set<String> = emptySet()): List<CategoryBreakdown> =
        scanner.scan(root, ignoredFolders)
}
