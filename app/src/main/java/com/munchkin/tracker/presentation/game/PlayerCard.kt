package com.munchkin.tracker.presentation.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.munchkin.tracker.domain.model.*
import com.munchkin.tracker.ui.theme.*

@Composable
fun PlayerCard(
    gamePlayer: GamePlayer,
    winLevel: Int,
    flashPositive: Boolean?,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onPowerIncrement: () -> Unit,
    onPowerDecrement: () -> Unit,
    onGenderChange: (Gender) -> Unit,
    onPlayerUpdate: (Int, String?, String?, String?, String?) -> Unit,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val level = gamePlayer.currentLevel
    val isNearWin = level >= winLevel - 1
    val isWinLevel = level >= winLevel
    var showEditDialog by remember { mutableStateOf(false) }
    var isEditingName by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(gamePlayer.player.name) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val flashColor = remember { Animatable(Color.Transparent) }
    LaunchedEffect(flashPositive) {
        if (flashPositive != null) {
            val target = if (flashPositive) LevelUp.copy(alpha = 0.35f) else LevelDown.copy(alpha = 0.35f)
            flashColor.animateTo(target, animationSpec = tween(80))
            flashColor.animateTo(Color.Transparent, animationSpec = tween(500))
        }
    }

    val cardShape = RoundedCornerShape(20.dp)
    val borderBrush = when {
        isWinLevel -> Brush.linearGradient(listOf(GoldGlow, Secondary, GoldGlow))
        isNearWin -> Brush.linearGradient(listOf(Secondary.copy(alpha = 0.5f), Secondary.copy(alpha = 0.5f)))
        else -> Brush.linearGradient(listOf(OutlineBright, OutlineBright))
    }
    val borderWidth = if (isNearWin || isWinLevel) 2.dp else 1.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(flashColor.value)
            .border(borderWidth, borderBrush, cardShape)
            .background(
                if (isWinLevel) Brush.verticalGradient(listOf(SecondaryContainer, Surface))
                else Brush.verticalGradient(listOf(SurfaceVariant, Surface)),
                cardShape
            )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Row 1: Name + lastDelta + gender + edit ────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                val genderColor = when (gamePlayer.player.gender) {
                    Gender.MALE -> Primary
                    Gender.FEMALE -> Tertiary
                }
                Box(Modifier.size(10.dp).background(genderColor, androidx.compose.foundation.shape.CircleShape))
                Spacer(Modifier.width(8.dp))

                if (isEditingName) {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleMedium.copy(color = OnBackground),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (editedName.isNotBlank() && editedName != gamePlayer.player.name) {
                                onNameChange(editedName.trim())
                            }
                            isEditingName = false
                            focusManager.clearFocus()
                        }),
                        modifier = Modifier.weight(1f).height(48.dp).focusRequester(focusRequester),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = Primary)
                    )
                    LaunchedEffect(Unit) { focusRequester.requestFocus() }
                } else {
                    Text(
                        text = gamePlayer.player.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = OnBackground,
                        modifier = Modifier.weight(1f).clickable {
                            editedName = gamePlayer.player.name
                            isEditingName = true
                        }
                    )
                }

                // lastDelta
                val delta = gamePlayer.lastDelta
                if (delta != 0) {
                    val deltaColor = if (delta > 0) LevelUp else LevelDown
                    Text(
                        text = "${if (delta > 0) "+" else ""}$delta",
                        style = MaterialTheme.typography.labelMedium,
                        color = deltaColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(deltaColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }

                // Значок пола
                Text(
                    text = gamePlayer.player.gender.icon,
                    fontSize = 18.sp,
                    color = genderColor,
                    modifier = Modifier.clickable {
                        onGenderChange(if (gamePlayer.player.gender == Gender.MALE) Gender.FEMALE else Gender.MALE)
                    }.padding(4.dp)
                )

                Spacer(Modifier.width(4.dp))

                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Редактировать",
                    tint = OnSurfaceVariant,
                    modifier = Modifier.size(18.dp).clickable { showEditDialog = true }.padding(2.dp)
                )
            }

            // ── Row 2: Раса и класс ────────────────────────────────────────
            val races = listOfNotNull(gamePlayer.player.race1, gamePlayer.player.race2)
            val classes = listOfNotNull(gamePlayer.player.class1, gamePlayer.player.class2)
            if (races.isNotEmpty() || classes.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (races.isNotEmpty()) {
                        Text(text = "Раса: ${races.joinToString("/")}", style = MaterialTheme.typography.labelSmall, color = Secondary,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(SecondaryContainer.copy(alpha = 0.3f)).padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    if (classes.isNotEmpty()) {
                        Text(text = "Класс: ${classes.joinToString("/")}", style = MaterialTheme.typography.labelSmall, color = Primary,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(PrimaryContainer.copy(alpha = 0.3f)).padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }

            // ── Row 3: Level + Power ───────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Уровень", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(onClick = onDecrement, enabled = level > 1,
                            colors = ButtonDefaults.buttonColors(containerColor = LevelDown.copy(alpha = 0.12f), contentColor = LevelDown, disabledContainerColor = SurfaceVariant, disabledContentColor = OnSurfaceVariant),
                            contentPadding = PaddingValues(0.dp), modifier = Modifier.size(36.dp)) { Text("-1", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
                            .background(if (isWinLevel) Brush.radialGradient(listOf(SecondaryContainer, Background)) else Brush.radialGradient(listOf(SurfaceBright, Background)))) {
                            Text(text = level.toString(), fontSize = if (level >= 10) 24.sp else 28.sp, fontWeight = FontWeight.Black, color = if (isWinLevel) GoldGlow else OnBackground)
                        }
                        Button(onClick = onIncrement, enabled = level < 10,
                            colors = ButtonDefaults.buttonColors(containerColor = LevelUp.copy(alpha = 0.15f), contentColor = LevelUp, disabledContainerColor = SurfaceVariant, disabledContentColor = OnSurfaceVariant),
                            contentPadding = PaddingValues(0.dp), modifier = Modifier.size(36.dp)) { Text("+1", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    }
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Сила", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(onClick = onPowerDecrement, enabled = gamePlayer.player.power > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = LevelDown.copy(alpha = 0.12f), contentColor = LevelDown, disabledContainerColor = SurfaceVariant, disabledContentColor = OnSurfaceVariant),
                            contentPadding = PaddingValues(0.dp), modifier = Modifier.size(36.dp)) { Text("-1", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(SurfaceBright)) {
                            Text(text = "${gamePlayer.player.power}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OnBackground)
                        }
                        Button(onClick = onPowerIncrement, enabled = gamePlayer.player.power < 50,
                            colors = ButtonDefaults.buttonColors(containerColor = LevelUp.copy(alpha = 0.15f), contentColor = LevelUp, disabledContainerColor = SurfaceVariant, disabledContentColor = OnSurfaceVariant),
                            contentPadding = PaddingValues(0.dp), modifier = Modifier.size(36.dp)) { Text("+1", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    }
                }
            }

            val progress = (level.toFloat() / winLevel).coerceIn(0f, 1f)
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = when { isWinLevel -> GoldGlow; isNearWin -> Secondary; else -> Primary }, trackColor = Outline)
        }
    }

    if (showEditDialog) {
        PlayerEditDialog(player = gamePlayer.player,
            onSave = { power, r1, r2, c1, c2 -> onPlayerUpdate(power, r1, r2, c1, c2); showEditDialog = false },
            onDismiss = { showEditDialog = false })
    }
}

@Composable
private fun PlayerEditDialog(player: Player, onSave: (Int, String?, String?, String?, String?) -> Unit, onDismiss: () -> Unit) {
    var race1 by remember { mutableStateOf(player.race1 ?: "") }
    var race2 by remember { mutableStateOf(player.race2 ?: "") }
    var class1 by remember { mutableStateOf(player.class1 ?: "") }
    var class2 by remember { mutableStateOf(player.class2 ?: "") }
    val availableRaces = PlayerRace.entries.map { it.label }
    val availableClasses = PlayerClass.entries.map { it.label }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceBright,
        modifier = Modifier.fillMaxWidth(),
        title = { Text("Параметры: ${player.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // ── Раса 1 ──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Раса 1:", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                    if (race1.isNotBlank() && race1 != "Человек") {
                        TextButton(onClick = { race1 = "Человек" }) {
                            Text("Убрать", color = Primary, fontSize = 14.sp)
                        }
                    }
                }
                RaceClassDropdown(
                    selected = race1,
                    options = availableRaces.filter { it != race2 || it.isBlank() },
                    onSelect = { race1 = it },
                    label = "Выберите расу"
                )

                // ── Раса 2 ──
                if (race1.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Раса 2:", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                        if (race2.isNotBlank()) {
                            TextButton(onClick = { race2 = "" }) {
                                Text("Убрать", color = Primary, fontSize = 14.sp)
                            }
                        }
                    }
                    RaceClassDropdown(
                        selected = race2,
                        options = availableRaces.filter { it != race1 || it.isBlank() },
                        onSelect = { race2 = it },
                        label = "Вторая раса"
                    )
                }

                // ── Класс 1 ──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Класс 1:", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                    if (class1.isNotBlank()) {
                        TextButton(onClick = {
                            class1 = class2
                            class2 = ""
                        }) {
                            Text("Убрать", color = Primary, fontSize = 14.sp)
                        }
                    }
                }
                RaceClassDropdown(
                    selected = class1,
                    options = availableClasses.filter { it != class2 || it.isBlank() },
                    onSelect = { class1 = it },
                    label = "Выберите класс"
                )

                // ── Класс 2 ──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Класс 2:", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                    if (class2.isNotBlank()) {
                        TextButton(onClick = { class2 = "" }) {
                            Text("Убрать", color = Primary, fontSize = 14.sp)
                        }
                    }
                }
                RaceClassDropdown(
                    selected = class2,
                    options = availableClasses.filter { it != class1 || it.isBlank() },
                    onSelect = { class2 = it },
                    label = "Второй класс"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    player.power,
                    race1.ifBlank { null },
                    race2.ifBlank { null },
                    class1.ifBlank { null },
                    class2.ifBlank { null }
                )
            }) {
                Text("Сохранить", color = Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = OnSurfaceVariant) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RaceClassDropdown(
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = modifier.menuAnchor(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}