package com.munchkin.tracker.presentation.players

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.munchkin.tracker.domain.model.Gender
import com.munchkin.tracker.domain.model.Player
import com.munchkin.tracker.presentation.components.AppTopBar
import com.munchkin.tracker.presentation.components.ConfirmDialog
import com.munchkin.tracker.presentation.components.MagicCircleBackground
import com.munchkin.tracker.presentation.navigation.Routes
import com.munchkin.tracker.ui.theme.*

@Composable
fun PlayerManagementScreen(
    navController: NavController,
    vm: PlayerManagementViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var playerToDelete by remember { mutableStateOf<Player?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar("Все игроки")

            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                MagicCircleBackground(alpha = 0.5f)

                if (state.players.isEmpty() && !state.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("👥", fontSize = 48.sp)
                            Text("Нет игроков", color = OnSurfaceVariant)
                            Text("Нажмите + для добавления", color = OnSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.players, key = { it.id }) { player ->
                            PlayerRow(
                                player = player,
                                onDetail = { navController.navigate(Routes.playerDetail(player.id)) },
                                onDelete = { playerToDelete = player }
                            )
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = Primary,
            contentColor = Background
        ) {
            Icon(Icons.Default.PersonAdd, "Добавить")
        }
    }

    if (showAddDialog) {
        QuickAddPlayerDialog(
            onAdd = { name, gender -> vm.addPlayer(name, gender); showAddDialog = false },
            onDismiss = { showAddDialog = false }
        )
    }

    if (playerToDelete != null) {
        ConfirmDialog(
            title = "Удалить игрока",
            message = "Удалить «${playerToDelete!!.name}»? Это действие нельзя отменить.",
            confirmText = "Удалить",
            onConfirm = { vm.deletePlayer(playerToDelete!!); playerToDelete = null },
            onDismiss = { playerToDelete = null }
        )
    }
}

@Composable
private fun PlayerRow(player: Player, onDetail: () -> Unit, onDelete: () -> Unit) {
    val genderColor = when (player.gender) { Gender.MALE -> Primary; Gender.FEMALE -> Tertiary }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape)
                .background(genderColor.copy(alpha = 0.15f))
                .border(1.dp, genderColor.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(player.name.take(1).uppercase(), fontWeight = FontWeight.Black, color = genderColor, fontSize = 20.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(player.name, style = MaterialTheme.typography.titleSmall, color = OnBackground)
        }
        IconButton(onClick = onDetail) { Icon(Icons.Default.BarChart, "Статистика", tint = Primary) }
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Удалить", tint = LevelDown.copy(alpha = 0.7f)) }
    }
}

@Composable
private fun QuickAddPlayerDialog(onAdd: (String, Gender) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf(Gender.MALE) }

    AlertDialog(onDismissRequest = onDismiss, containerColor = SurfaceBright, title = { Text("Новый игрок") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Имя") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary))
                Text("Пол", style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(Gender.MALE, Gender.FEMALE).forEach { g ->
                        FilterChip(gender == g, { gender = g }, { Text("${g.icon} ${g.label}") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = (if (g == Gender.MALE) Primary else Tertiary).copy(alpha = 0.2f), selectedLabelColor = if (g == Gender.MALE) Primary else Tertiary))
                    }
                }
            }
        },
        confirmButton = { TextButton({ if (name.isNotBlank()) onAdd(name.trim(), gender) }, enabled = name.isNotBlank()) { Text("Добавить", color = Primary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = OnSurfaceVariant) } })
}