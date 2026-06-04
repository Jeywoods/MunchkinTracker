package com.munchkin.tracker.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val gender: String,
    val power: Int = 0,
    val race1: String? = null,
    val race2: String? = null,
    val class1: String? = null,
    val class2: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val duration: Long = 0L,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "win_level") val winLevel: Int = 10
)

@Entity(
    tableName = "game_players",
    foreignKeys = [
        ForeignKey(entity = GameEntity::class, parentColumns = ["id"], childColumns = ["game_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PlayerEntity::class, parentColumns = ["id"], childColumns = ["player_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("game_id"), Index("player_id")]
)
data class GamePlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "game_id") val gameId: Long,
    @ColumnInfo(name = "player_id") val playerId: Long,
    @ColumnInfo(name = "current_level") val currentLevel: Int = 1,
    @ColumnInfo(name = "is_winner") val isWinner: Boolean = false
)

@Entity(
    tableName = "level_changes",
    foreignKeys = [
        ForeignKey(entity = GamePlayerEntity::class, parentColumns = ["id"], childColumns = ["game_player_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("game_player_id")]
)
data class LevelChangeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "game_player_id") val gamePlayerId: Long,
    @ColumnInfo(name = "old_level") val oldLevel: Int,
    @ColumnInfo(name = "new_level") val newLevel: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "manual"
)