package com.goddy.storagetoolkit.ui.apkmanager

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
import androidx.compose.ui.unit.dp
import com.goddy.storagetoolkit.ui.common.FolderAccessPrompt
import com.goddy.storagetoolkit.utils.FileUtils
import com.goddy.storagetoolkit.viewmodel.ApkManagerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkManagerScreen(viewModel: ApkManagerViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri -> uri?.let { viewModel.onFolderGranted(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("APK Manager") },
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
                        message = "Grant access to a folder containing APK installers to manage them.",
                        onRequestAccess = { folderPicker.launch(null) }
                    )
                }
                uiState.isScanning -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Text("Scanning for APKs...", modifier = Modifier.padding(top = 16.dp))
                    }
                }
                uiState.apkFiles.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("No APK files found in this folder.")
                    }
                }
                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${uiState.selectedIds.size} of ${uiState.apkFiles.size} selected")
                        Row {
                            TextButton(onClick = {
                                if (uiState.selectedIds.size == uiState.apkFiles.size) viewModel.clearSelection()
                                else viewModel.selectAll()
                            }) {
                                Text(if (uiState.selectedIds.size == uiState.apkFiles.size) "Clear" else "Select All")
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
                        items(uiState.apkFiles) { apk ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = apk.documentId in uiState.selectedIds,
                                        onCheckedChange = { viewModel.toggleSelection(apk.documentId) }
                                    )
                                    Column(modifier = Modifier.padding(start = 8.dp)) {
                                        Text(apk.appLabel ?: apk.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "v${apk.versionName ?: "unknown"} • ${FileUtils.formatSize(apk.sizeBytes)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "Modified: ${FileUtils.formatDate(apk.installedDate)}",
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
