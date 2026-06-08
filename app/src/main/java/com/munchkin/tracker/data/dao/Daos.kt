package com.munchkin.tracker.data.dao

import androidx.room.*
import com.munchkin.tracker.data.entity.*
import kotlinx.coroutines.flow.Flow

// ─── PlayerDao ───────────────────────────────────────────────────────────────
@Dao
interface PlayerDao {
    @Query("SELECT * FROM players ORDER BY created_at DESC")
    fun getAllPlayers(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun getPlayerById(id: Long): PlayerEntity?

    @Query("SELECT * FROM players WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getPlayerByName(name: String): PlayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: PlayerEntity): Long

    @Update
    suspend fun updatePlayer(player: PlayerEntity)

    @Delete
    suspend fun deletePlayer(player: PlayerEntity)

    @Query("SELECT COUNT(*) FROM players")
    suspend fun getPlayerCount(): Int

    @Query("""
        SELECT gender, COUNT(*) as count
        FROM players
        GROUP BY gender
    """)
    suspend fun getGenderDistribution(): List<GenderCount>
}

// ─── GameDao ─────────────────────────────────────────────────────────────────
@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY date DESC")
    fun getAllGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE is_active = 1 LIMIT 1")
    fun getActiveGame(): Flow<GameEntity?>

    @Query("SELECT * FROM games WHERE is_active = 1 LIMIT 1")
    suspend fun getActiveGameOnce(): GameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity): Long

    @Update
    suspend fun updateGame(game: GameEntity)

    @Query("UPDATE games SET is_active = 0, duration = :duration WHERE id = :id")
    suspend fun finishGame(id: Long, duration: Long)

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun deleteGame(id: Long)

    @Query("""
        SELECT AVG(duration) as avg_duration,
               COUNT(*) as total_games,
               MIN(duration) as min_duration,
               MAX(duration) as max_duration
        FROM games
        WHERE is_active = 0 AND duration > 0
    """)
    suspend fun getGameDurationStats(): GameDurationRaw
}

// ─── GamePlayerDao ────────────────────────────────────────────────────────────
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

    @Update
    suspend fun updateGamePlayer(gp: GamePlayerEntity)

    @Query("UPDATE game_players SET current_level = :level WHERE id = :id")
    suspend fun updateLevel(id: Long, level: Int)

    @Query("UPDATE game_players SET is_winner = 1 WHERE id = :id")
    suspend fun setWinner(id: Long)

    @Query("""
        SELECT gp.* FROM game_players gp
        INNER JOIN players p ON p.id = gp.player_id
        WHERE p.name LIKE :nameLike AND gp.game_id = :gameId
        LIMIT 1
    """)
    suspend fun findByPlayerName(gameId: Long, nameLike: String): GamePlayerEntity?

    @Query("""
        SELECT p.gender, COUNT(*) as wins
        FROM game_players gp
        INNER JOIN players p ON p.id = gp.player_id
        INNER JOIN games g ON g.id = gp.game_id
        WHERE gp.is_winner = 1 AND g.is_active = 0
        GROUP BY p.gender
    """)
    suspend fun getWinsByGender(): List<GenderWinCount>

    @Query("""
        SELECT p.id, p.name, p.gender, COUNT(*) as wins
        FROM game_players gp
        INNER JOIN players p ON p.id = gp.player_id
        INNER JOIN games g ON g.id = gp.game_id
        WHERE gp.is_winner = 1 AND g.is_active = 0
        GROUP BY p.id
        ORDER BY wins DESC
        LIMIT 10
    """)
    suspend fun getTopPlayers(): List<PlayerWinCount>

    @Query("""
        SELECT gp.* FROM game_players gp WHERE gp.player_id = :playerId
        ORDER BY gp.id DESC
    """)
    suspend fun getGamesByPlayer(playerId: Long): List<GamePlayerEntity>

    @Query("""
        SELECT p.class1, p.class2, COUNT(*) as wins
        FROM game_players gp
        INNER JOIN players p ON p.id = gp.player_id
        INNER JOIN games g ON g.id = gp.game_id
        WHERE gp.is_winner = 1 AND g.is_active = 0
        GROUP BY p.class1, p.class2
    """)
    suspend fun getWinsByClass(): List<ClassWinCount>

    @Query("""
        SELECT p.race1, p.race2, COUNT(*) as wins
        FROM game_players gp
        INNER JOIN players p ON p.id = gp.player_id
        INNER JOIN games g ON g.id = gp.game_id
        WHERE gp.is_winner = 1 AND g.is_active = 0
        GROUP BY p.race1, p.race2
    """)
    suspend fun getWinsByRace(): List<RaceWinCount>

    @Query("""
        SELECT p.class1, p.class2, COUNT(*) as games_count
        FROM game_players gp
        INNER JOIN players p ON p.id = gp.player_id
        INNER JOIN games g ON g.id = gp.game_id
        WHERE g.is_active = 0
        GROUP BY p.class1, p.class2
    """)
    suspend fun getClassPopularity(): List<ClassPopularityCount>

    @Query("""
        SELECT p.race1, p.race2, COUNT(*) as games_count
        FROM game_players gp
        INNER JOIN players p ON p.id = gp.player_id
        INNER JOIN games g ON g.id = gp.game_id
        WHERE g.is_active = 0
        GROUP BY p.race1, p.race2
    """)
    suspend fun getRacePopularity(): List<RacePopularityCount>

    @Query("""
        SELECT p.class1, p.class2,
               COUNT(*) as total_games,
               SUM(CASE WHEN gp.is_winner = 1 THEN 1 ELSE 0 END) as wins
        FROM game_players gp
        INNER JOIN players p ON p.id = gp.player_id
        INNER JOIN games g ON g.id = gp.game_id
        WHERE g.is_active = 0
        GROUP BY p.class1, p.class2
    """)
    suspend fun getClassEfficiency(): List<ClassEfficiencyCount>

    @Query("""
        SELECT p.race1, p.race2,
               COUNT(*) as total_games,
               SUM(CASE WHEN gp.is_winner = 1 THEN 1 ELSE 0 END) as wins
        FROM game_players gp
        INNER JOIN players p ON p.id = gp.player_id
        INNER JOIN games g ON g.id = gp.game_id
        WHERE g.is_active = 0
        GROUP BY p.race1, p.race2
    """)
    suspend fun getRaceEfficiency(): List<RaceEfficiencyCount>

    @Query("""
        SELECT p.class1, p.class2, p.race1, p.race2,
               COUNT(*) as wins
        FROM game_players gp
        INNER JOIN players p ON p.id = gp.player_id
        INNER JOIN games g ON g.id = gp.game_id
        WHERE gp.is_winner = 1 AND g.is_active = 0
        GROUP BY p.class1, p.class2, p.race1, p.race2
        ORDER BY wins DESC
        LIMIT 10
    """)
    suspend fun getTopClassRaceCombos(): List<ClassRaceComboCount>
}

