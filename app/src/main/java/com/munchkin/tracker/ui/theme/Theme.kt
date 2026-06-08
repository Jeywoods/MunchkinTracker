package com.munchkin.tracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── Frozen North Palette ─────────────────────────────────────────────────────
val Background       = Color(0xFF0D1B2A)   // тёмно-синий лёд
val Surface          = Color(0xFF1B2D41)   // карточки
val SurfaceVariant   = Color(0xFF243850)   // elevated surface
val SurfaceBright    = Color(0xFF2D4560)   // диалоги / sheets

val Primary          = Color(0xFF64B5F6)   // голубой
val PrimaryDim       = Color(0xFF4293E8)
val PrimaryContainer = Color(0xFF002244)

val Secondary        = Color(0xFFFFB74D)   // тёплый оранжевый
val SecondaryDim     = Color(0xFFE09A30)
val SecondaryContainer = Color(0xFF3D2000)

// Пол: мужской — синий, женский — красный
val MaleColor        = Color(0xFF448AFF)   // синий
val FemaleColor      = Color(0xFFE91E63)   // розовый
val Tertiary         = FemaleColor
val TertiaryContainer = Color(0xFF3D001A)

val GoldGlow         = Color(0xFFFFD700)   // победное золото
val LevelUp          = Color(0xFF4CAF50)   // зелёный
val LevelDown        = Color(0xFFF44336)   // красный

val OnBackground     = Color(0xFFBBDEFB)   // светло-голубой текст
val OnSurface        = Color(0xFFA8D0F0)
val OnSurfaceVariant = Color(0xFF7898B8)
val Outline          = Color(0xFF243850)
val OutlineBright    = Color(0xFF385878)

val VoiceSleep       = Color(0xFF385878)
val VoiceActive      = Color(0xFF64B5F6)
val VoiceRecording   = Color(0xFFF44336)
val VoiceError       = Color(0xFFFFB74D)

// ─── Dark colour scheme ──────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary            = Primary,
    onPrimary          = Color(0xFF001A30),
    primaryContainer   = PrimaryContainer,
    onPrimaryContainer = Color(0xFFC8E0FF),
    secondary          = Secondary,
    onSecondary        = Color(0xFF1F0D00),
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = Color(0xFFFFD8A0),
    tertiary           = Tertiary,
    onTertiary         = Color(0xFF1A0008),
    tertiaryContainer  = TertiaryContainer,
    onTertiaryContainer = Color(0xFFFFB3D0),
    background         = Background,
    onBackground       = OnBackground,
    surface            = Surface,
    onSurface          = OnSurface,
    surfaceVariant     = SurfaceVariant,
    onSurfaceVariant   = OnSurfaceVariant,
    outline            = Outline,
    outlineVariant     = OutlineBright,
    error              = FemaleColor,
    onError            = Color(0xFF1A0008),
)

@Composable
fun MunchkinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = MunchkinTypography,
        content = content
    )
}