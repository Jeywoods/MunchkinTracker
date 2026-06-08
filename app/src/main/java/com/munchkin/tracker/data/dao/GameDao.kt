package com.munchkin.tracker.data.dao

import androidx.room.*
import com.munchkin.tracker.data.entity.GameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY date DESC")
    fun getAllGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE is_active = 1 LIMIT 1")
    fun getActiveGame(): Flow<GameEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity): Long

    @Query("UPDATE games SET is_active = 0, duration = :duration WHERE id = :id")
    suspend fun finishGame(id: Long, duration: Long)

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun deleteGame(id: Long)

    @Query("""SELECT AVG(duration) as avg_duration, COUNT(*) as total_games, MIN(duration) as min_duration, MAX(duration) as max_duration FROM games WHERE is_active = 0 AND duration > 0""")
    suspend fun getGameDurationStats(): GameDurationRaw
}