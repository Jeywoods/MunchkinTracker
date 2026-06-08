package com.munchkin.tracker.domain.model

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