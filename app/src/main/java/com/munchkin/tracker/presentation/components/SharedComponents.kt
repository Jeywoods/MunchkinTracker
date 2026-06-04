package com.munchkin.tracker.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.munchkin.tracker.domain.model.VoiceState
import com.munchkin.tracker.ui.theme.*

// ─── Voice Indicator ──────────────────────────────────────────────────────────
@Composable
fun VoiceIndicator(
    voiceState: VoiceState,
    recognizedText: String,
    amplitude: Float,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val indicatorColor = when (voiceState) {
        VoiceState.SLEEPING -> VoiceSleep
        VoiceState.ACTIVE -> VoiceActive
        VoiceState.RECORDING -> VoiceRecording
        VoiceState.ERROR -> VoiceError
    }

    // Pulsing animation for recording state
    val infiniteTransition = rememberInfiniteTransition(label = "voice_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (voiceState == VoiceState.RECORDING) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Mic icon button with animated colour
        IconButton(
            onClick = onMicClick,
            modifier = Modifier
                .size(40.dp)
                .scale(pulseScale)
                .background(indicatorColor.copy(alpha = 0.15f), CircleShape)
        ) {
            Icon(
                imageVector = if (voiceState == VoiceState.SLEEPING) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = "Микрофон",
                tint = indicatorColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when (voiceState) {
                    VoiceState.SLEEPING -> "👂 Ожидание"
                    VoiceState.ACTIVE -> "✅ Активен"
                    VoiceState.RECORDING -> "🎤 Запись..."
                    VoiceState.ERROR -> "⚠️ Ошибка"
                },
                style = MaterialTheme.typography.labelMedium,
                color = indicatorColor
            )
            if (recognizedText.isNotEmpty() && voiceState == VoiceState.RECORDING) {
                Text(
                    text = recognizedText,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        // Sound wave bars
        if (voiceState == VoiceState.RECORDING) {
            SoundWave(amplitude = amplitude, color = VoiceRecording)
        }
    }
}

// ─── Sound wave visualiser ────────────────────────────────────────────────────
@Composable
fun SoundWave(amplitude: Float, color: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "b1"
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(300), RepeatMode.Reverse), label = "b2"
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "b3"
    )

    Row(
        modifier = modifier.height(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        listOf(bar1, bar2, bar3).forEach { scale ->
            val height = (24 * scale * (0.4f + amplitude * 0.6f)).coerceAtLeast(4f).dp
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(height)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

// ─── Game timer ───────────────────────────────────────────────────────────────
@Composable
fun GameTimer(seconds: Long, modifier: Modifier = Modifier) {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    val text = if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = OnSurfaceVariant,
        modifier = modifier
    )
}

// ─── Gender icon ──────────────────────────────────────────────────────────────
@Composable
fun GenderIcon(gender: com.munchkin.tracker.domain.model.Gender, size: Dp = 16.dp) {
    val (icon, color) = when (gender) {
        com.munchkin.tracker.domain.model.Gender.MALE -> "♂" to Primary
        com.munchkin.tracker.domain.model.Gender.FEMALE -> "♀" to Tertiary
    }
    Text(text = icon, color = color, fontSize = size.value.sp)
}

// ─── Screen header ────────────────────────────────────────────────────────────
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = OnBackground,
            modifier = Modifier.weight(1f)
        )
        actions()
    }
}

// ─── Confirm dialog ───────────────────────────────────────────────────────────
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "Подтвердить",
    dismissText: String = "Отмена",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceBright,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = Primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissText, color = OnSurfaceVariant) }
        }
    )
}
