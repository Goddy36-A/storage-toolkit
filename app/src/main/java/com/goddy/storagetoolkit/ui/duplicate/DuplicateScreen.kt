package com.goddy.storagetoolkit.ui.duplicate

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.goddy.storagetoolkit.ui.common.FolderAccessPrompt
import com.goddy.storagetoolkit.ui.common.OnResumeEffect
import com.goddy.storagetoolkit.utils.FileUtils
import com.goddy.storagetoolkit.viewmodel.DuplicateSortOrder
import com.goddy.storagetoolkit.viewmodel.DuplicateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateScreen(viewModel: DuplicateViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var sortMenuOpen by remember { mutableStateOf(false) }

    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { viewModel.refreshAccess() }

    OnResumeEffect { viewModel.refreshAccess() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Duplicate Files") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.hasFolderAccess) {
                        Box {
                            IconButton(onClick = { sortMenuOpen = true }) {
                                Icon(Icons.Filled.Sort, contentDescription = "Sort")
                            }
                            DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("Most space wasted") },
                                    onClick = { viewModel.setSortOrder(DuplicateSortOrder.WASTED_SPACE); sortMenuOpen = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Name") },
                                    onClick = { viewModel.setSortOrder(DuplicateSortOrder.NAME); sortMenuOpen = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Date") },
                                    onClick = { viewModel.setSortOrder(DuplicateSortOrder.DATE); sortMenuOpen = false }
                                )
                            }
                        }
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
                        message = "Grant storage access to find duplicate files across your whole device, by content -- not just name.",
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
                        Text("Hashing files to find duplicates...", modifier = Modifier.padding(top = 16.dp))
                        TextButton(onClick = { viewModel.cancelScan() }, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Cancel")
                        }
                    }
                }
                uiState.groups.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("No duplicate files found.")
                    }
                }
                else -> {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "${uiState.groups.size} duplicate group(s) • ${FileUtils.formatSize(uiState.totalWastedBytes)} reclaimable",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = {
                                if (uiState.selectedIds.isEmpty()) viewModel.selectAllExceptKeepers()
                                else viewModel.clearSelection()
                            }) {
                                Text(if (uiState.selectedIds.isEmpty()) "Select duplicates (keep 1 each)" else "Clear selection")
                            }
                            Button(
                                onClick = { viewModel.deleteSelected() },
                                enabled = uiState.selectedIds.isNotEmpty()
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                                Text("Delete (${uiState.selectedIds.size})")
                            }
                        }
                        uiState.statusMessage?.let {
                            Text(it, modifier = Modifier.padding(top = 4.dp))
                        }
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        uiState.groups.forEach { group ->
                            item {
                                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            "${group.files.size} copies • ${FileUtils.formatSize(group.files.first().sizeBytes)} each",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        group.files.forEachIndexed { index, file ->
                                            val isKeeper = index == 0
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (isKeeper) {
                                                    Text(
                                                        "KEEP",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(end = 8.dp)
                                                    )
                                                } else {
                                                    Checkbox(
                                                        checked = file.documentId in uiState.selectedIds,
                                                        onCheckedChange = { viewModel.toggleSelection(file.documentId) }
                                                    )
                                                }
                                                Column {
                                                    Text(file.name, style = MaterialTheme.typography.bodyMedium)
                                                    Text(
                                                        text = (file.relativePath.ifEmpty { "In root folder" }) +
                                                            " • ${FileUtils.formatDate(file.lastModified)}",
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
}
