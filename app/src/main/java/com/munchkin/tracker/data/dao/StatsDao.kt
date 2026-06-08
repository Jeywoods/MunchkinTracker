package com.munchkin.tracker.data.dao

import androidx.room.*

@Dao
interface StatsDao {
    @Query("SELECT p.gender, COUNT(*) as wins FROM game_players gp INNER JOIN players p ON p.id = gp.player_id INNER JOIN games g ON g.id = gp.game_id WHERE gp.is_winner = 1 AND g.is_active = 0 GROUP BY p.gender")
    suspend fun getWinsByGender(): List<GenderWinCount>

    @Query("SELECT p.id, p.name, p.gender, COUNT(*) as wins FROM game_players gp INNER JOIN players p ON p.id = gp.player_id INNER JOIN games g ON g.id = gp.game_id WHERE gp.is_winner = 1 AND g.is_active = 0 GROUP BY p.id ORDER BY wins DESC LIMIT 10")
    suspend fun getTopPlayers(): List<PlayerWinCount>

    @Query("SELECT p.class1, p.class2, COUNT(*) as wins FROM game_players gp INNER JOIN players p ON p.id = gp.player_id INNER JOIN games g ON g.id = gp.game_id WHERE gp.is_winner = 1 AND g.is_active = 0 GROUP BY p.class1, p.class2")
    suspend fun getWinsByClass(): List<ClassWinCount>

    @Query("SELECT p.race1, p.race2, COUNT(*) as wins FROM game_players gp INNER JOIN players p ON p.id = gp.player_id INNER JOIN games g ON g.id = gp.game_id WHERE gp.is_winner = 1 AND g.is_active = 0 GROUP BY p.race1, p.race2")
    suspend fun getWinsByRace(): List<RaceWinCount>

    @Query("SELECT p.class1, p.class2, COUNT(*) as games_count FROM game_players gp INNER JOIN players p ON p.id = gp.player_id INNER JOIN games g ON g.id = gp.game_id WHERE g.is_active = 0 GROUP BY p.class1, p.class2")
    suspend fun getClassPopularity(): List<ClassPopularityCount>

    @Query("SELECT p.race1, p.race2, COUNT(*) as games_count FROM game_players gp INNER JOIN players p ON p.id = gp.player_id INNER JOIN games g ON g.id = gp.game_id WHERE g.is_active = 0 GROUP BY p.race1, p.race2")
    suspend fun getRacePopularity(): List<RacePopularityCount>

    @Query("SELECT p.class1, p.class2, COUNT(*) as total_games, SUM(CASE WHEN gp.is_winner = 1 THEN 1 ELSE 0 END) as wins FROM game_players gp INNER JOIN players p ON p.id = gp.player_id INNER JOIN games g ON g.id = gp.game_id WHERE g.is_active = 0 GROUP BY p.class1, p.class2")
    suspend fun getClassEfficiency(): List<ClassEfficiencyCount>

    @Query("SELECT p.race1, p.race2, COUNT(*) as total_games, SUM(CASE WHEN gp.is_winner = 1 THEN 1 ELSE 0 END) as wins FROM game_players gp INNER JOIN players p ON p.id = gp.player_id INNER JOIN games g ON g.id = gp.game_id WHERE g.is_active = 0 GROUP BY p.race1, p.race2")
    suspend fun getRaceEfficiency(): List<RaceEfficiencyCount>

    @Query("SELECT p.class1, p.class2, p.race1, p.race2, COUNT(*) as wins FROM game_players gp INNER JOIN players p ON p.id = gp.player_id INNER JOIN games g ON g.id = gp.game_id WHERE gp.is_winner = 1 AND g.is_active = 0 GROUP BY p.class1, p.class2, p.race1, p.race2 ORDER BY wins DESC LIMIT 10")
    suspend fun getTopClassRaceCombos(): List<ClassRaceComboCount>
}