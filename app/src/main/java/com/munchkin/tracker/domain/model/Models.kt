package com.munchkin.tracker.domain.model

enum class Gender(val label: String, val icon: String) {
    MALE("Мужской", "♂"),
    FEMALE("Женский", "♀")
}

enum class VoiceState {
    SLEEPING, ACTIVE, RECORDING, ERROR
}

enum class LevelChangeSource(val label: String) {
    MANUAL("Кнопка"),
    VOICE("Голос"),
    UNDO("Отмена")
}

data class Player(
    val id: Long,
    val name: String,
    val gender: Gender,
    val power: Int = 0,
    val race1: String? = null,
    val race2: String? = null,
    val class1: String? = null,
    val class2: String? = null,
    val createdAt: Long
)

data class Game(
    val id: Long,
    val date: Long,
    val duration: Long,
    val isActive: Boolean,
    val winLevel: Int
)

data class GamePlayer(
    val id: Long,
    val gameId: Long,
    val player: Player,
    val currentLevel: Int,
    val isWinner: Boolean,
    val lastDelta: Int = 0
)

data class LevelChange(
    val id: Long,
    val gamePlayerId: Long,
    val playerName: String,
    val oldLevel: Int,
    val newLevel: Int,
    val timestamp: Long,
    val source: LevelChangeSource
)

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

data class GameHistoryItem(
    val game: Game,
    val players: List<GamePlayer>,
    val winner: GamePlayer?
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