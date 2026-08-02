package com.goddy.storagetoolkit.ui.scanhistory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.goddy.storagetoolkit.data.database.entities.ScanHistoryEntity
import com.goddy.storagetoolkit.utils.FileUtils
import com.goddy.storagetoolkit.viewmodel.ScanHistoryViewModel

private fun labelFor(scanType: String): String = when (scanType) {
    "downloads_organizer" -> "Downloads Organizer"
    "apk_manager" -> "APK Manager"
    "zero_byte" -> "Zero-byte Files"
    "empty_folder" -> "Empty Folders"
    "duplicate_files" -> "Duplicate Files"
    "large_files" -> "Large Files"
    else -> scanType.replace("_", " ").replaceFirstChar { it.uppercase() }
}

private fun iconFor(scanType: String): ImageVector = when (scanType) {
    "downloads_organizer" -> Icons.Filled.Download
    "apk_manager" -> Icons.Filled.Android
    "zero_byte" -> Icons.Filled.FileOpen
    "empty_folder" -> Icons.Filled.FolderOff
    "duplicate_files" -> Icons.Filled.ContentCopy
    "large_files" -> Icons.Filled.Description
    else -> Icons.Filled.History
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanHistoryScreen(viewModel: ScanHistoryViewModel, onBack: () -> Unit) {
    val history by viewModel.history.collectAsState()
    var confirmClearOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (history.isNotEmpty()) {
                        IconButton(onClick = { confirmClearOpen = true }) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear history")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (history.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.History, contentDescription = null)
                    Text(
                        "No scans yet. Run any scanner and it'll show up here.",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    items(history) { entry -> ScanHistoryRow(entry) }
                }
            }
        }
    }

    if (confirmClearOpen) {
        AlertDialog(
            onDismissRequest = { confirmClearOpen = false },
            title = { Text("Clear scan history?") },
            text = { Text("This removes the log of past scans. It won't undelete or restore any files.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    confirmClearOpen = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearOpen = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ScanHistoryRow(entry: ScanHistoryEntity) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = iconFor(entry.scanType),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(labelFor(entry.scanType), style = MaterialTheme.typography.titleMedium)
                Text(
                    FileUtils.formatDate(entry.scanDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = buildString {
                        append("${entry.filesFound} found")
                        if (entry.filesRemoved > 0) append(" • ${entry.filesRemoved} removed")
                        if (entry.spaceSavedBytes > 0) append(" • ${FileUtils.formatSize(entry.spaceSavedBytes)} saved")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
