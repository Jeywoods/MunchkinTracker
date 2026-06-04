package com.munchkin.tracker.presentation.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.munchkin.tracker.domain.model.GamePlayer
import com.munchkin.tracker.ui.theme.*

@Composable
fun EndGameDialog(
    players: List<GamePlayer>,
    winLevel: Int = 10,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val eligiblePlayers = players.filter { it.currentLevel >= winLevel }
    val hasAutoWinners = eligiblePlayers.isNotEmpty()

    var selectedId by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceBright,
        modifier = Modifier.fillMaxWidth(),
        title = {
            Text("🏆 Завершить игру", style = MaterialTheme.typography.titleLarge, color = GoldGlow)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hasAutoWinners) {
                    // Есть игроки с 10 уровнем — все победители
                    Text(
                        "Победители:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GoldGlow
                    )
                    eligiblePlayers.sortedByDescending { it.currentLevel }.forEach { gp ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(SecondaryContainer, SurfaceVariant)
                                    )
                                )
                                .border(1.dp, GoldGlow, RoundedCornerShape(14.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("⭐", fontSize = 20.sp)
                            Text(
                                gp.player.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = GoldGlow,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "Ур. ${gp.currentLevel}",
                                style = MaterialTheme.typography.labelLarge,
                                color = GoldGlow,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // Нет игроков с 10 уровнем — выбираем победителя
                    Text(
                        "Выберите победителя:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                    players.sortedByDescending { it.currentLevel }.forEach { gp ->
                        val selected = selectedId == gp.id
                        val borderColor = if (selected) GoldGlow else OutlineBright
                        val bg = if (selected)
                            Brush.horizontalGradient(listOf(SecondaryContainer, SurfaceVariant))
                        else
                            Brush.horizontalGradient(listOf(SurfaceVariant, SurfaceVariant))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(bg)
                                .border(
                                    if (selected) 2.dp else 1.dp,
                                    borderColor,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { selectedId = gp.id }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                if (selected) "👑" else "  ",
                                fontSize = 20.sp
                            )
                            Text(
                                gp.player.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (selected) GoldGlow else OnBackground,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "Ур. ${gp.currentLevel}",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) GoldGlow else OnSurfaceVariant,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (hasAutoWinners) {
                        // Завершаем с первым победителем (все равно все с 10 уровнем)
                        onConfirm(eligiblePlayers.first().id)
                    } else {
                        selectedId?.let { onConfirm(it) }
                    }
                },
                enabled = hasAutoWinners || selectedId != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Secondary,
                    contentColor = Background
                )
            ) {
                Text("🏆 Завершить", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = OnSurfaceVariant)
            }
        }
    )
}