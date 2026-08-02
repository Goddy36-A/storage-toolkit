package com.goddy.storagetoolkit.ui.search

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.goddy.storagetoolkit.ui.common.FolderAccessPrompt
import com.goddy.storagetoolkit.ui.common.OnResumeEffect
import com.goddy.storagetoolkit.utils.FileUtils
import com.goddy.storagetoolkit.viewmodel.SearchField
import com.goddy.storagetoolkit.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: SearchViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { viewModel.refreshAccess() }

    OnResumeEffect { viewModel.refreshAccess() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.hasFolderAccess) {
                        IconButton(onClick = { viewModel.buildIndex() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Rebuild index")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                !uiState.hasFolderAccess -> {
                    FolderAccessPrompt(
                        message = "Grant storage access to search filenames, extensions, and folders across your device.",
                        onRequestAccess = {
                            if (viewModel.needsLegacyPermission) {
                                legacyPermissionLauncher.launch(viewModel.legacyPermission)
                            } else {
                                context.startActivity(viewModel.requestIntent())
                            }
                        }
                    )
                }
                uiState.isIndexing -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Text("Building search index...", modifier = Modifier.padding(top = 16.dp))
                        TextButton(onClick = { viewModel.cancelIndexing() }, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Cancel")
                        }
                    }
                }
                else -> {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = uiState.query,
                            onValueChange = { viewModel.setQuery(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search files...") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            trailingIcon = {
                                if (uiState.query.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setQuery("") }) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SearchField.values().forEach { field ->
                                FilterChip(
                                    selected = uiState.field == field,
                                    onClick = { viewModel.setField(field) },
                                    label = { Text(field.label) }
                                )
                            }
                        }

                        Text(
                            text = "Indexed ${uiState.indexedCount} files" +
                                (uiState.indexBuiltAt?.let { " • ${FileUtils.formatDate(it)}" } ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }

                    when {
                        uiState.query.isBlank() -> {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("Start typing to search by filename, extension, or folder.")
                            }
                        }
                        uiState.results.isEmpty() -> {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("No matches for \"${uiState.query}\".")
                            }
                        }
                        else -> {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val countLabel = if (uiState.totalMatches > uiState.results.size) {
                                    "Showing ${uiState.results.size} of ${uiState.totalMatches} matches"
                                } else {
                                    "${uiState.results.size} matches"
                                }
                                Text(countLabel)
                                Row {
                                    TextButton(onClick = {
                                        if (uiState.selectedIds.size == uiState.results.size) viewModel.clearSelection()
                                        else viewModel.selectAll()
                                    }) {
                                        Text(if (uiState.selectedIds.size == uiState.results.size) "Clear" else "Select All")
                                    }
                                    Button(
                                        onClick = { viewModel.deleteSelected() },
                                        enabled = uiState.selectedIds.isNotEmpty()
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                                        Text("Delete")
                                    }
                                }
                            }

                            uiState.statusMessage?.let {
                                Text(it, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                            }

                            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                                items(uiState.results) { file ->
                                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = file.documentId in uiState.selectedIds,
                                                onCheckedChange = { viewModel.toggleSelection(file.documentId) }
                                            )
                                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                                Text(file.name, style = MaterialTheme.typography.titleMedium)
                                                Text(
                                                    text = if (file.relativePath.isEmpty()) "In root folder" else file.relativePath,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "${FileUtils.formatSize(file.sizeBytes)} • ${FileUtils.formatDate(file.lastModified)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
