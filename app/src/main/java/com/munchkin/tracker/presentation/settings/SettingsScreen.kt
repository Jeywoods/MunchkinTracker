package com.munchkin.tracker.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.munchkin.tracker.presentation.components.AppTopBar
import com.munchkin.tracker.ui.theme.*

@Composable
fun SettingsScreen(
    navController: NavController,
    vm: SettingsViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar("Настройки")
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { SectionLabel("Голосовое управление") }
                item {
                    SettingsCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ToggleRow(Icons.Default.Mic, "Режим «всегда слушает»", "Активируется фразой «Эй Манчкин»", state.alwaysListenEnabled, vm::setAlwaysListen)
                            HorizontalDivider(color = Outline)
                            ToggleRow(Icons.AutoMirrored.Filled.VolumeUp, "Голосовые подтверждения (TTS)", "Озвучивать результат команды", state.ttsEnabled, vm::setTtsEnabled)
                            HorizontalDivider(color = Outline)
                            Text("Горячее слово: «${state.hotword}»", style = MaterialTheme.typography.titleSmall, color = OnBackground)
                        }
                    }
                }
                item { SectionLabel("О приложении") }
                item { SettingsCard { InfoRow(Icons.Default.Info, "Версия", "1.0.0") } }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) { Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = Primary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) }

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) { Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SurfaceVariant).padding(16.dp), content = content) }

@Composable
private fun ToggleRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = Primary, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleSmall, color = OnBackground); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant) }
        Switch(checked, onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Background, checkedTrackColor = Primary))
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp)); Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = OnBackground); Text(value, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
    }
}