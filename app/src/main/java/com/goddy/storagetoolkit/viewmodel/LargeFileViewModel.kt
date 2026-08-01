package com.goddy.storagetoolkit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goddy.storagetoolkit.models.FileItem
import com.goddy.storagetoolkit.repository.LargeFileRepository
import com.goddy.storagetoolkit.repository.ScanHistoryRepository
import com.goddy.storagetoolkit.repository.ScanType
import com.goddy.storagetoolkit.utils.StorageAccessManager
import com.goddy.storagetoolkit.utils.StorageRoots
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SizeFilter(val label: String, val minBytes: Long) {
    MB_100("100 MB+", 100L * 1024 * 1024),
    MB_500("500 MB+", 500L * 1024 * 1024),
    GB_1("1 GB+", 1024L * 1024 * 1024)
}

enum class LargeFileSortOrder { SIZE, NAME, DATE }

data class LargeFileUiState(
    val hasFolderAccess: Boolean = false,
    val isScanning: Boolean = false,
    val files: List<FileItem> = emptyList(),
    val filter: SizeFilter = SizeFilter.MB_100,
    val sortOrder: LargeFileSortOrder = LargeFileSortOrder.SIZE,
    val selectedIds: Set<String> = emptySet(),
    val previewFile: FileItem? = null,
    val statusMessage: String? = null
)

class LargeFileViewModel(
    private val storageAccessManager: StorageAccessManager,
    private val largeFileRepository: LargeFileRepository,
    private val scanHistoryRepository: ScanHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LargeFileUiState())
    val uiState: StateFlow<LargeFileUiState> = _uiState.asStateFlow()

    val needsLegacyPermission: Boolean get() = storageAccessManager.needsLegacyFlow
    val legacyPermission: String get() = storageAccessManager.legacyPermission
    fun requestIntent() = storageAccessManager.buildRequestIntent()

    private var scanJob: Job? = null
    private var rawFiles: List<FileItem> = emptyList()

    init {
        viewModelScope.launch {
            storageAccessManager.grantedFlow.collect { granted ->
                _uiState.value = _uiState.value.copy(hasFolderAccess = granted)
                if (granted) scan()
            }
        }
    }

    fun refreshAccess() {
        storageAccessManager.refresh()
    }

    fun scan() {
        if (!_uiState.value.hasFolderAccess) return
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, statusMessage = null)
            rawFiles = largeFileRepository.scan(StorageRoots.primary())
            scanHistoryRepository.record(
                scanType = ScanType.LARGE_FILES,
                filesFound = rawFiles.size,
                filesRemoved = 0,
                spaceSavedBytes = 0
            )
            applyFilterAndSort()
            _uiState.value = _uiState.value.copy(isScanning = false, selectedIds = emptySet())
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        _uiState.value = _uiState.value.copy(isScanning = false)
    }

    fun setFilter(filter: SizeFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
        applyFilterAndSort()
    }

    fun setSortOrder(order: LargeFileSortOrder) {
        _uiState.value = _uiState.value.copy(sortOrder = order)
        applyFilterAndSort()
    }

    private fun applyFilterAndSort() {
        val filter = _uiState.value.filter
        val order = _uiState.value.sortOrder
        val filtered = rawFiles.filter { it.sizeBytes >= filter.minBytes }
        val sorted = when (order) {
            LargeFileSortOrder.SIZE -> filtered.sortedByDescending { it.sizeBytes }
            LargeFileSortOrder.NAME -> filtered.sortedBy { it.name.lowercase() }
            LargeFileSortOrder.DATE -> filtered.sortedByDescending { it.lastModified }
        }
        _uiState.value = _uiState.value.copy(files = sorted)
    }

    fun showPreview(file: FileItem) {
        _uiState.value = _uiState.value.copy(previewFile = file)
    }

    fun dismissPreview() {
        _uiState.value = _uiState.value.copy(previewFile = null)
    }

    fun toggleSelection(documentId: String) {
        val current = _uiState.value.selectedIds
        val updated = if (documentId in current) current - documentId else current + documentId
        _uiState.value = _uiState.value.copy(selectedIds = updated)
    }

    fun selectAll() {
        _uiState.value = _uiState.value.copy(selectedIds = _uiState.value.files.map { it.documentId }.toSet())
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedIds = emptySet())
    }

    /** Deletes just the file currently shown in the preview dialog, then closes it. */
    fun deletePreviewedFile() {
        val file = _uiState.value.previewFile ?: return
        _uiState.value = _uiState.value.copy(previewFile = null)
        deleteFiles(listOf(file))
    }

    fun deleteSelected() {
        val selected = _uiState.value.files.filter { it.documentId in _uiState.value.selectedIds }
        deleteFiles(selected)
    }

    private fun deleteFiles(files: List<FileItem>) {
        if (files.isEmpty()) return
        viewModelScope.launch {
            val spaceSaved = files.sumOf { it.sizeBytes }
            val deletedCount = largeFileRepository.delete(files)
            scanHistoryRepository.record(
                scanType = ScanType.LARGE_FILES,
                filesFound = _uiState.value.files.size,
                filesRemoved = deletedCount,
                spaceSavedBytes = spaceSaved
            )
            _uiState.value = _uiState.value.copy(statusMessage = "Deleted $deletedCount file(s)")
            scan()
        }
    }
}
