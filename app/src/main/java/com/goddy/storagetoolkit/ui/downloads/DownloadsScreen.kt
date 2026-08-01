package com.goddy.storagetoolkit.ui.downloads

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.goddy.storagetoolkit.ui.common.FolderAccessPrompt
import com.goddy.storagetoolkit.utils.FileUtils
import com.goddy.storagetoolkit.viewmodel.DownloadsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(viewModel: DownloadsViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri -> uri?.let { viewModel.onFolderGranted(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads Organizer") },
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
                        message = "Grant access to your Downloads folder to categorize and organize its files.",
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
                        Text("Scanning folder...", modifier = Modifier.padding(top = 16.dp))
                    }
                }
                else -> {
                    val totalFiles = uiState.filesByCategory.values.sumOf { it.size }
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("$totalFiles files found", style = MaterialTheme.typography.titleMedium)
                        uiState.statusMessage?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                        }
                        if (uiState.isOrganizing) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                        }
                        Button(
                            onClick = { viewModel.organizeAll() },
                            enabled = totalFiles > 0 && !uiState.isOrganizing,
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                        ) {
                            Text("Organize All Into Folders")
                        }
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        uiState.filesByCategory.entries.sortedByDescending { it.value.size }.forEach { (category, files) ->
                            item {
                                val totalSize = files.sumOf { it.sizeBytes }
                                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(category.label, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                "${files.size} files",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(FileUtils.formatSize(totalSize), style = MaterialTheme.typography.titleMedium)
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
