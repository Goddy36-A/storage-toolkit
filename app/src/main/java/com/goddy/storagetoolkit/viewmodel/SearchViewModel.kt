package com.goddy.storagetoolkit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goddy.storagetoolkit.data.datastore.SettingsManager
import com.goddy.storagetoolkit.models.FileItem
import com.goddy.storagetoolkit.repository.SearchRepository
import com.goddy.storagetoolkit.utils.StorageAccessManager
import com.goddy.storagetoolkit.utils.StorageRoots
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

enum class SearchField(val label: String) {
    ALL("All"),
    NAME("Filename"),
    EXTENSION("Extension"),
    FOLDER("Folder")
}

private const val MAX_DISPLAYED_RESULTS = 300

data class SearchUiState(
    val hasFolderAccess: Boolean = false,
    val isIndexing: Boolean = false,
    val indexedCount: Int = 0,
    val indexBuiltAt: Long? = null,
    val query: String = "",
    val field: SearchField = SearchField.ALL,
    val results: List<FileItem> = emptyList(),
    val totalMatches: Int = 0,
    val selectedIds: Set<String> = emptySet(),
    val statusMessage: String? = null
)

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val storageAccessManager: StorageAccessManager,
    private val searchRepository: SearchRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val needsLegacyPermission: Boolean get() = storageAccessManager.needsLegacyFlow
    val legacyPermission: String get() = storageAccessManager.legacyPermission
    fun requestIntent() = storageAccessManager.buildRequestIntent()

    private val queryFlow = MutableStateFlow("")
    private val fieldFlow = MutableStateFlow(SearchField.ALL)

    private var rawIndex: List<FileItem> = emptyList()
    private var indexJob: Job? = null

    init {
        viewModelScope.launch {
            storageAccessManager.grantedFlow.collect { granted ->
                _uiState.value = _uiState.value.copy(hasFolderAccess = granted)
                if (granted && rawIndex.isEmpty()) buildIndex()
            }
        }
        viewModelScope.launch {
            combine(queryFlow.debounce(250), fieldFlow) { q, f -> q to f }
                .collect { (query, field) -> applyFilter(query, field) }
        }
    }

    fun refreshAccess() {
        storageAccessManager.refresh()
    }

    fun buildIndex() {
        if (!_uiState.value.hasFolderAccess) return
        indexJob?.cancel()
        indexJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isIndexing = true, statusMessage = null)
            val ignored = settingsManager.currentIgnoredFolders()
            rawIndex = searchRepository.buildIndex(StorageRoots.primary(), ignored)
            _uiState.value = _uiState.value.copy(
                isIndexing = false,
                indexedCount = rawIndex.size,
                indexBuiltAt = System.currentTimeMillis()
            )
            applyFilter(queryFlow.value, fieldFlow.value)
        }
    }

    fun cancelIndexing() {
        indexJob?.cancel()
        _uiState.value = _uiState.value.copy(isIndexing = false)
    }

    fun setQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        queryFlow.value = query
    }

    fun setField(field: SearchField) {
        _uiState.value = _uiState.value.copy(field = field)
        fieldFlow.value = field
    }

    private fun applyFilter(query: String, field: SearchField) {
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(results = emptyList(), totalMatches = 0, selectedIds = emptySet())
            return
        }
        val needle = query.trim().removePrefix(".").lowercase()

        val matches = rawIndex.filter { item ->
            when (field) {
                SearchField.ALL ->
                    item.name.lowercase().contains(needle) ||
                        item.extension.lowercase().contains(needle) ||
                        item.relativePath.lowercase().contains(needle)
                SearchField.NAME -> item.name.lowercase().contains(needle)
                SearchField.EXTENSION -> item.extension.lowercase() == needle || item.extension.lowercase().contains(needle)
                SearchField.FOLDER -> item.relativePath.lowercase().contains(needle)
            }
        }.sortedBy { it.name.lowercase() }

        _uiState.value = _uiState.value.copy(
            results = matches.take(MAX_DISPLAYED_RESULTS),
            totalMatches = matches.size,
            selectedIds = emptySet()
        )
    }

    fun toggleSelection(documentId: String) {
        val current = _uiState.value.selectedIds
        val updated = if (documentId in current) current - documentId else current + documentId
        _uiState.value = _uiState.value.copy(selectedIds = updated)
    }

    fun selectAll() {
        _uiState.value = _uiState.value.copy(selectedIds = _uiState.value.results.map { it.documentId }.toSet())
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedIds = emptySet())
    }

    fun deleteSelected() {
        val selected = _uiState.value.results.filter { it.documentId in _uiState.value.selectedIds }
        if (selected.isEmpty()) return

        viewModelScope.launch {
            val deletedCount = searchRepository.delete(selected)
            // Keep the in-memory index consistent so deleted files disappear from
            // results immediately without a full rebuild.
            val deletedUris = selected.map { it.uriString }.toSet()
            rawIndex = rawIndex.filterNot { it.uriString in deletedUris }
            _uiState.value = _uiState.value.copy(
                statusMessage = "Deleted $deletedCount file(s)",
                indexedCount = rawIndex.size
            )
            applyFilter(queryFlow.value, fieldFlow.value)
        }
    }
}