// ─── Data classes ────────────────────────────────────────────────────────────
data class GenderWinCount(val gender: String, val wins: Int)
data class GenderCount(val gender: String, val count: Int)
data class PlayerWinCount(val id: Long, val name: String, val gender: String, val wins: Int)
data class ClassWinCount(val class1: String?, val class2: String?, val wins: Int)
data class RaceWinCount(val race1: String?, val race2: String?, val wins: Int)
data class ClassPopularityCount(val class1: String?, val class2: String?, val games_count: Int)
data class RacePopularityCount(val race1: String?, val race2: String?, val games_count: Int)
data class ClassEfficiencyCount(val class1: String?, val class2: String?, val total_games: Int, val wins: Int)
data class RaceEfficiencyCount(val race1: String?, val race2: String?, val total_games: Int, val wins: Int)
data class ClassRaceComboCount(val class1: String?, val class2: String?, val race1: String?, val race2: String?, val wins: Int)
data class GameDurationRaw(val avg_duration: Double?, val total_games: Int, val min_duration: Long?, val max_duration: Long?)

// ─── LevelChangeDao ───────────────────────────────────────────────────────────
@Dao
interface LevelChangeDao {
    @Query("""
        SELECT lc.* FROM level_changes lc
        INNER JOIN game_players gp ON gp.id = lc.game_player_id
        WHERE gp.game_id = :gameId
        ORDER BY lc.timestamp DESC
    """)
    fun getChangesForGame(gameId: Long): Flow<List<LevelChangeEntity>>

    @Query("""
        SELECT lc.* FROM level_changes lc
        INNER JOIN game_players gp ON gp.id = lc.game_player_id
        WHERE gp.game_id = :gameId
        ORDER BY lc.timestamp DESC
        LIMIT 1
    """)
    suspend fun getLastChangeForGame(gameId: Long): LevelChangeEntity?

    @Insert
    suspend fun insertChange(change: LevelChangeEntity): Long

    @Delete
    suspend fun deleteChange(change: LevelChangeEntity)

    @Query("SELECT * FROM level_changes WHERE game_player_id = :gpId ORDER BY timestamp ASC")
    suspend fun getChangesForGamePlayer(gpId: Long): List<LevelChangeEntity>
}