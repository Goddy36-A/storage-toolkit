package com.goddy.storagetoolkit.ui.storageanalyzer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
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
import com.goddy.storagetoolkit.viewmodel.StorageAnalyzerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageAnalyzerScreen(viewModel: StorageAnalyzerViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { viewModel.refreshAccess() }

    OnResumeEffect { viewModel.refreshAccess() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage Analyzer") },
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
                        message = "Grant storage access to see a breakdown of what's using space on your device.",
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
                        Text("Analyzing storage...", modifier = Modifier.padding(top = 16.dp))
                        TextButton(onClick = { viewModel.cancelScan() }, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Cancel")
                        }
                    }
                }
                uiState.breakdown.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("No files found.")
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            "${FileUtils.formatSize(uiState.totalBytes)} across ${uiState.breakdown.sumOf { it.fileCount }} file(s)",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        CategoryBarChart(breakdown = uiState.breakdown)
                    }
                }
            }
        }
    }
}
