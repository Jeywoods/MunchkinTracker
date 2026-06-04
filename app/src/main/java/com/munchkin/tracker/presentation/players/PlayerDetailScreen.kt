package com.munchkin.tracker.presentation.players

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.munchkin.tracker.domain.model.Gender
import com.munchkin.tracker.presentation.components.AppTopBar
import com.munchkin.tracker.ui.theme.*

@Composable
fun PlayerDetailScreen(
    playerId: Long,
    navController: NavController,
    vm: PlayerManagementViewModel = hiltViewModel()
) {
    val detail by vm.detail.collectAsStateWithLifecycle()
    LaunchedEffect(playerId) { vm.loadDetail(playerId) }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        if (detail == null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Primary) }; return@Box }
        val stats = detail!!
        val genderColor = when (stats.player.gender) { Gender.MALE -> Primary; Gender.FEMALE -> Tertiary }

        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(stats.player.name, onBack = { navController.popBackStack() })
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(SurfaceVariant).padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.size(72.dp).clip(CircleShape).background(genderColor.copy(alpha = 0.15f)).border(2.dp, genderColor, CircleShape), contentAlignment = Alignment.Center) {
                            Text(stats.player.name.take(1).uppercase(), fontWeight = FontWeight.Black, color = genderColor, fontSize = 32.sp)
                        }
                        Column { Text(stats.player.name, style = MaterialTheme.typography.headlineSmall, color = OnBackground); Text("${stats.player.gender.icon} ${stats.player.gender.label}  💪 ${stats.player.power}", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant) }
                    }
                }
                item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { StatCard("🎮", "Игр", "${stats.totalGames}", Primary, Modifier.weight(1f)); StatCard("🏆", "Побед", "${stats.wins}", GoldGlow, Modifier.weight(1f)) } }
                item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { StatCard("⚡", "Макс. уровень", "${stats.maxLevel}", Secondary, Modifier.weight(1f)); StatCard("📊", "Ср. финал", "%.1f".format(stats.avgFinalLevel), Tertiary, Modifier.weight(1f)) } }
                item {
                    val winRate = if (stats.totalGames > 0) stats.wins.toFloat() / stats.totalGames else 0f
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SurfaceVariant).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Процент побед: ${(winRate * 100).toInt()}%", style = MaterialTheme.typography.titleSmall, color = OnBackground)
                        LinearProgressIndicator({ winRate }, Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = GoldGlow, trackColor = Outline)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(icon: String, label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(SurfaceVariant).border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(16.dp)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(icon, fontSize = 28.sp); Text(value, fontWeight = FontWeight.Black, color = color, fontSize = 28.sp); Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
    }
}