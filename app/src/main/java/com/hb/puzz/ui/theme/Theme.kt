package com.hb.puzz.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Light theme colors for Cozy Picture Blocks.
 */
private val LightColors = lightColorScheme(
    primary = MUTE_TEAL,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9EFF2),
    onPrimaryContainer = DEEP_TEAL,
    secondary = PEACH_ACCENT,
    secondaryContainer = Color(0xFFFFE3D8),
    onSecondaryContainer = Color(0xFF6B4036),
    surface = Color(0xFFFFFBF6),
    background = COZY_CREAM,
    onSurface = DEEP_TEAL
)

/**
 * Dark theme colors for Cozy Picture Blocks.
 */
private val DarkColors = darkColorScheme(
    primary = Color(0xFF79B8C5),
    onPrimary = Color(0xFF0B323A),
    primaryContainer = Color(0xFF244B55),
    onPrimaryContainer = Color(0xFFD9F3F7),
    secondary = Color(0xFFFFB9A6),
    secondaryContainer = Color(0xFF5B3A32),
    onSecondaryContainer = Color(0xFFFFE2D8),
    surface = Color(0xFF202427),
    background = DARK_COZY_CREAM,
    onSurface = Color(0xFFE8E4DB)
)

/**
 * Cozy Picture Blocks theme.
 */
@Composable
fun CozyBlocksTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
