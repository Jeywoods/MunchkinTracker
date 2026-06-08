package com.munchkin.tracker.presentation.statistics

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.munchkin.tracker.data.export.CsvExportHelper
import com.munchkin.tracker.presentation.components.AppTopBar
import com.munchkin.tracker.presentation.components.MagicCircleBackground
import com.munchkin.tracker.ui.theme.*

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
    LaunchedEffect(Unit) { vm.refresh() }
    LaunchedEffect(selectedTab) { if (selectedTab == 2) vm.refresh() }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar("Статистика") {
                IconButton(onClick = { vm.exportStats() }) {
                    Icon(Icons.Default.Download, "Экспорт CSV", tint = Primary)
                }
            }
            TabRow(selectedTabIndex = selectedTab, containerColor = Background, contentColor = Primary, indicator = {}) {
                listOf("Игры", "Игроки", "Графики").forEachIndexed { idx, label ->
                    Tab(selected = selectedTab == idx, onClick = { selectedTab = idx },
                        text = { Text(label, fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal, color = if (selectedTab == idx) Primary else OnSurfaceVariant) })
                }
            }
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