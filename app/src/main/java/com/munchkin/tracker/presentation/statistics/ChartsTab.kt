package com.munchkin.tracker.presentation.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.munchkin.tracker.domain.model.CategoryStats
import com.munchkin.tracker.ui.theme.*
import java.util.Locale

@Composable
fun ChartsTab(state: StatsUiState) {
    if (state.isLoading) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Primary) }; return }
    val hasData = state.summary != null || state.winsByClass.isNotEmpty() || state.winsByRace.isNotEmpty() ||
            state.classPopularity.isNotEmpty() || state.racePopularity.isNotEmpty() || state.gameDurationStats != null ||
            state.classEfficiency.isNotEmpty() || state.raceEfficiency.isNotEmpty() || state.topClassRaceCombos.isNotEmpty()
    if (!hasData) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Нет данных", color = OnSurfaceVariant) }; return }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        if (state.summary != null) { item { SectionTitle("Победы по полу"); HorizontalBarChart(listOf(CategoryStats("♂ Муж.", state.summary.maleWins) to Primary, CategoryStats("♀ Жен.", state.summary.femaleWins) to Tertiary)) } }
        if (state.winsByClass.isNotEmpty()) { item { SectionTitle("Победы по классам"); HorizontalBarChart(state.winsByClass.map { (k,v) -> CategoryStats(k,v) to Primary }.sortedByDescending { it.first.count }) } }
        if (state.winsByRace.isNotEmpty()) { item { SectionTitle("Победы по расам"); HorizontalBarChart(state.winsByRace.map { (k,v) -> CategoryStats(k,v) to Primary }.sortedByDescending { it.first.count }) } }
        if (state.classPopularity.isNotEmpty()) { item { SectionTitle("Популярность классов"); HorizontalBarChart(state.classPopularity.map { (k,v) -> CategoryStats(k,v) to Secondary }.sortedByDescending { it.first.count }) } }
        if (state.racePopularity.isNotEmpty()) { item { SectionTitle("Популярность рас"); HorizontalBarChart(state.racePopularity.map { (k,v) -> CategoryStats(k,v) to Secondary }.sortedByDescending { it.first.count }) } }
        if (state.gameDurationStats != null) { item { SectionTitle("Длительность игр"); val s=state.gameDurationStats; Card(Modifier.fillMaxWidth(), colors=CardDefaults.cardColors(containerColor=SurfaceVariant)) { Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) { StatRow("Средняя",String.format(Locale.getDefault(),"%.1f мин",s.avgDurationMin)); StatRow("Минимальная",String.format(Locale.getDefault(),"%.1f мин",s.minDurationMin)); StatRow("Максимальная",String.format(Locale.getDefault(),"%.1f мин",s.maxDurationMin)); StatRow("Всего игр",s.totalGames.toString()) } } } }
        if (state.classEfficiency.isNotEmpty()) { item { SectionTitle("Эффективность классов"); HorizontalBarChart(state.classEfficiency.map{(k,v)->CategoryStats("$k (${String.format(Locale.getDefault(),"%.0f",v)}%)",v.toInt()) to Primary}.sortedByDescending{it.first.count}, maxValue=100f) } }
        if (state.raceEfficiency.isNotEmpty()) { item { SectionTitle("Эффективность рас"); HorizontalBarChart(state.raceEfficiency.map{(k,v)->CategoryStats("$k (${String.format(Locale.getDefault(),"%.0f",v)}%)",v.toInt()) to Primary}.sortedByDescending{it.first.count}, maxValue=100f) } }
        if (state.topClassRaceCombos.isNotEmpty()) { item { SectionTitle("Топ комбинаций класс+раса"); Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=SurfaceVariant)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){state.topClassRaceCombos.forEach{c->Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text("⚔️ ${c.classCombo} | 🎭 ${c.raceCombo}",style=MaterialTheme.typography.labelMedium,color=OnBackground);Row(verticalAlignment=Alignment.CenterVertically){Text("${c.wins}",color=GoldGlow,fontWeight=FontWeight.Bold);Text(" побед",style=MaterialTheme.typography.labelSmall,color=OnSurfaceVariant)}}}} } } }
    }
}