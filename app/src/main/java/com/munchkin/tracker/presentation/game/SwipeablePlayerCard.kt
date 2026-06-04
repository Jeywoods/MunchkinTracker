package com.munchkin.tracker.presentation.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.munchkin.tracker.domain.model.GamePlayer
import com.munchkin.tracker.domain.model.Gender
import com.munchkin.tracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeablePlayerCard(
    gamePlayer: GamePlayer,
    winLevel: Int,
    flashPositive: Boolean?,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onPowerIncrement: () -> Unit,
    onPowerDecrement: () -> Unit,
    onSwipeUndo: () -> Unit,
    onGenderChange: (Gender) -> Unit,
    onPlayerUpdate: (Int, String?, String?, String?, String?) -> Unit,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value -> if (value == SwipeToDismissBoxValue.StartToEnd) onSwipeUndo(); false },
        positionalThreshold = { it * 0.35f }
    )
    SwipeToDismissBox(
        state = dismissState, modifier = modifier,
        enableDismissFromStartToEnd = true, enableDismissFromEndToStart = false,
        backgroundContent = {
            val progress = dismissState.progress
            val scale by animateFloatAsState(if (progress > 0.1f) 1f else 0.85f, label = "icon_scale")
            Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(Secondary.copy(alpha = (progress * 2).coerceIn(0f, 0.25f))), contentAlignment = Alignment.CenterStart) {
                Row(modifier = Modifier.padding(start = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Undo, "Отмена", tint = Secondary, modifier = Modifier.scale(scale))
                    Text("Отмена", color = Secondary, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    ) {
        PlayerCard(
            gamePlayer = gamePlayer, winLevel = winLevel, flashPositive = flashPositive,
            onIncrement = onIncrement, onDecrement = onDecrement,
            onPowerIncrement = onPowerIncrement, onPowerDecrement = onPowerDecrement,
            onGenderChange = onGenderChange, onPlayerUpdate = onPlayerUpdate,
            onNameChange = onNameChange
        )
    }
}