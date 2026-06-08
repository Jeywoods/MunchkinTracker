package com.munchkin.tracker.presentation.players

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.munchkin.tracker.data.repository.MunchkinRepository
import com.munchkin.tracker.domain.model.Gender
import com.munchkin.tracker.domain.model.Player
import com.munchkin.tracker.domain.model.PlayerStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerMgmtState(
    val players: List<Player> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class PlayerManagementViewModel @Inject constructor(
    private val repo: MunchkinRepository
) : ViewModel() {
    private val _state = MutableStateFlow(PlayerMgmtState())
    val state: StateFlow<PlayerMgmtState> = _state.asStateFlow()

    private val _detail = MutableStateFlow<PlayerStats?>(null)
    val detail: StateFlow<PlayerStats?> = _detail.asStateFlow()

    init { loadPlayers() }

    private fun loadPlayers() {
        viewModelScope.launch {
            repo.getAllPlayers().collect { players ->
                _state.update { it.copy(players = players, isLoading = false) }
            }
        }
    }

    fun addPlayer(name: String, gender: Gender) {
        viewModelScope.launch { repo.insertPlayer(name, gender) }
    }

    fun deletePlayer(player: Player) {
        viewModelScope.launch { repo.deletePlayer(player) }
    }

    fun loadDetail(playerId: Long) {
        viewModelScope.launch {
            _detail.value = repo.getPlayerStats(playerId)
        }
    }
}
