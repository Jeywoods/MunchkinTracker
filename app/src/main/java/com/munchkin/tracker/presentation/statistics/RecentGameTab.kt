package com.munchkin.tracker.presentation.statistics

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.munchkin.tracker.domain.model.Game
import com.munchkin.tracker.domain.model.GamePlayer
import com.munchkin.tracker.presentation.components.ConfirmDialog
import com.munchkin.tracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentGamesTab(games: List<Game>, vm: StatisticsViewModel) {
    if (games.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Нет сыгранных игр", color = OnSurfaceVariant) }
        return
    }
    var selectedGameId by remember { mutableStateOf<Long?>(null) }
    var gameToDelete by remember { mutableStateOf<Game?>(null) }
    val gamePlayers by vm.getGamePlayers(selectedGameId).collectAsStateWithLifecycle(emptyList())
    val sdf = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(games, key = { it.id }) { game ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value -> if (value == SwipeToDismissBoxValue.EndToStart) { gameToDelete = game }; false }
            )
            SwipeToDismissBox(
                state = dismissState, enableDismissFromEndToStart = true, enableDismissFromStartToEnd = false,
                backgroundContent = {
                    val color by animateColorAsState(
                        if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) LevelDown.copy(alpha = 0.3f) else Color.Transparent, label = "swipe_color"
                    )
                    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)).background(color), contentAlignment = Alignment.CenterEnd) {
                        Icon(Icons.Default.Delete, "Удалить", tint = LevelDown, modifier = Modifier.padding(end = 20.dp))
                    }
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SurfaceVariant).clickable { selectedGameId = game.id }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(if (game.isActive) "🎮" else "🏁", fontSize = 24.sp)
                    Column(Modifier.weight(1f)) {
                        Text(sdf.format(Date(game.date)), style = MaterialTheme.typography.titleSmall, color = OnBackground)
                        if (game.duration / 60000 > 0) Text("${game.duration / 60000} мин", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    }
                    Text("До ур. ${game.winLevel}", style = MaterialTheme.typography.labelMedium, color = Primary)
                }
            }
        }
    }
    if (selectedGameId != null) GameDetailDialog(gamePlayers, onDismiss = { selectedGameId = null })
    if (gameToDelete != null) {
        ConfirmDialog(
            title = "Удалить игру", message = "Удалить игру от ${sdf.format(Date(gameToDelete!!.date))}? Все связанные данные будут удалены.",
            confirmText = "Удалить", onConfirm = { vm.deleteGame(gameToDelete!!); gameToDelete = null }, onDismiss = { gameToDelete = null }
        )
    }
}

@Composable
private fun GameDetailDialog(gamePlayers: List<GamePlayer>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = SurfaceBright, modifier = Modifier.fillMaxWidth(), title = { Text("Детали игры") },
        text = {
            if (gamePlayers.isEmpty()) Text("Загрузка...", color = OnSurfaceVariant)
            else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                gamePlayers.sortedByDescending { it.currentLevel }.forEach { gp ->
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SurfaceVariant)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (gp.isWinner) "${gp.player.name} 👑" else gp.player.name, style = MaterialTheme.typography.titleSmall, color = OnBackground, modifier = Modifier.weight(1f))
                                Text("Ур. ${gp.currentLevel}", style = MaterialTheme.typography.labelLarge, color = Primary, fontWeight = FontWeight.Bold)
                            }
                            Text("💪 Сила: ${gp.player.power}", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                            val races = listOfNotNull(gp.player.race1, gp.player.race2)
                            val classes = listOfNotNull(gp.player.class1, gp.player.class2)
                            if (races.isNotEmpty()) Text("🎭 Раса: ${races.joinToString("/")}", style = MaterialTheme.typography.labelSmall, color = Secondary)
                            if (classes.isNotEmpty()) Text("⚔️ Класс: ${classes.joinToString("/")}", style = MaterialTheme.typography.labelSmall, color = Primary)
                        }
                    }
                }
            }
        },
        confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("Закрыть", color = OnSurfaceVariant) } }
    )
}