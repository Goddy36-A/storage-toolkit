package com.goddy.storagetoolkit.ui.storageanalyzer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.goddy.storagetoolkit.models.CategoryBreakdown
import com.goddy.storagetoolkit.models.FileCategory
import com.goddy.storagetoolkit.utils.FileUtils

/** Fixed colors per category so the same category always reads the same way across scans. */
private val categoryColors: Map<FileCategory, Color> = mapOf(
    FileCategory.IMAGES to Color(0xFF4C9AFF),
    FileCategory.VIDEOS to Color(0xFFFF7452),
    FileCategory.AUDIO to Color(0xFF6554C0),
    FileCategory.DOCUMENTS to Color(0xFF00B8D9),
    FileCategory.ARCHIVES to Color(0xFFFFAB00),
    FileCategory.APKS to Color(0xFF36B37E),
    FileCategory.OTHERS to Color(0xFF8993A4)
)

/** One horizontal bar per category, sized relative to the largest category's size. */
@Composable
fun CategoryBarChart(breakdown: List<CategoryBreakdown>, modifier: Modifier = Modifier) {
    val maxBytes = breakdown.maxOfOrNull { it.totalSizeBytes } ?: 1L

    Column(modifier = modifier) {
        breakdown.forEach { entry ->
            val fraction = if (maxBytes == 0L) 0f else entry.totalSizeBytes.toFloat() / maxBytes.toFloat()
            val color = categoryColors[entry.category] ?: MaterialTheme.colorScheme.primary

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text(
                    text = entry.category.label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                drawRect(color = color.copy(alpha = 0.15f))
                drawRect(color = color, size = size.copy(width = size.width * fraction))
            }
            Text(
                text = "${entry.fileCount} file(s) • ${FileUtils.formatSize(entry.totalSizeBytes)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }
    }
}
