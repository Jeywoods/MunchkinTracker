package com.munchkin.tracker.presentation.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.munchkin.tracker.domain.model.Gender
import com.munchkin.tracker.domain.model.Player
import com.munchkin.tracker.ui.theme.*

@Composable
fun AddPlayerDialog(
    existingPlayers: List<Player>,
    activePlayerIds: Set<Long>,
    onAddExisting: (Long) -> Unit,
    onCreateNew: (String, Gender) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf(Gender.MALE) }
    var tab by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceBright,
        modifier = Modifier.fillMaxWidth(),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Добавить игрока", modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = OnSurfaceVariant)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Из списка", "Новый").forEachIndexed { idx, label ->
                        val selected = tab == idx
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) Primary else Color.Transparent)
                                .clickable { tab = idx }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) Background else OnSurfaceVariant
                            )
                        }
                    }
                }

                if (tab == 0) {
                    val available = existingPlayers.filter { it.id !in activePlayerIds }
                    if (available.isEmpty()) {
                        Text("Нет доступных игроков", color = OnSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            available.forEach { player ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SurfaceVariant)
                                        .clickable { onAddExisting(player.id); onDismiss() }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val genderColor = when (player.gender) {
                                        Gender.MALE -> Primary
                                        Gender.FEMALE -> Tertiary
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(genderColor, androidx.compose.foundation.shape.CircleShape)
                                    )
                                    Text(player.name, style = MaterialTheme.typography.bodyLarge, color = OnBackground, modifier = Modifier.weight(1f))
                                    Text(player.gender.label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Имя игрока") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            focusedLabelColor = Primary
                        )
                    )

                    Text("Пол", style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(Gender.MALE, Gender.FEMALE).forEach { g ->
                            val selected = gender == g
                            val gColor = when (g) {
                                Gender.MALE -> Primary
                                Gender.FEMALE -> Tertiary
                            }
                            FilterChip(
                                selected = selected,
                                onClick = { gender = g },
                                label = { Text("${g.icon} ${g.label}") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = gColor.copy(alpha = 0.2f),
                                    selectedLabelColor = gColor
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            AnimatedVisibility(tab == 1) {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            onCreateNew(name.trim(), gender)
                            onDismiss()
                        }
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text("Добавить", color = Primary)
                }
            }
        }
    )
}