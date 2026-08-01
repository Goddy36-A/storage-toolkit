package com.goddy.storagetoolkit.ui.largefile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.goddy.storagetoolkit.models.FileCategory
import com.goddy.storagetoolkit.models.FileItem
import com.goddy.storagetoolkit.ui.common.FolderAccessPrompt
import com.goddy.storagetoolkit.ui.common.OnResumeEffect
import com.goddy.storagetoolkit.utils.FileUtils
import com.goddy.storagetoolkit.viewmodel.LargeFileSortOrder
import com.goddy.storagetoolkit.viewmodel.LargeFileViewModel
import com.goddy.storagetoolkit.viewmodel.SizeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeFileScreen(viewModel: LargeFileViewModel, onBack: () -> Unit) {
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
                title = { Text("Large Files") },
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
                                    text = { Text("Largest first") },
                                    onClick = { viewModel.setSortOrder(LargeFileSortOrder.SIZE); sortMenuOpen = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Name") },
                                    onClick = { viewModel.setSortOrder(LargeFileSortOrder.NAME); sortMenuOpen = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Date") },
                                    onClick = { viewModel.setSortOrder(LargeFileSortOrder.DATE); sortMenuOpen = false }
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
                        message = "Grant storage access to find large files across your whole device.",
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
                        Text("Scanning for large files...", modifier = Modifier.padding(top = 16.dp))
                        TextButton(onClick = { viewModel.cancelScan() }, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Cancel")
                        }
                    }
                }
                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SizeFilter.values().forEach { filter ->
                            FilterChip(
                                selected = uiState.filter == filter,
                                onClick = { viewModel.setFilter(filter) },
                                label = { Text(filter.label) }
                            )
                        }
                    }

                    if (uiState.files.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("No files found at or above ${uiState.filter.label}.")
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${uiState.selectedIds.size} of ${uiState.files.size} selected")
                            Row {
                                TextButton(onClick = {
                                    if (uiState.selectedIds.size == uiState.files.size) viewModel.clearSelection()
                                    else viewModel.selectAll()
                                }) {
                                    Text(if (uiState.selectedIds.size == uiState.files.size) "Clear" else "Select All")
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
                            items(uiState.files) { file ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = file.documentId in uiState.selectedIds,
                                            onCheckedChange = { viewModel.toggleSelection(file.documentId) }
                                        )
                                        Column(
                                            modifier = Modifier
                                                .padding(start = 8.dp)
                                                .weight(1f)
                                        ) {
                                            Text(file.name, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                text = "${FileUtils.formatSize(file.sizeBytes)} • ${file.relativePath.ifEmpty { "In root folder" }}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Modified: ${FileUtils.formatDate(file.lastModified)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        TextButton(onClick = { viewModel.showPreview(file) }) {
                                            Text("Preview")
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

    uiState.previewFile?.let { file ->
        LargeFilePreviewDialog(
            file = file,
            onDismiss = { viewModel.dismissPreview() },
            onDelete = { viewModel.deletePreviewedFile() }
        )
    }
}

@Composable
private fun LargeFilePreviewDialog(file: FileItem, onDismiss: () -> Unit, onDelete: () -> Unit) {
    var thumbnail by remember(file.documentId) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(file.documentId) {
        if (file.category == FileCategory.IMAGES) {
            thumbnail = withContext(Dispatchers.IO) { decodeThumbnail(file.uriString) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(file.name) },
        text = {
            Column {
                thumbnail?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(160.dp).padding(bottom = 12.dp)
                    )
                }
                Text("Size: ${FileUtils.formatSize(file.sizeBytes)}")
                Text("Location: ${file.relativePath.ifEmpty { "Root folder" }}")
                Text("Modified: ${FileUtils.formatDate(file.lastModified)}")
            }
        },
        confirmButton = {
            Button(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

/** Downsampled decode so a multi-hundred-MB photo doesn't get fully loaded into memory just for a preview. */
private fun decodeThumbnail(uriString: String): Bitmap? {
    return try {
        val path = android.net.Uri.parse(uriString).path ?: return null
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, boundsOptions)

        var sampleSize = 1
        val targetPixels = 512
        while (boundsOptions.outWidth / sampleSize > targetPixels || boundsOptions.outHeight / sampleSize > targetPixels) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        BitmapFactory.decodeFile(path, decodeOptions)
    } catch (e: Exception) {
        null
    }
}
