package com.munchkin.tracker.presentation.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.munchkin.tracker.domain.model.StatsSummary
import com.munchkin.tracker.domain.model.TopPlayer
import com.munchkin.tracker.presentation.navigation.Routes
import com.munchkin.tracker.ui.theme.*

@Composable
fun PlayerStatsTab(summary: StatsSummary?, navController: NavController) {
    if (summary?.topPlayers.isNullOrEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Недостаточно данных", color = OnSurfaceVariant) }
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(summary!!.topPlayers.take(10).mapIndexed { i, p -> i to p }) { (rank, player) ->
            TopPlayerRow(rank + 1, player) { navController.navigate(Routes.playerDetail(player.id)) }
        }
    }
}

@Composable
private fun TopPlayerRow(rank: Int, player: TopPlayer, onClick: () -> Unit) {
    val rankColor = when (rank) { 1 -> GoldGlow; 2 -> Color(0xFFC0C0C0); 3 -> Color(0xFFCD7F32); else -> OnSurfaceVariant }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SurfaceVariant).clickable { onClick() }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("#$rank", fontWeight = FontWeight.Black, color = rankColor, fontSize = 18.sp, modifier = Modifier.width(36.dp))
        Column(Modifier.weight(1f)) { Text(player.name, style = MaterialTheme.typography.titleSmall, color = OnBackground) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Default.EmojiEvents, null, tint = GoldGlow, modifier = Modifier.size(16.dp))
            Text("${player.wins}", fontWeight = FontWeight.Bold, color = GoldGlow)
        }
    }
}