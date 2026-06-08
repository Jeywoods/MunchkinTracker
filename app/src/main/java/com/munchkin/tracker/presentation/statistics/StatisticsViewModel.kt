package com.munchkin.tracker.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.munchkin.tracker.data.repository.MunchkinRepository
import com.munchkin.tracker.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatsUiState(
    val summary: StatsSummary? = null,
    val recentGames: List<Game> = emptyList(),
    val isLoading: Boolean = true,
    val csvContent: String? = null,
    val winsByClass: Map<String, Int> = emptyMap(),
    val winsByRace: Map<String, Int> = emptyMap(),
    val genderDistribution: Map<String, Int> = emptyMap(),
    val classPopularity: Map<String, Int> = emptyMap(),
    val racePopularity: Map<String, Int> = emptyMap(),
    val classEfficiency: Map<String, Float> = emptyMap(),
    val raceEfficiency: Map<String, Float> = emptyMap(),
    val topClassRaceCombos: List<ClassRaceCombo> = emptyList(),
    val gameDurationStats: GameDurationStats? = null
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repo: MunchkinRepository
) : ViewModel() {
    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            repo.getAllGames().collect { games ->
                _state.update { it.copy(recentGames = games.take(20), isLoading = false) }
            }
        }
        viewModelScope.launch {
            loadStats()
        }
    }

    private suspend fun loadStats() {
        val summary = repo.getStatsSummary()
        val winsByClass = repo.getWinsByClass()
        val winsByRace = repo.getWinsByRace()
        val genderDistribution = repo.getGenderDistribution()
        val classPopularity = repo.getClassPopularity()
        val racePopularity = repo.getRacePopularity()
        val classEfficiency = repo.getClassEfficiency()
        val raceEfficiency = repo.getRaceEfficiency()
        val topClassRaceCombos = repo.getTopClassRaceCombos()
        val gameDurationStats = repo.getGameDurationStats()

        val updatedSummary = summary?.copy(
            maleCount = genderDistribution["male"] ?: 0,
            femaleCount = genderDistribution["female"] ?: 0
        )

        _state.update {
            it.copy(
                summary = updatedSummary,
                winsByClass = winsByClass,
                winsByRace = winsByRace,
                genderDistribution = genderDistribution,
                classPopularity = classPopularity,
                racePopularity = racePopularity,
                classEfficiency = classEfficiency,
                raceEfficiency = raceEfficiency,
                topClassRaceCombos = topClassRaceCombos,
                gameDurationStats = gameDurationStats
            )
        }
    }

    fun getGamePlayers(gameId: Long?): Flow<List<GamePlayer>> {
        if (gameId == null) return flowOf(emptyList())
        return repo.getGamePlayers(gameId)
    }

    fun exportStats() {
        viewModelScope.launch {
            val csv = repo.exportStatsCsv()
            _state.update { it.copy(csvContent = csv) }
        }
    }

    fun clearCsv() { _state.update { it.copy(csvContent = null) } }

    fun deleteGame(game: Game) {
        viewModelScope.launch {
            repo.deleteGame(game.id)
            loadStats()
        }
    }
    fun refresh() {
        viewModelScope.launch {
            loadStats()
        }
    }
}