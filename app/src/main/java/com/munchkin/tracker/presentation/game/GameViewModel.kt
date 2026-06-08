package com.munchkin.tracker.presentation.game

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.munchkin.tracker.data.repository.MunchkinRepository
import com.munchkin.tracker.domain.model.*
import com.munchkin.tracker.presentation.settings.settingsDataStore
import com.munchkin.tracker.voice.HotwordManager
import com.munchkin.tracker.voice.LLMParser
import com.munchkin.tracker.voice.VoiceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GameUiState(
    val activeGame: Game? = null,
    val players: List<GamePlayer> = emptyList(),
    val voiceState: VoiceState = VoiceState.SLEEPING,
    val recognizedText: String = "",
    val timerSeconds: Long = 0L,
    val snackbarMessage: String? = null,
    val lastFlash: Map<Long, Boolean> = emptyMap(),
    val lastDeltas: Map<Long, Int> = emptyMap(),
    val isLoading: Boolean = true
)

@HiltViewModel
class GameViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repo: MunchkinRepository,
    val voiceManager: VoiceManager,
    private val hotwordManager: HotwordManager,
    private val llmParser: LLMParser
) : ViewModel() {

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var gameStartMs = 0L
    private var ttsEnabled = true
    private var alwaysListenEnabled = false
    private val globalUndoStack = mutableListOf<suspend () -> Unit>()
    private val playerUndoStacks = mutableMapOf<Long, MutableList<suspend () -> Unit>>()

    init {
        voiceManager.init()
        observeActiveGame()
        observeVoice()
        observeSettings()
    }

    fun stopVoiceListening() {
        voiceManager.stopListening()
        _state.update { it.copy(voiceState = VoiceState.SLEEPING, recognizedText = "") }
        if (alwaysListenEnabled) hotwordManager.start()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            appContext.settingsDataStore.data.catch { emit(emptyPreferences()) }.collect { prefs ->
                ttsEnabled = prefs[booleanPreferencesKey("tts_enabled")] ?: true
                val listenEnabled = prefs[booleanPreferencesKey("always_listen")] ?: false
                if (listenEnabled != alwaysListenEnabled) {
                    alwaysListenEnabled = listenEnabled
                    if (listenEnabled) startHotwordDetection() else hotwordManager.stop()
                }
            }
        }
    }

    private fun startHotwordDetection() {
        hotwordManager.start()
        viewModelScope.launch {
            hotwordManager.hotwordDetected.collect { detected ->
                if (detected) {
                    Log.d("GameVM", "Hotword detected!")
                    hotwordManager.stop()
                    delay(800)
                    if (ttsEnabled) voiceManager.speak("Слушаю")
                    delay(500)
                    voiceManager.startListening()
                }
            }
        }
    }

    private fun observeActiveGame() {
        viewModelScope.launch {
            repo.getActiveGame().collect { game ->
                _state.update { it.copy(activeGame = game, isLoading = false) }
                if (game != null) {
                    gameStartMs = game.date
                    startTimer()
                    val players = repo.getGamePlayers(game.id).first()
                    _state.update { it.copy(players = players) }
                    voiceManager.updatePlayerNames(players.map { it.player.name })
                } else {
                    timerJob?.cancel()
                    _state.update { it.copy(players = emptyList(), lastDeltas = emptyMap()) }
                }
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                _state.update { it.copy(timerSeconds = (System.currentTimeMillis() - gameStartMs) / 1000) }
                delay(1000)
            }
        }
    }

    private fun observeVoice() {
        viewModelScope.launch {
            voiceManager.voiceState.collect { vs ->
                _state.update { it.copy(voiceState = vs) }
            }
        }
        viewModelScope.launch {
            voiceManager.recognizedText.collect { text ->
                _state.update { s -> s.copy(recognizedText = text) }
                if (text.isNotBlank()) {
                    processWithLLM(text)
                }
            }
        }
    }

    private suspend fun processWithLLM(text: String) {
        try {
            val players = _state.value.players.map {
                "${it.player.name} (сила ${it.player.power}, уровень ${it.currentLevel}, раса ${it.player.race1 ?: "нет"}, класс ${it.player.class1 ?: "нет"})"
            }
            val actions = llmParser.parseCommand(text, players)
            if (actions.isEmpty()) {
                Log.d("GameVM", "LLM returned no actions")
                restartHotwordIfNeeded()
                return
            }
            actions.forEach { action ->
                Log.d("GameVM", "Processing action: ${action.type} player=${action.player} value=${action.value}")
                var gp = findPlayerByName(action.player) ?: run {
                    Log.w("GameVM", "Player not found: ${action.player}")
                    return@forEach
                }
                when (action.type) {
                    "set_power" -> {
                        val power = (action.value as? Double)?.toInt() ?: return@forEach
                        val updated = gp.player.copy(power = power)
                        applyPlayerUpdate(gp.player.id, updated)
                        gp = gp.copy(player = updated)
                    }
                    "set_race" -> {
                        val race = action.value as? String ?: return@forEach
                        val updated = gp.player.copy(race1 = race.ifBlank { null })
                        applyPlayerUpdate(gp.player.id, updated)
                        gp = gp.copy(player = updated)
                    }
                    "set_race2" -> {
                        val race = action.value as? String ?: return@forEach
                        if (race.isNotBlank() && race == gp.player.race1) { Log.w("GameVM", "Skipping duplicate race2: $race"); return@forEach }
                        val updated = gp.player.copy(race2 = race.ifBlank { null })
                        applyPlayerUpdate(gp.player.id, updated)
                        gp = gp.copy(player = updated)
                    }
                    "set_class" -> {
                        val cls = action.value as? String ?: return@forEach
                        val updated = gp.player.copy(class1 = cls.ifBlank { null })
                        applyPlayerUpdate(gp.player.id, updated)
                        gp = gp.copy(player = updated)
                    }
                    "set_class2" -> {
                        val cls = action.value as? String ?: return@forEach
                        if (cls.isNotBlank() && cls == gp.player.class1) { Log.w("GameVM", "Skipping duplicate class2: $cls"); return@forEach }
                        val updated = gp.player.copy(class2 = cls.ifBlank { null })
                        applyPlayerUpdate(gp.player.id, updated)
                        gp = gp.copy(player = updated)
                    }
                    "level_change" -> {
                        val delta = (action.value as? Double)?.toInt() ?: return@forEach
                        changeLevel(gp, delta)
                    }
                    "set_level" -> {
                        val level = (action.value as? Double)?.toInt() ?: return@forEach
                        setLevel(gp, level)
                    }
                }
            }
            if (ttsEnabled) voiceManager.speak("Готово")
        } catch (e: Exception) {
            Log.e("GameVM", "LLM error: ${e.message}", e)
        } finally {
            restartHotwordIfNeeded()
        }
    }

    private suspend fun restartHotwordIfNeeded() {
        if (alwaysListenEnabled) {
            delay(500)
            hotwordManager.start()
        }
    }

    private fun findPlayerByName(name: String): GamePlayer? {
        return _state.value.players.find { it.player.name.equals(name, ignoreCase = true) }
    }

    fun undoLastAction() {
        val action = globalUndoStack.removeLastOrNull() ?: return
        viewModelScope.launch {
            action()
            val game = _state.value.activeGame ?: return@launch
            val players = repo.getGamePlayers(game.id).first()
            _state.update { state -> state.copy(players = players.map { gp -> gp.copy(lastDelta = state.lastDeltas[gp.id] ?: 0) }, snackbarMessage = "Отменено") }
        }
    }

    fun changeLevel(gamePlayer: GamePlayer, delta: Int) {
        val oldLevel = gamePlayer.currentLevel
        val newLevel = (oldLevel + delta).coerceIn(1, 10)
        if (newLevel == oldLevel) return
        val playerId = gamePlayer.player.id
        viewModelScope.launch {
            repo.updateLevel(gamePlayer.id, oldLevel, newLevel, "manual")
            val newPower = (gamePlayer.player.power + delta).coerceIn(0, 50)
            val updatedPlayer = gamePlayer.player.copy(power = newPower)
            repo.updatePlayer(updatedPlayer)
            flashCard(gamePlayer.id, delta > 0)
            _state.update { s -> s.copy(players = s.players.map { gp -> if (gp.id == gamePlayer.id) gp.copy(lastDelta = delta, currentLevel = newLevel, player = updatedPlayer) else gp }, lastDeltas = s.lastDeltas + (gamePlayer.id to delta), snackbarMessage = "${gamePlayer.player.name}, уровень $newLevel") }
            delay(2000)
            _state.update { s -> s.copy(lastDeltas = s.lastDeltas - gamePlayer.id) }
        }
        val undoAction: suspend () -> Unit = { repo.updateLevel(gamePlayer.id, newLevel, oldLevel, "undo"); repo.updatePlayer(gamePlayer.player.copy(power = gamePlayer.player.power - delta)) }
        globalUndoStack.add(undoAction)
        playerUndoStacks.getOrPut(playerId) { mutableListOf() }.add(undoAction)
    }

    fun setLevel(gamePlayer: GamePlayer, level: Int) = changeLevel(gamePlayer, level - gamePlayer.currentLevel)

    fun changePower(playerId: Long, delta: Int) {
        val gp = _state.value.players.find { it.player.id == playerId } ?: return
        val newPower = (gp.player.power + delta).coerceIn(0, 50)
        if (newPower == gp.player.power) return
        applyPlayerUpdate(playerId, gp.player.copy(power = newPower))
    }

    fun updatePlayerGender(playerId: Long, newGender: Gender) {
        val gp = _state.value.players.find { it.player.id == playerId } ?: return
        if (gp.player.gender == newGender) return
        applyPlayerUpdate(playerId, gp.player.copy(gender = newGender))
    }

    fun updatePlayerName(playerId: Long, newName: String) {
        val gp = _state.value.players.find { it.player.id == playerId } ?: return
        if (gp.player.name == newName) return
        applyPlayerUpdate(playerId, gp.player.copy(name = newName))
    }

    fun updatePlayerDetails(playerId: Long, power: Int, race1: String?, race2: String?, class1: String?, class2: String?) {
        val gp = _state.value.players.find { it.player.id == playerId } ?: return
        applyPlayerUpdate(playerId, gp.player.copy(power = power, race1 = race1, race2 = race2, class1 = class1, class2 = class2))
    }

    private fun applyPlayerUpdate(playerId: Long, updated: Player) {
        val oldPlayer = _state.value.players.find { it.player.id == playerId }?.player ?: return
        viewModelScope.launch {
            repo.updatePlayer(updated)
            _state.update { s -> s.copy(players = s.players.map { if (it.player.id == playerId) it.copy(player = updated) else it }) }
        }
        val undoAction: suspend () -> Unit = { repo.updatePlayer(oldPlayer) }
        globalUndoStack.add(undoAction)
        playerUndoStacks.getOrPut(playerId) { mutableListOf() }.add(undoAction)
    }

    fun startNewGame(playerIds: List<Long>, winLevel: Int = 10) {
        viewModelScope.launch {
            val gid = repo.startNewGame(winLevel)
            playerIds.forEach { repo.addPlayerToGame(gid, it) }
            globalUndoStack.clear(); playerUndoStacks.clear()
            _state.update { it.copy(lastDeltas = emptyMap()) }
        }
    }

    fun finishGame(selectedWinnerId: Long) {
        viewModelScope.launch {
            val g = _state.value.activeGame ?: return@launch
            val allPlayers = _state.value.players
            val durationMs = System.currentTimeMillis() - gameStartMs
            val winners = allPlayers.filter { it.currentLevel >= g.winLevel }
            if (winners.isNotEmpty()) repo.finishGame(g.id, winners.map { it.id }, durationMs)
            else repo.finishGame(g.id, listOf(selectedWinnerId), durationMs)
            timerJob?.cancel()
            _state.update { it.copy(snackbarMessage = "Игра завершена! 🏆") }
            delay(3000); clearSnackbar()
        }
    }

    fun addPlayerToCurrentGame(playerId: Long) {
        viewModelScope.launch { val g = _state.value.activeGame ?: return@launch; repo.addPlayerToGame(g.id, playerId) }
    }

    fun createAndAddPlayer(name: String, gender: Gender) {
        viewModelScope.launch {
            val pid = repo.insertPlayer(name, gender)
            val g = _state.value.activeGame
            if (g != null) repo.addPlayerToGame(g.id, pid)
        }
    }

    fun clearSnackbar() { _state.update { it.copy(snackbarMessage = null) } }

    fun startVoiceListening() {
        Log.d("GameVM", "startVoiceListening called, alwaysListenEnabled=$alwaysListenEnabled")
        if (alwaysListenEnabled) {
            hotwordManager.stop()
            Log.d("GameVM", "Hotword stopped, waiting 300ms...")
            viewModelScope.launch {
                delay(300)
                Log.d("GameVM", "Calling voiceManager.startListening()")
                voiceManager.startListening()
            }
        } else {
            Log.d("GameVM", "Calling voiceManager.startListening() directly")
            voiceManager.startListening()
        }
    }

    private fun flashCard(gpId: Long, isPositive: Boolean) {
        viewModelScope.launch { _state.update { it.copy(lastFlash = it.lastFlash + (gpId to isPositive)) }; delay(600); _state.update { it.copy(lastFlash = it.lastFlash - gpId) } }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
        hotwordManager.stop()
    }
}