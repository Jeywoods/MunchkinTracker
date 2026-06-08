package com.munchkin.tracker.presentation.statistics

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.munchkin.tracker.data.export.CsvExportHelper
import com.munchkin.tracker.domain.model.*
import com.munchkin.tracker.presentation.components.AppTopBar
import com.munchkin.tracker.presentation.components.ConfirmDialog
import com.munchkin.tracker.presentation.components.MagicCircleBackground
import com.munchkin.tracker.presentation.navigation.Routes
import com.munchkin.tracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatisticsScreen(
    navController: NavController,
    vm: StatisticsViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current


    LaunchedEffect(state.csvContent) {
        state.csvContent?.let { csv ->
            context.startActivity(Intent.createChooser(CsvExportHelper.saveCsvAndShare(context, csv, "munchkin_stats"), "Экспортировать CSV"))
            vm.clearCsv()
        }
    }
    LaunchedEffect(Unit) {
        vm.refresh()
    }
    LaunchedEffect(selectedTab) {
        if (selectedTab == 2) {
            vm.refresh()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar("Статистика") {
                IconButton(onClick = { vm.exportStats() }) {
                    Icon(Icons.Default.Download, "Экспорт CSV", tint = Primary)
                }
            }
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Background,
                contentColor = Primary,
                indicator = {}
            ) {
                listOf("Игры", "Игроки", "Графики").forEachIndexed { idx, label ->
                    Tab(selected = selectedTab == idx, onClick = { selectedTab = idx },
                        text = {
                            Text(label,
                                fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == idx) Primary else OnSurfaceVariant
                            )
                        })
                }
            }

            // Контент с кругом на фоне
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                MagicCircleBackground(alpha = 0.5f)

                when (selectedTab) {
                    0 -> RecentGamesTab(state.recentGames, vm)
                    1 -> PlayerStatsTab(state.summary, navController)
                    2 -> ChartsTab(state)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecentGamesTab(games: List<Game>, vm: StatisticsViewModel) {
    if (games.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Нет сыгранных игр", color = OnSurfaceVariant)
        }
        return
    }
    var selectedGameId by remember { mutableStateOf<Long?>(null) }
    var gameToDelete by remember { mutableStateOf<Game?>(null) }
    val gamePlayers by vm.getGamePlayers(selectedGameId).collectAsStateWithLifecycle(emptyList())

    val sdf = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(games, key = { it.id }) { game ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value == SwipeToDismissBoxValue.EndToStart) {
                        gameToDelete = game
                    }
                    false
                }
            )
            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromEndToStart = true,
                enableDismissFromStartToEnd = false,
                backgroundContent = {
                    val color by animateColorAsState(
                        if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                            LevelDown.copy(alpha = 0.3f)
                        else Color.Transparent,
                        label = "swipe_color"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp))
                            .background(color),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            "Удалить",
                            tint = LevelDown,
                            modifier = Modifier.padding(end = 20.dp)
                        )
                    }
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceVariant)
                        .clickable { selectedGameId = game.id }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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

    if (selectedGameId != null) {
        GameDetailDialog(
            gamePlayers = gamePlayers,
            onDismiss = { selectedGameId = null }
        )
    }

    if (gameToDelete != null) {
        ConfirmDialog(
            title = "Удалить игру",
            message = "Удалить игру от ${sdf.format(Date(gameToDelete!!.date))}? Все связанные данные будут удалены.",
            confirmText = "Удалить",
            onConfirm = {
                vm.deleteGame(gameToDelete!!)
                gameToDelete = null
            },
            onDismiss = { gameToDelete = null }
        )
    }
}

@Composable
private fun GameDetailDialog(gamePlayers: List<GamePlayer>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceBright,
        modifier = Modifier.fillMaxWidth(),
        title = { Text("Детали игры") },
        text = {
            if (gamePlayers.isEmpty()) {
                Text("Загрузка...", color = OnSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    gamePlayers.sortedByDescending { it.currentLevel }.forEach { gp ->
                        val winnerIcon = if (gp.isWinner) " 👑" else ""
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${gp.player.name}$winnerIcon", style = MaterialTheme.typography.titleSmall, color = OnBackground, modifier = Modifier.weight(1f))
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
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Закрыть", color = OnSurfaceVariant) } }
    )
}

@Composable
private fun PlayerStatsTab(summary: StatsSummary?, navController: NavController) {
    if (summary == null || summary.topPlayers.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Недостаточно данных", color = OnSurfaceVariant) }
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(summary.topPlayers.take(10).mapIndexed { i, p -> i to p }) { (rank, player) ->
            TopPlayerRow(rank + 1, player) { navController.navigate(Routes.playerDetail(player.id)) }
        }
    }
}

@Composable
private fun TopPlayerRow(rank: Int, player: TopPlayer, onClick: () -> Unit) {
    val rankColor = when (rank) { 1 -> GoldGlow; 2 -> Color(0xFFC0C0C0); 3 -> Color(0xFFCD7F32); else -> OnSurfaceVariant }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SurfaceVariant).clickable { onClick() }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("#$rank", fontWeight = FontWeight.Black, color = rankColor, fontSize = 18.sp, modifier = Modifier.width(36.dp))
        Column(Modifier.weight(1f)) {
            Text(player.name, style = MaterialTheme.typography.titleSmall, color = OnBackground)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Default.EmojiEvents, null, tint = GoldGlow, modifier = Modifier.size(16.dp))
            Text("${player.wins}", fontWeight = FontWeight.Bold, color = GoldGlow)
        }
    }
}

