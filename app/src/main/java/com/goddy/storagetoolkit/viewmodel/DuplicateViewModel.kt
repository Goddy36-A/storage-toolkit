package com.goddy.storagetoolkit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goddy.storagetoolkit.models.DuplicateGroup
import com.goddy.storagetoolkit.repository.DuplicateRepository
import com.goddy.storagetoolkit.repository.ScanHistoryRepository
import com.goddy.storagetoolkit.repository.ScanType
import com.goddy.storagetoolkit.utils.StorageAccessManager
import com.goddy.storagetoolkit.utils.StorageRoots
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class DuplicateSortOrder { WASTED_SPACE, NAME, DATE }

data class DuplicateUiState(
    val hasFolderAccess: Boolean = false,
    val isScanning: Boolean = false,
    val groups: List<DuplicateGroup> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val sortOrder: DuplicateSortOrder = DuplicateSortOrder.WASTED_SPACE,
    val statusMessage: String? = null
) {
    val totalWastedBytes: Long get() = groups.sumOf { it.wastedBytes }
}

/**
 * Each group's files list is sorted oldest-first by the scanner, so files.first()
 * is the suggested "keeper" (the original) in every group -- the rest are what
 * "Select all duplicates (keep 1 each)" pre-selects for deletion.
 */
class DuplicateViewModel(
    private val storageAccessManager: StorageAccessManager,
    private val duplicateRepository: DuplicateRepository,
    private val scanHistoryRepository: ScanHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DuplicateUiState())
    val uiState: StateFlow<DuplicateUiState> = _uiState.asStateFlow()

    val needsLegacyPermission: Boolean get() = storageAccessManager.needsLegacyFlow
    val legacyPermission: String get() = storageAccessManager.legacyPermission
    fun requestIntent() = storageAccessManager.buildRequestIntent()

    private var scanJob: Job? = null
    private var rawGroups: List<DuplicateGroup> = emptyList()

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
            rawGroups = duplicateRepository.scan(StorageRoots.primary())
            val totalDuplicateFiles = rawGroups.sumOf { it.files.size - 1 }
            scanHistoryRepository.record(
                scanType = ScanType.DUPLICATE_FILES,
                filesFound = totalDuplicateFiles,
                filesRemoved = 0,
                spaceSavedBytes = 0
            )
            _uiState.value = _uiState.value.copy(
                isScanning = false,
                groups = sorted(rawGroups, _uiState.value.sortOrder),
                selectedIds = emptySet()
            )
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        _uiState.value = _uiState.value.copy(isScanning = false)
    }

    fun setSortOrder(order: DuplicateSortOrder) {
        _uiState.value = _uiState.value.copy(sortOrder = order, groups = sorted(rawGroups, order))
    }

    private fun sorted(groups: List<DuplicateGroup>, order: DuplicateSortOrder): List<DuplicateGroup> {
        return when (order) {
            DuplicateSortOrder.WASTED_SPACE -> groups.sortedByDescending { it.wastedBytes }
            DuplicateSortOrder.NAME -> groups.sortedBy { it.files.firstOrNull()?.name?.lowercase() ?: "" }
            DuplicateSortOrder.DATE -> groups.sortedByDescending { g -> g.files.maxOfOrNull { it.lastModified } ?: 0L }
        }
    }

    fun toggleSelection(documentId: String) {
        val current = _uiState.value.selectedIds
        val updated = if (documentId in current) current - documentId else current + documentId
        _uiState.value = _uiState.value.copy(selectedIds = updated)
    }

    /** Selects every duplicate in every group except the first (suggested keeper) in each. */
    fun selectAllExceptKeepers() {
        val ids = _uiState.value.groups.flatMap { group -> group.files.drop(1).map { it.documentId } }.toSet()
        _uiState.value = _uiState.value.copy(selectedIds = ids)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedIds = emptySet())
    }

    fun deleteSelected() {
        val selectedIds = _uiState.value.selectedIds
        val selectedFiles = _uiState.value.groups.flatMap { it.files }.filter { it.documentId in selectedIds }
        if (selectedFiles.isEmpty()) return

        viewModelScope.launch {
            val spaceSaved = selectedFiles.sumOf { it.sizeBytes }
            val deletedCount = duplicateRepository.delete(selectedFiles)
            scanHistoryRepository.record(
                scanType = ScanType.DUPLICATE_FILES,
                filesFound = selectedFiles.size,
                filesRemoved = deletedCount,
                spaceSavedBytes = spaceSaved
            )
            _uiState.value = _uiState.value.copy(statusMessage = "Deleted $deletedCount file(s)")
            scan()
        }
    }
}
