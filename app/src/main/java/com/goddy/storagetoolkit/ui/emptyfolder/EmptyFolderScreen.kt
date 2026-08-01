package com.goddy.storagetoolkit.ui.emptyfolder

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.goddy.storagetoolkit.ui.common.FolderAccessPrompt
import com.goddy.storagetoolkit.ui.common.OnResumeEffect
import com.goddy.storagetoolkit.utils.FileUtils
import com.goddy.storagetoolkit.viewmodel.EmptyFolderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmptyFolderScreen(viewModel: EmptyFolderViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { viewModel.refreshAccess() }

    OnResumeEffect { viewModel.refreshAccess() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Empty Folders") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.hasFolderAccess) {
                        IconButton(onClick = { viewModel.scan() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Rescan")
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
                        message = "Grant storage access to scan your whole device for empty subfolders, including nested ones.",
                        onRequestAccess = {
                            if (viewModel.needsLegacyPermission) {
                                legacyPermissionLauncher.launch(viewModel.legacyPermission)
                            } else {
                                context.startActivity(viewModel.requestIntent())
                            }
                        }
                    )
                }
                uiState.isScanning -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Text("Scanning for empty folders...", modifier = Modifier.padding(top = 16.dp))
                        TextButton(onClick = { viewModel.cancelScan() }, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Cancel")
                        }
                    }
                }
                uiState.folders.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FolderOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "No empty folders found.",
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${uiState.selectedIds.size} of ${uiState.folders.size} selected")
                        Row {
                            TextButton(onClick = {
                                if (uiState.selectedIds.size == uiState.folders.size) viewModel.clearSelection()
                                else viewModel.selectAll()
                            }) {
                                Text(if (uiState.selectedIds.size == uiState.folders.size) "Clear" else "Select All")
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
                        items(uiState.folders) { folder ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = folder.documentId in uiState.selectedIds,
                                        onCheckedChange = { viewModel.toggleSelection(folder.documentId) }
                                    )
                                    Column(modifier = Modifier.padding(start = 8.dp)) {
                                        Text(folder.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = folder.relativePath.ifEmpty { "In root folder" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Modified: ${FileUtils.formatDate(folder.lastModified)}",
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
