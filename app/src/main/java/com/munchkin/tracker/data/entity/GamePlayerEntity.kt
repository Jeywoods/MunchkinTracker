package com.munchkin.tracker.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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