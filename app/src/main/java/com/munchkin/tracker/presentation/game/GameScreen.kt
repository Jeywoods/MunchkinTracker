package com.munchkin.tracker.presentation.game

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.munchkin.tracker.domain.model.Gender
import com.munchkin.tracker.presentation.components.AppTopBar
import com.munchkin.tracker.presentation.components.GameTimer
import com.munchkin.tracker.presentation.components.NotificationCard
import com.munchkin.tracker.presentation.components.VoiceIndicator
import com.munchkin.tracker.presentation.players.PlayerManagementViewModel
import com.munchkin.tracker.ui.theme.*

@Composable
fun GameScreen(
    navController: NavController,
    vm: GameViewModel = hiltViewModel(),
    playerVm: PlayerManagementViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val playerState by playerVm.state.collectAsStateWithLifecycle()
    val amplitude by vm.voiceManager.amplitude.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showAddPlayer by remember { mutableStateOf(false) }
    var showEndGame by remember { mutableStateOf(false) }
    var showNewGameConfirm by remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) vm.startVoiceListening() }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = "Манчкин",
                actions = {
                    if (state.activeGame != null) {
                        GameTimer(state.timerSeconds)
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { vm.undoLastAction() }) {
                            Icon(Icons.AutoMirrored.Filled.Undo, "Отменить", tint = OnSurfaceVariant)
                        }
                    }
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    VoiceIndicator(
                        voiceState = state.voiceState,
                        recognizedText = state.recognizedText,
                        amplitude = amplitude,
                        onMicClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                vm.startVoiceListening()
                            } else {
                                permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (state.activeGame == null && !state.isLoading) {
                    item { NoGamePlaceholder(onStart = { showNewGameConfirm = true }) }
                }

                items(state.players, key = { it.id }) { gp ->
                    val flash = state.lastFlash[gp.id]
                    SwipeablePlayerCard(
                        gamePlayer = gp, winLevel = state.activeGame?.winLevel ?: 10, flashPositive = flash,
                        onIncrement = { vm.changeLevel(gp, 1) }, onDecrement = { vm.changeLevel(gp, -1) },
                        onPowerIncrement = { vm.changePower(gp.player.id, 1) },
                        onPowerDecrement = { vm.changePower(gp.player.id, -1) },
                        onSwipeUndo = { vm.undoLastPlayerAction(gp.player.id) },
                        onGenderChange = { vm.updatePlayerGender(gp.player.id, it) },
                        onPlayerUpdate = { p, r1, r2, c1, c2 -> vm.updatePlayerDetails(gp.player.id, p, r1, r2, c1, c2) },
                        onNameChange = { vm.updatePlayerName(gp.player.id, it) }
                    )
                }

                if (state.activeGame != null) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { showAddPlayer = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary), border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.5f))) {
                                Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Добавить игрока")
                            }
                            Button(onClick = { showEndGame = true }, colors = ButtonDefaults.buttonColors(containerColor = Secondary, contentColor = Background)) { Text("🏆", fontSize = 18.sp) }
                        }
                    }
                }
            }
        }

        NotificationCard(
            message = state.snackbarMessage ?: "",
            isVisible = state.snackbarMessage != null,
            onDismiss = { vm.clearSnackbar() },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showAddPlayer) {
        AddPlayerDialog(existingPlayers = playerState.players, activePlayerIds = state.players.map { it.player.id }.toSet(),
            onAddExisting = { pid -> if (state.activeGame != null) vm.addPlayerToCurrentGame(pid) else showNewGameConfirm = true },
            onCreateNew = { n, g -> vm.createAndAddPlayer(n, g) }, onDismiss = { showAddPlayer = false })
    }
    if (showEndGame && state.players.isNotEmpty()) {
        EndGameDialog(players = state.players, winLevel = state.activeGame?.winLevel ?: 10,
            onConfirm = { vm.finishGame(it); showEndGame = false }, onDismiss = { showEndGame = false })
    }
    if (showNewGameConfirm) {
        NewGameSetupDialog(allPlayers = playerState.players,
            onStart = { ids, wl -> vm.startNewGame(ids, wl); showNewGameConfirm = false },
            onCreatePlayer = { n, g -> playerVm.addPlayer(n, g) }, onDismiss = { showNewGameConfirm = false })
    }
}

@Composable
private fun NoGamePlaceholder(onStart: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.verticalGradient(listOf(PrimaryContainer.copy(alpha = 0.3f), Background))).padding(40.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("⚔️", fontSize = 56.sp)
            Text("Нет активной игры", style = MaterialTheme.typography.headlineSmall, color = OnBackground)
            Text("Нажмите кнопку ниже, чтобы начать новую игру", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
            Button(onClick = onStart, colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Background)) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Новая игра", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun NewGameSetupDialog(
    allPlayers: List<com.munchkin.tracker.domain.model.Player>,
    onStart: (List<Long>, Int) -> Unit,
    onCreatePlayer: (String, Gender) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var winLevel by remember { mutableIntStateOf(10) }
    var newPlayerName by remember { mutableStateOf("") }
    var newPlayerGender by remember { mutableStateOf(Gender.MALE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceBright,
        modifier = Modifier.fillMaxWidth(),
        title = { Text("⚔️ Новая игра") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Уровень победы: $winLevel", color = OnSurfaceVariant)
                Slider(
                    value = winLevel.toFloat(),
                    onValueChange = { value -> winLevel = value.toInt() },
                    valueRange = 5f..15f,
                    steps = 9,
                    colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary)
                )
                HorizontalDivider(color = Outline)
                Text("Выберите игроков:", color = OnSurfaceVariant)
                allPlayers.forEach { player ->
                    val isSelected = player.id in selectedIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) PrimaryContainer else SurfaceVariant)
                            .clickable {
                                selectedIds = if (isSelected) selectedIds - player.id
                                else selectedIds + player.id
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                selectedIds = if (checked) selectedIds + player.id
                                else selectedIds - player.id
                            },
                            colors = CheckboxDefaults.colors(checkedColor = Primary)
                        )
                        Text(player.name, modifier = Modifier.weight(1f), color = OnBackground)
                        Text(player.gender.icon, color = OnSurfaceVariant)
                    }
                }
                HorizontalDivider(color = Outline)
                Text("Добавить нового:", color = OnSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        newPlayerName, { newPlayerName = it },
                        label = { Text("Имя") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary)
                    )
                    IconButton(onClick = {
                        if (newPlayerName.isNotBlank()) {
                            onCreatePlayer(newPlayerName.trim(), newPlayerGender)
                            newPlayerName = ""
                        }
                    }) {
                        Icon(Icons.Default.Add, "Добавить", tint = Primary)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onStart(selectedIds.toList(), winLevel) },
                enabled = selectedIds.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Background)
            ) {
                Text("Начать игру")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = OnSurfaceVariant) } }
    )
}