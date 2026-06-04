package com.munchkin.tracker.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.munchkin.tracker.data.repository.MunchkinRepository
import com.munchkin.tracker.domain.model.LevelChange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repo: MunchkinRepository
) : ViewModel() {

    val changes: Flow<List<LevelChange>> = repo.getActiveGame().flatMapLatest { game ->
        if (game != null) repo.getLevelChanges(game.id) else emptyFlow()
    }
}
