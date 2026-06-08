package com.munchkin.tracker.data.dao

import androidx.room.*
import com.munchkin.tracker.data.entity.LevelChangeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LevelChangeDao {
    @Query("""SELECT lc.* FROM level_changes lc INNER JOIN game_players gp ON gp.id = lc.game_player_id WHERE gp.game_id = :gameId ORDER BY lc.timestamp DESC""")
    fun getChangesForGame(gameId: Long): Flow<List<LevelChangeEntity>>

    @Query("""SELECT lc.* FROM level_changes lc INNER JOIN game_players gp ON gp.id = lc.game_player_id WHERE gp.game_id = :gameId ORDER BY lc.timestamp DESC LIMIT 1""")
    suspend fun getLastChangeForGame(gameId: Long): LevelChangeEntity?

    @Insert
    suspend fun insertChange(change: LevelChangeEntity): Long

    @Delete
    suspend fun deleteChange(change: LevelChangeEntity)

    @Query("SELECT * FROM level_changes WHERE game_player_id = :gpId ORDER BY timestamp ASC")
    suspend fun getChangesForGamePlayer(gpId: Long): List<LevelChangeEntity>
}