package com.goddy.storagetoolkit.ui.recyclebin

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
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.goddy.storagetoolkit.data.database.entities.RecycleBinEntity
import com.goddy.storagetoolkit.utils.FileUtils
import com.goddy.storagetoolkit.viewmodel.RecycleBinViewModel
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(viewModel: RecycleBinViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recycle Bin") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.items.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Recycle bin is empty.")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${uiState.items.size} item(s) • auto-deletes after ${uiState.autoDeleteDays} day(s)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = { viewModel.emptyBin() }) {
                        Text("Empty Bin")
                    }
                }

                uiState.statusMessage?.let {
                    Text(it, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }

                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(uiState.items, key = { it.id }) { entry ->
                        RecycleBinRow(
                            entry = entry,
                            onRestore = { viewModel.restore(entry) },
                            onDeleteForever = { viewModel.deleteForever(entry) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecycleBinRow(entry: RecycleBinEntity, onRestore: () -> Unit, onDeleteForever: () -> Unit) {
    val daysAgo = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - entry.deletedAt)

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.originalName, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${entry.sourceFeature} • ${FileUtils.formatSize(entry.sizeBytes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (daysAgo <= 0L) "Deleted today" else "Deleted $daysAgo day(s) ago",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRestore) {
                Icon(Icons.Filled.Restore, contentDescription = "Restore ${entry.originalName}")
            }
            IconButton(onClick = onDeleteForever) {
                Icon(Icons.Filled.DeleteForever, contentDescription = "Delete ${entry.originalName} forever")
            }
        }
    }
}
