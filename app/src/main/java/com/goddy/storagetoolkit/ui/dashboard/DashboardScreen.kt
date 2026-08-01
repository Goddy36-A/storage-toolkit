package com.goddy.storagetoolkit.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.goddy.storagetoolkit.utils.FileUtils
import com.goddy.storagetoolkit.viewmodel.DashboardViewModel

private data class QuickAction(
    val title: String,
    val icon: ImageVector,
    val isAvailable: Boolean,
    val lastScanTime: Long? = null,
    val onClick: () -> Unit = {}
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onOpenDownloads: () -> Unit,
    onOpenApkManager: () -> Unit,
    onOpenZeroByte: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val actions = listOf(
        QuickAction("Duplicate Files", Icons.Filled.ContentCopy, isAvailable = false),
        QuickAction("Zero-byte Files", Icons.Filled.FileOpen, isAvailable = true, lastScanTime = uiState.zeroByteLastScan, onClick = onOpenZeroByte),
        QuickAction("Empty Folders", Icons.Filled.FolderOff, isAvailable = false),
        QuickAction("Large Files", Icons.Filled.Description, isAvailable = false),
        QuickAction("APK Files", Icons.Filled.Android, isAvailable = true, lastScanTime = uiState.apkLastScan, onClick = onOpenApkManager),
        QuickAction("Downloads", Icons.Filled.Download, isAvailable = true, lastScanTime = uiState.downloadsLastScan, onClick = onOpenDownloads),
        QuickAction("Images", Icons.Filled.Image, isAvailable = false),
        QuickAction("Videos", Icons.Filled.Movie, isAvailable = false),
        QuickAction("Documents", Icons.Filled.CreateNewFolder, isAvailable = false)
    )

    Scaffold(topBar = { TopAppBar(title = { Text("Storage Toolkit") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    StorageDonutChart(
                        usedFraction = uiState.storageStats.usedFraction,
                        freeBytesLabel = FileUtils.formatSize(uiState.storageStats.freeBytes)
                    )
                    Text(
                        text = "${FileUtils.formatSize(uiState.storageStats.usedBytes)} used of ${FileUtils.formatSize(uiState.storageStats.totalBytes)}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }

            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(actions) { action -> QuickActionCard(action) }
            }
        }
    }
}

@Composable
private fun QuickActionCard(action: QuickAction) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = action.isAvailable) { action.onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (action.isAvailable) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(imageVector = action.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = action.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = when {
                    !action.isAvailable -> "Coming soon"
                    action.lastScanTime == null -> "Not scanned yet"
                    else -> "Last scan: ${FileUtils.formatDate(action.lastScanTime)}"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
