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
import com.munchkin.tracker.voice.VoiceCommand
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
    @ApplicationContext private val context: Context,
    private val repo: MunchkinRepository,
    val voiceManager: VoiceManager,
    private val hotwordManager: HotwordManager
) : ViewModel() {

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var playersJob: Job? = null
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

    private fun observeSettings() {
        viewModelScope.launch {
            context.settingsDataStore.data.catch { emit(emptyPreferences()) }.collect { prefs ->
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
                    hotwordManager.stop(); delay(800)
                    if (ttsEnabled) voiceManager.speak("Слушаю"); delay(500)
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
                    observePlayers(game.id)
                } else {
                    timerJob?.cancel()
                    playersJob?.cancel()
                    _state.update { it.copy(players = emptyList(), lastDeltas = emptyMap()) }
                }
            }
        }
    }

    private fun observePlayers(gameId: Long) {
        playersJob?.cancel()
        playersJob = viewModelScope.launch {
            repo.getGamePlayers(gameId).collect { players ->
                _state.update { state ->
                    state.copy(players = players.map { gp ->
                        gp.copy(lastDelta = state.lastDeltas[gp.id] ?: 0)
                    })
                }
                voiceManager.updatePlayerNames(players.map { it.player.name })
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
                if (vs == VoiceState.SLEEPING || vs == VoiceState.ERROR) {
                    if (alwaysListenEnabled) { delay(500); hotwordManager.start() }
                }
            }
        }
        viewModelScope.launch { voiceManager.recognizedText.collect { _state.update { s -> s.copy(recognizedText = it) } } }
        viewModelScope.launch {
            voiceManager.command.collect { cmd ->
                if (cmd != null) {
                    handleVoiceCommand(cmd)
                    voiceManager.consumeCommand()
                }
            }
        }
    }

    fun undoLastAction() {
        val action = globalUndoStack.removeLastOrNull() ?: return
        viewModelScope.launch {
            action()
            val game = _state.value.activeGame ?: return@launch
            val players = repo.getGamePlayers(game.id).first()
            _state.update { state ->
                state.copy(
                    players = players.map { gp ->
                        gp.copy(lastDelta = state.lastDeltas[gp.id] ?: 0)
                    },
                    snackbarMessage = "Отменено"
                )
            }
        }
    }

    fun undoLastPlayerAction(playerId: Long) {
        val stack = playerUndoStacks[playerId] ?: return
        val action = stack.removeLastOrNull() ?: return
        globalUndoStack.remove(action)
        viewModelScope.launch {
            action()
            val game = _state.value.activeGame ?: return@launch
            val players = repo.getGamePlayers(game.id).first()
            _state.update { state ->
                state.copy(
                    players = players.map { gp ->
                        gp.copy(lastDelta = state.lastDeltas[gp.id] ?: 0)
                    },
                    lastDeltas = state.lastDeltas - playerId,
                    snackbarMessage = "Отменено"
                )
            }
        }
    }


    private fun undoGlobal() {
        val action = globalUndoStack.removeLastOrNull() ?: return
        viewModelScope.launch { action() }
    }

    fun changeLevel(gamePlayer: GamePlayer, delta: Int) {
        val oldLevel = gamePlayer.currentLevel
        val newLevel = (oldLevel + delta).coerceIn(1, 10)
        if (newLevel == oldLevel) return
        val playerId = gamePlayer.player.id
        viewModelScope.launch {
            // Обновляем уровень
            repo.updateLevel(gamePlayer.id, oldLevel, newLevel, "manual")

            // Обновляем силу на ту же дельту
            val newPower = (gamePlayer.player.power + delta).coerceIn(0, 50)
            val updatedPlayer = gamePlayer.player.copy(power = newPower)
            repo.updatePlayer(updatedPlayer)

            flashCard(gamePlayer.id, delta > 0)
            _state.update { s -> s.copy(
                players = s.players.map { gp ->
                    if (gp.id == gamePlayer.id) {
                        gp.copy(lastDelta = delta, currentLevel = newLevel, player = updatedPlayer)
                    } else gp
                },
                lastDeltas = s.lastDeltas + (gamePlayer.id to delta),
                snackbarMessage = "${gamePlayer.player.name}, уровень $newLevel"
            )}
            delay(2000)
            _state.update { s -> s.copy(lastDeltas = s.lastDeltas - gamePlayer.id) }
        }
        val undoAction: suspend () -> Unit = {
            repo.updateLevel(gamePlayer.id, newLevel, oldLevel, "undo")
            repo.updatePlayer(gamePlayer.player.copy(power = gamePlayer.player.power - delta))
        }
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
            _state.update { s ->
                s.copy(players = s.players.map {
                    if (it.player.id == playerId) it.copy(player = updated) else it
                })
            }
        }
        val undoAction: suspend () -> Unit = { repo.updatePlayer(oldPlayer) }
        globalUndoStack.add(undoAction)
        playerUndoStacks.getOrPut(playerId) { mutableListOf() }.add(undoAction)
    }

    fun startNewGame(playerIds: List<Long>, winLevel: Int = 10) {
        viewModelScope.launch {
            val gid = repo.startNewGame(winLevel)
            playerIds.forEach { playerId ->
                repo.addPlayerToGame(gid, playerId)
            }
            globalUndoStack.clear()
            playerUndoStacks.clear()
            _state.update { it.copy(lastDeltas = emptyMap()) }
        }
    }

    fun finishGame(selectedWinnerId: Long) {
        viewModelScope.launch {
            val g = _state.value.activeGame ?: return@launch
            val allPlayers = _state.value.players
            val durationMs = System.currentTimeMillis() - gameStartMs

            // Находим всех игроков с уровнем >= winLevel
            val winners = allPlayers.filter { it.currentLevel >= g.winLevel }

            if (winners.isNotEmpty()) {
                // Все с 10 уровнем — победители
                repo.finishGame(g.id, winners.map { it.id }, durationMs)
            } else {
                // Выбранный победитель
                repo.finishGame(g.id, listOf(selectedWinnerId), durationMs)
            }

            timerJob?.cancel()
            _state.update { it.copy(snackbarMessage = "Игра завершена! 🏆") }
            delay(3000)
            clearSnackbar()
        }
    }

    fun addPlayerToCurrentGame(playerId: Long) {
        viewModelScope.launch {
            val g = _state.value.activeGame ?: return@launch
            repo.addPlayerToGame(g.id, playerId)
        }
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
        if (alwaysListenEnabled) hotwordManager.stop()
        voiceManager.startListening()
    }

    private suspend fun handleVoiceCommand(cmd: VoiceCommand) {
        val game = _state.value.activeGame ?: return
        when (cmd) {
            is VoiceCommand.LevelChange -> {
                val gp = repo.findGamePlayerByName(game.id, cmd.playerName)
                if (gp != null) {
                    val newLevel = (gp.currentLevel + cmd.delta).coerceIn(1, 10)
                    changeLevel(gp, cmd.delta)
                    if (ttsEnabled) voiceManager.speak("${gp.player.name}, уровень $newLevel")
                }
            }
            is VoiceCommand.SetLevel -> {
                val gp = repo.findGamePlayerByName(game.id, cmd.playerName)
                if (gp != null) {
                    setLevel(gp, cmd.level)
                    if (ttsEnabled) voiceManager.speak("${gp.player.name}, уровень ${cmd.level}")
                }
            }
            is VoiceCommand.EndGame -> {
                val w = cmd.winnerName?.let { repo.findGamePlayerByName(game.id, it) }
                if (w != null) finishGame(w.id)
                if (ttsEnabled) voiceManager.speak("Игра завершена!")
            }
            VoiceCommand.NewGame -> _state.update { it.copy(snackbarMessage = "Скажите: выберите игроков и начните игру") }
            VoiceCommand.Undo -> undoLastAction()
            VoiceCommand.Unknown -> _state.update { it.copy(snackbarMessage = "Команда не распознана") }
        }
    }

    private fun flashCard(gpId: Long, isPositive: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(lastFlash = it.lastFlash + (gpId to isPositive)) }
            delay(600)
            _state.update { it.copy(lastFlash = it.lastFlash - gpId) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
        hotwordManager.stop()
    }
}