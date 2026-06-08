package com.munchkin.tracker.presentation.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.munchkin.tracker.domain.model.CategoryStats
import com.munchkin.tracker.ui.theme.*

@Composable
fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = OnBackground, fontWeight = FontWeight.Bold)
}

@Composable
fun HorizontalBarChart(data: List<Pair<CategoryStats, Color>>, maxValue: Float? = null) {
    val max = maxValue ?: data.maxOfOrNull { it.first.count }?.toFloat()?.takeIf { it > 0 } ?: 1f
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SurfaceVariant)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            data.forEach { (category, color) ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(category.label, Modifier.width(80.dp), style = MaterialTheme.typography.labelSmall, color = OnBackground, maxLines = 1)
                    LinearProgressIndicator(progress = { category.count.toFloat() / max }, modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)), color = color, trackColor = Outline)
                    Text("${category.count}", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelLarge, color = OnBackground, fontWeight = FontWeight.Bold)
    }
}