@Composable
private fun ChartsTab(state: StatsUiState) {
    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }

    // Проверяем, есть ли хоть какие-то данные
    val hasData = state.summary != null ||
            state.winsByClass.isNotEmpty() ||
            state.winsByRace.isNotEmpty() ||
            state.classPopularity.isNotEmpty() ||
            state.racePopularity.isNotEmpty() ||
            state.gameDurationStats != null ||
            state.classEfficiency.isNotEmpty() ||
            state.raceEfficiency.isNotEmpty() ||
            state.topClassRaceCombos.isNotEmpty()

    if (!hasData) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Нет данных для отображения", color = OnSurfaceVariant)
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. Победы по полу (только если есть summary)
        if (state.summary != null) {
            item {
                SectionTitle("Победы по полу")
                HorizontalBarChart(
                    data = listOf(
                        CategoryStats("♂ Муж.", state.summary.maleWins) to Primary,
                        CategoryStats("♀ Жен.", state.summary.femaleWins) to Tertiary
                    )
                )
            }
        }

        // 2. Победы по классам
        if (state.winsByClass.isNotEmpty()) {
            item {
                SectionTitle("Победы по классам")
                HorizontalBarChart(
                    data = state.winsByClass.map { (cls, wins) ->
                        CategoryStats(cls, wins) to Primary
                    }.sortedByDescending { it.first.count }
                )
            }
        }

        // 3. Победы по расам
        if (state.winsByRace.isNotEmpty()) {
            item {
                SectionTitle("Победы по расам")
                HorizontalBarChart(
                    data = state.winsByRace.map { (race, wins) ->
                        CategoryStats(race, wins) to Primary
                    }.sortedByDescending { it.first.count }
                )
            }
        }

        // 5. Популярность классов
        if (state.classPopularity.isNotEmpty()) {
            item {
                SectionTitle("Популярность классов")
                HorizontalBarChart(
                    data = state.classPopularity.map { (cls, count) ->
                        CategoryStats(cls, count) to Secondary
                    }.sortedByDescending { it.first.count }
                )
            }
        }

        // 6. Популярность рас
        if (state.racePopularity.isNotEmpty()) {
            item {
                SectionTitle("Популярность рас")
                HorizontalBarChart(
                    data = state.racePopularity.map { (race, count) ->
                        CategoryStats(race, count) to Secondary
                    }.sortedByDescending { it.first.count }
                )
            }
        }

        // 7. Средняя длительность игр
        if (state.gameDurationStats != null) {
            item {
                SectionTitle("Длительность игр")
                val stats = state.gameDurationStats
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatRow("Средняя", "${String.format("%.1f", stats.avgDurationMin)} мин")
                        StatRow("Минимальная", "${String.format("%.1f", stats.minDurationMin)} мин")
                        StatRow("Максимальная", "${String.format("%.1f", stats.maxDurationMin)} мин")
                        StatRow("Всего игр", "${stats.totalGames}")
                    }
                }
            }
        }

        // 8. Эффективность классов
        if (state.classEfficiency.isNotEmpty()) {
            item {
                SectionTitle("Эффективность классов (% побед)")
                HorizontalBarChart(
                    data = state.classEfficiency.map { (cls, efficiency) ->
                        CategoryStats("${cls} (${String.format("%.0f", efficiency)}%)", efficiency.toInt()) to Primary
                    }.sortedByDescending { it.first.count },
                    maxValue = 100f
                )
            }
        }

        // 9. Эффективность рас
        if (state.raceEfficiency.isNotEmpty()) {
            item {
                SectionTitle("Эффективность рас (% побед)")
                HorizontalBarChart(
                    data = state.raceEfficiency.map { (race, efficiency) ->
                        CategoryStats("${race} (${String.format("%.0f", efficiency)}%)", efficiency.toInt()) to Primary
                    }.sortedByDescending { it.first.count },
                    maxValue = 100f
                )
            }
        }

        // 10. Топ комбинаций класс+раса
        if (state.topClassRaceCombos.isNotEmpty()) {
            item {
                SectionTitle("Топ комбинаций класс+раса")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.topClassRaceCombos.forEach { combo ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "⚔️ ${combo.classCombo} | 🎭 ${combo.raceCombo}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = OnBackground
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${combo.wins}", color = GoldGlow, fontWeight = FontWeight.Bold)
                                    Text(" побед", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = OnBackground,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun HorizontalBarChart(
    data: List<Pair<CategoryStats, Color>>,
    maxValue: Float? = null
) {
    val max = maxValue ?: data.maxOfOrNull { it.first.count }?.toFloat()?.takeIf { it > 0 } ?: 1f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            data.forEach { (category, color) ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        category.label,
                        Modifier.width(80.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnBackground,
                        maxLines = 1
                    )
                    LinearProgressIndicator(
                        progress = { category.count.toFloat() / max },
                        modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)),
                        color = color,
                        trackColor = Outline,
                    )
                    Text(
                        "${category.count}",
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(32.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelLarge, color = OnBackground, fontWeight = FontWeight.Bold)
    }
}