package com.munchkin.tracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── Palette ────────────────────────────────────────────────────────────────
// Dark base: deep midnight blue-black, NOT purple
val Background       = Color(0xFF0B0F1A)   // near-black with cool blue tint
val Surface          = Color(0xFF131827)   // slightly lighter card surface
val SurfaceVariant   = Color(0xFF1C2235)   // elevated surface
val SurfaceBright    = Color(0xFF242B40)   // for dialogs / sheets

// Neon accent: electric cyan-teal
val Primary          = Color(0xFF00E5CC)   // electric teal – main accent
val PrimaryDim       = Color(0xFF00B8A3)   // pressed / dimmed variant
val PrimaryContainer = Color(0xFF003D36)   // container tint

// Secondary: vibrant amber-gold for levels / winners
val Secondary        = Color(0xFFFFB930)   // warm gold
val SecondaryDim     = Color(0xFFC88E00)
val SecondaryContainer = Color(0xFF3D2E00)

// Tertiary: rose / magenta for female indicator
val Tertiary         = Color(0xFFFF5C8A)
val TertiaryContainer= Color(0xFF3D0020)

// Level milestone colours
val GoldGlow         = Color(0xFFFFD700)   // level 9–10 glow
val LevelUp          = Color(0xFF39FF14)   // neon green flash on +
val LevelDown        = Color(0xFFFF3D5A)   // neon red flash on -

// Neutrals
val OnBackground     = Color(0xFFE8EAF6)
val OnSurface        = Color(0xFFCDD3E8)
val OnSurfaceVariant = Color(0xFF8891AB)
val Outline          = Color(0xFF2E3650)
val OutlineBright    = Color(0xFF3E4E70)

// Voice indicator
val VoiceSleep       = Color(0xFF3E4E70)
val VoiceActive      = Color(0xFF00E5CC)
val VoiceRecording   = Color(0xFFFF3D5A)
val VoiceError       = Color(0xFFFFB930)

// ─── Light palette (secondary theme) ────────────────────────────────────────
val LightBackground       = Color(0xFFF4F6FF)
val LightSurface          = Color(0xFFFFFFFF)
val LightSurfaceVariant   = Color(0xFFEAEDF8)
val LightPrimary          = Color(0xFF007A6D)
val LightOnPrimary        = Color(0xFFFFFFFF)
val LightSecondary        = Color(0xFF8A6200)
val LightTertiary         = Color(0xFFB5003E)
val LightOnBackground     = Color(0xFF0D1120)
val LightOnSurface        = Color(0xFF1A2035)

// ─── Dark colour scheme ──────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary            = Primary,
    onPrimary          = Color(0xFF001A17),
    primaryContainer   = PrimaryContainer,
    onPrimaryContainer = Color(0xFF80FFE8),
    secondary          = Secondary,
    onSecondary        = Color(0xFF1F1700),
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = Color(0xFFFFE08A),
    tertiary           = Tertiary,
    onTertiary         = Color(0xFF200011),
    tertiaryContainer  = TertiaryContainer,
    onTertiaryContainer= Color(0xFFFFB3CB),
    background         = Background,
    onBackground       = OnBackground,
    surface            = Surface,
    onSurface          = OnSurface,
    surfaceVariant     = SurfaceVariant,
    onSurfaceVariant   = OnSurfaceVariant,
    outline            = Outline,
    outlineVariant     = OutlineBright,
    error              = LevelDown,
    onError            = Color(0xFF1A0009),
)

private val LightColorScheme = lightColorScheme(
    primary            = LightPrimary,
    onPrimary          = LightOnPrimary,
    primaryContainer   = Color(0xFFB2F5EC),
    onPrimaryContainer = Color(0xFF002019),
    secondary          = LightSecondary,
    onSecondary        = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE08A),
    onSecondaryContainer = Color(0xFF271900),
    tertiary           = LightTertiary,
    onTertiary         = Color(0xFFFFFFFF),
    tertiaryContainer  = Color(0xFFFFD9E4),
    onTertiaryContainer= Color(0xFF3E0013),
    background         = LightBackground,
    onBackground       = LightOnBackground,
    surface            = LightSurface,
    onSurface          = LightOnSurface,
    surfaceVariant     = LightSurfaceVariant,
    onSurfaceVariant   = Color(0xFF44485A),
    outline            = Color(0xFF74788A),
    outlineVariant     = Color(0xFFC4C7DA),
)

@Composable
fun MunchkinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MunchkinTypography,
        content = content
    )
}
