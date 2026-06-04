package com.munchkin.tracker.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.munchkin.tracker.domain.model.LevelChange
import com.munchkin.tracker.domain.model.LevelChangeSource
import com.munchkin.tracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    vm: HistoryViewModel = hiltViewModel()
) {
    val changes by vm.changes.collectAsStateWithLifecycle(emptyList())

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("История изменений") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = OnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { padding ->
        if (changes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Нет записей для текущей игры", color = OnSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(changes, key = { it.id }) { change ->
                    LevelChangeItem(change)
                }
            }
        }
    }
}

@Composable
private fun LevelChangeItem(change: LevelChange) {
    val isPositive = change.newLevel > change.oldLevel
    val isUndo = change.source == LevelChangeSource.UNDO
    val delta = change.newLevel - change.oldLevel
    val deltaColor = when {
        isUndo -> OnSurfaceVariant
        isPositive -> LevelUp
        else -> LevelDown
    }
    val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Delta indicator
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(deltaColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when {
                    isUndo -> "↩"
                    isPositive -> "+$delta"
                    else -> "$delta"
                },
                fontWeight = FontWeight.Bold,
                color = deltaColor
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                change.playerName,
                style = MaterialTheme.typography.titleSmall,
                color = OnBackground
            )
            Text(
                "${change.oldLevel} → ${change.newLevel}",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                sdf.format(Date(change.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant
            )
            Text(
                change.source.label,
                style = MaterialTheme.typography.labelSmall,
                color = when (change.source) {
                    LevelChangeSource.VOICE -> VoiceActive
                    LevelChangeSource.UNDO -> OnSurfaceVariant
                    LevelChangeSource.MANUAL -> OnSurfaceVariant
                }
            )
        }
    }
}
