package com.munchkin.tracker.data.repository

import com.munchkin.tracker.data.dao.*
import com.munchkin.tracker.data.entity.*
import com.munchkin.tracker.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MunchkinRepository @Inject constructor(
    private val playerDao: PlayerDao,
    private val gameDao: GameDao,
    private val gamePlayerDao: GamePlayerDao,
    private val levelChangeDao: LevelChangeDao
) {
    fun getAllPlayers(): Flow<List<Player>> =
        playerDao.getAllPlayers().map { list -> list.map { it.toDomain() } }

    suspend fun insertPlayer(name: String, gender: Gender): Long {
        val existing = playerDao.getPlayerByName(name.trim())
        if (existing != null) {
            return existing.id
        }
        return playerDao.insertPlayer(
            PlayerEntity(
                name = name.trim(),
                gender = gender.name.lowercase(),
                power = 1
            )
        )
    }

    suspend fun updatePlayer(player: Player) =
        playerDao.updatePlayer(player.toEntity())

    suspend fun updatePlayerGender(playerId: Long, newGender: Gender) {
        val player = playerDao.getPlayerById(playerId) ?: return
        playerDao.updatePlayer(player.copy(gender = newGender.name.lowercase()))
    }

    suspend fun deletePlayer(player: Player) =
        playerDao.deletePlayer(player.toEntity())

    fun getActiveGame(): Flow<Game?> =
        gameDao.getActiveGame().map { it?.toDomain() }

    fun getAllGames(): Flow<List<Game>> =
        gameDao.getAllGames().map { list -> list.map { it.toDomain() } }

    suspend fun startNewGame(winLevel: Int = 10): Long =
        gameDao.insertGame(GameEntity(winLevel = winLevel))

    suspend fun finishGame(gameId: Long, winnerIds: List<Long>, durationMs: Long) {
        gameDao.finishGame(gameId, durationMs)
        winnerIds.forEach { winnerId ->
            gamePlayerDao.setWinner(winnerId)
        }
    }

    suspend fun getActiveGameOnce(): Game? = gameDao.getActiveGameOnce()?.toDomain()

    fun getGamePlayers(gameId: Long): Flow<List<GamePlayer>> =
        gamePlayerDao.getGamePlayers(gameId).map { list ->
            list.map { gp ->
                val player = playerDao.getPlayerById(gp.playerId)?.toDomain()
                    ?: return@map null
                gp.toDomain(player)
            }.filterNotNull()
        }

    suspend fun addPlayerToGame(gameId: Long, playerId: Long): Long {
        val player = playerDao.getPlayerById(playerId)
        player?.let {
            playerDao.updatePlayer(
                it.copy(
                    race1 = "Человек",
                    race2 = null,
                    class1 = null,
                    class2 = null
                )
            )
        }
        return gamePlayerDao.insertGamePlayer(GamePlayerEntity(gameId = gameId, playerId = playerId))
    }

    suspend fun updateLevel(gamePlayerId: Long, oldLevel: Int, newLevel: Int, source: String) {
        gamePlayerDao.updateLevel(gamePlayerId, newLevel)
        levelChangeDao.insertChange(
            LevelChangeEntity(
                gamePlayerId = gamePlayerId,
                oldLevel = oldLevel,
                newLevel = newLevel,
                source = source
            )
        )
    }

    suspend fun findGamePlayerByName(gameId: Long, name: String): GamePlayer? {
        val gp = gamePlayerDao.findByPlayerName(gameId, "%$name%") ?: return null
        val player = playerDao.getPlayerById(gp.playerId)?.toDomain() ?: return null
        return gp.toDomain(player)
    }

    fun getLevelChanges(gameId: Long): Flow<List<LevelChange>> =
        levelChangeDao.getChangesForGame(gameId).map { list ->
            list.map { lc ->
                val gp = gamePlayerDao.getById(lc.gamePlayerId)
                val player = gp?.let { playerDao.getPlayerById(it.playerId) }
                LevelChange(
                    id = lc.id,
                    gamePlayerId = lc.gamePlayerId,
                    playerName = player?.name ?: "Неизвестный",
                    oldLevel = lc.oldLevel,
                    newLevel = lc.newLevel,
                    timestamp = lc.timestamp,
                    source = when (lc.source) {
                        "voice" -> LevelChangeSource.VOICE
                        "undo" -> LevelChangeSource.UNDO
                        else -> LevelChangeSource.MANUAL
                    }
                )
            }
        }

    suspend fun undoLastChange(gameId: Long) {
        val last = levelChangeDao.getLastChangeForGame(gameId) ?: return
        gamePlayerDao.updateLevel(last.gamePlayerId, last.oldLevel)
        levelChangeDao.deleteChange(last)
        levelChangeDao.insertChange(
            LevelChangeEntity(
                gamePlayerId = last.gamePlayerId,
                oldLevel = last.newLevel,
                newLevel = last.oldLevel,
                source = "undo"
            )
        )
    }

    suspend fun getStatsSummary(): StatsSummary {
        val genderWins = gamePlayerDao.getWinsByGender()
        val topRaw = gamePlayerDao.getTopPlayers()
        val genderDist = playerDao.getGenderDistribution()
        return StatsSummary(
            maleWins = genderWins.find { it.gender == "male" }?.wins ?: 0,
            femaleWins = genderWins.find { it.gender == "female" }?.wins ?: 0,
            maleCount = genderDist.find { it.gender == "male" }?.count ?: 0,
            femaleCount = genderDist.find { it.gender == "female" }?.count ?: 0,
            topPlayers = topRaw.map {
                TopPlayer(it.id, it.name, genderFromString(it.gender), it.wins)
            }
        )
    }

    suspend fun getWinsByClass(): Map<String, Int> {
        val raw = gamePlayerDao.getWinsByClass()
        val result = mutableMapOf<String, Int>()
        raw.forEach { entry ->
            listOfNotNull(entry.class1, entry.class2).forEach { cls ->
                result[cls] = (result[cls] ?: 0) + entry.wins
            }
        }
        return result
    }

    suspend fun getWinsByRace(): Map<String, Int> {
        val raw = gamePlayerDao.getWinsByRace()
        val result = mutableMapOf<String, Int>()
        raw.forEach { entry ->
            listOfNotNull(entry.race1, entry.race2).forEach { race ->
                result[race] = (result[race] ?: 0) + entry.wins
            }
        }
        return result
    }

    suspend fun getGenderDistribution(): Map<String, Int> {
        val raw = playerDao.getGenderDistribution()
        return raw.associate { it.gender to it.count }
    }

    suspend fun getClassPopularity(): Map<String, Int> {
        val raw = gamePlayerDao.getClassPopularity()
        val result = mutableMapOf<String, Int>()
        raw.forEach { entry ->
            listOfNotNull(entry.class1, entry.class2).forEach { cls ->
                result[cls] = (result[cls] ?: 0) + entry.games_count
            }
        }
        return result
    }

    suspend fun getRacePopularity(): Map<String, Int> {
        val raw = gamePlayerDao.getRacePopularity()
        val result = mutableMapOf<String, Int>()
        raw.forEach { entry ->
            listOfNotNull(entry.race1, entry.race2).forEach { race ->
                result[race] = (result[race] ?: 0) + entry.games_count
            }
        }
        return result
    }

    suspend fun getClassEfficiency(): Map<String, Float> {
        val raw = gamePlayerDao.getClassEfficiency()
        val totalGames = mutableMapOf<String, Int>()
        val totalWins = mutableMapOf<String, Int>()
        raw.forEach { entry ->
            listOfNotNull(entry.class1, entry.class2).forEach { cls ->
                totalGames[cls] = (totalGames[cls] ?: 0) + entry.total_games
                totalWins[cls] = (totalWins[cls] ?: 0) + entry.wins
            }
        }
        val result = mutableMapOf<String, Float>()
        totalGames.forEach { (cls, games) ->
            val wins = totalWins[cls] ?: 0
            result[cls] = if (games > 0) wins.toFloat() / games.toFloat() * 100f else 0f
        }
        return result
    }

    suspend fun getRaceEfficiency(): Map<String, Float> {
        val raw = gamePlayerDao.getRaceEfficiency()
        val totalGames = mutableMapOf<String, Int>()
        val totalWins = mutableMapOf<String, Int>()
        raw.forEach { entry ->
            listOfNotNull(entry.race1, entry.race2).forEach { race ->
                totalGames[race] = (totalGames[race] ?: 0) + entry.total_games
                totalWins[race] = (totalWins[race] ?: 0) + entry.wins
            }
        }
        val result = mutableMapOf<String, Float>()
        totalGames.forEach { (race, games) ->
            val wins = totalWins[race] ?: 0
            result[race] = if (games > 0) wins.toFloat() / games.toFloat() * 100f else 0f
        }
        return result
    }

    suspend fun getTopClassRaceCombos(): List<ClassRaceCombo> {
        val raw = gamePlayerDao.getTopClassRaceCombos()
        return raw.map { entry ->
            val classes = listOfNotNull(entry.class1, entry.class2).joinToString("/")
            val races = listOfNotNull(entry.race1, entry.race2).joinToString("/")
            ClassRaceCombo(
                classCombo = classes.ifEmpty { "Без класса" },
                raceCombo = races.ifEmpty { "Без расы" },
                wins = entry.wins
            )
        }
    }

    suspend fun getGameDurationStats(): GameDurationStats {
        val stats = gameDao.getGameDurationStats()
        return GameDurationStats(
            avgDurationMin = (stats.avg_duration ?: 0.0) / 60000.0,
            totalGames = stats.total_games,
            minDurationMin = (stats.min_duration ?: 0L) / 60000.0,
            maxDurationMin = (stats.max_duration ?: 0L) / 60000.0
        )
    }

    suspend fun getPlayerStats(playerId: Long): PlayerStats? {
        val player = playerDao.getPlayerById(playerId)?.toDomain() ?: return null
        val gamesForPlayer = gamePlayerDao.getGamesByPlayer(playerId)
        val wins = gamesForPlayer.count { it.isWinner }
        val maxLevel = gamesForPlayer.maxOfOrNull { it.currentLevel } ?: 1
        val avgFinalLevel = if (gamesForPlayer.isEmpty()) 1.0
        else gamesForPlayer.sumOf { it.currentLevel }.toDouble() / gamesForPlayer.size
        return PlayerStats(player, gamesForPlayer.size, wins, maxLevel, avgFinalLevel)
    }

    suspend fun getLevelProgressForPlayer(gamePlayerId: Long): List<Pair<Long, Int>> {
        return levelChangeDao.getChangesForGamePlayer(gamePlayerId).map {
            it.timestamp to it.newLevel
        }
    }

    suspend fun exportGamesCsv(): String {
        val sb = StringBuilder()
        sb.appendLine("id,date,duration_min,win_level,winner")
        val games = gameDao.getAllGames().first()
        games.forEach { game ->
            val players = gamePlayerDao.getGamePlayersOnce(game.id)
            val winner = players.find { it.isWinner }
            val winnerPlayer = winner?.let { playerDao.getPlayerById(it.playerId) }
            sb.appendLine("${game.id},${game.date},${game.duration / 60000},${game.winLevel},${winnerPlayer?.name ?: ""}")
        }
        return sb.toString()
    }

    suspend fun exportStatsCsv(): String {
        val sb = StringBuilder()
        sb.appendLine("player_id,name,gender,wins,total_games,max_level")
        val players = playerDao.getAllPlayers().first()
        players.forEach { p ->
            val stats = getPlayerStats(p.id)
            if (stats != null) {
                sb.appendLine("${p.id},${p.name},${p.gender},${stats.wins},${stats.totalGames},${stats.maxLevel}")
            }
        }
        return sb.toString()
    }

    private fun PlayerEntity.toDomain() = Player(
        id = id, name = name, gender = genderFromString(gender),
        power = power, race1 = race1, race2 = race2,
        class1 = class1, class2 = class2, createdAt = createdAt
    )

    private fun Player.toEntity() = PlayerEntity(
        id = id, name = name, gender = gender.name.lowercase(),
        power = power, race1 = race1, race2 = race2,
        class1 = class1, class2 = class2, createdAt = createdAt
    )

    private fun GameEntity.toDomain() = Game(id, date, duration, isActive, winLevel)

    private fun GamePlayerEntity.toDomain(player: Player) = GamePlayer(
        id = id, gameId = gameId, player = player,
        currentLevel = currentLevel, isWinner = isWinner
    )

    private fun genderFromString(s: String) = when (s.lowercase()) {
        "male" -> Gender.MALE
        "female" -> Gender.FEMALE
        else -> Gender.MALE
    }

    suspend fun deleteGame(gameId: Long) {
        gameDao.deleteGame(gameId)
    }
}