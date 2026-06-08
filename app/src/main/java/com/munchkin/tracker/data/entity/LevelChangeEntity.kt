package com.munchkin.tracker.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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