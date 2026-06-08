package com.munchkin.tracker.data.dao

import androidx.room.*
import com.munchkin.tracker.data.entity.GamePlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GamePlayerDao {
    @Query("SELECT * FROM game_players WHERE game_id = :gameId")
    fun getGamePlayers(gameId: Long): Flow<List<GamePlayerEntity>>

    @Query("SELECT * FROM game_players WHERE game_id = :gameId")
    suspend fun getGamePlayersOnce(gameId: Long): List<GamePlayerEntity>

    @Query("SELECT * FROM game_players WHERE id = :id")
    suspend fun getById(id: Long): GamePlayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGamePlayer(gp: GamePlayerEntity): Long

    @Query("UPDATE game_players SET current_level = :level WHERE id = :id")
    suspend fun updateLevel(id: Long, level: Int)

    @Query("UPDATE game_players SET is_winner = 1 WHERE id = :id")
    suspend fun setWinner(id: Long)

    @Query("""SELECT gp.* FROM game_players gp INNER JOIN players p ON p.id = gp.player_id WHERE p.name LIKE :nameLike AND gp.game_id = :gameId LIMIT 1""")
    suspend fun findByPlayerName(gameId: Long, nameLike: String): GamePlayerEntity?

    @Query("""SELECT gp.* FROM game_players gp WHERE gp.player_id = :playerId ORDER BY gp.id DESC""")
    suspend fun getGamesByPlayer(playerId: Long): List<GamePlayerEntity>
}