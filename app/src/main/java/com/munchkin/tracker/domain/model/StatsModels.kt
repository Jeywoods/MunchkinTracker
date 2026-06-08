package com.munchkin.tracker.domain.model

data class PlayerStats(
    val player: Player,
    val totalGames: Int,
    val wins: Int,
    val maxLevel: Int,
    val avgFinalLevel: Double
)

data class StatsSummary(
    val maleWins: Int = 0,
    val femaleWins: Int = 0,
    val topPlayers: List<TopPlayer> = emptyList(),
    val maleCount: Int = 0,
    val femaleCount: Int = 0
)

data class TopPlayer(
    val id: Long,
    val name: String,
    val gender: Gender,
    val wins: Int
)

data class ClassRaceCombo(
    val classCombo: String,
    val raceCombo: String,
    val wins: Int
)

data class GameDurationStats(
    val avgDurationMin: Double,
    val totalGames: Int,
    val minDurationMin: Double,
    val maxDurationMin: Double
)

data class CategoryStats(
    val label: String,
    val count: Int
)

data class EfficiencyStats(
    val label: String,
    val efficiency: Float,
    val games: Int,
    val wins: Int
)

data class GameHistoryItem(
    val game: Game,
    val players: List<GamePlayer>,
    val winner: GamePlayer?
)