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
    secondary = PEACH_ACCENT,
    surface = COZY_CREAM,
    background = COZY_CREAM,
    onSurface = DEEP_TEAL
)

/**
 * Dark theme colors for Cozy Picture Blocks.
 */
private val DarkColors = darkColorScheme(
    primary = DARK_MUTE_TEAL,
    onPrimary = Color(0xFFFFFFFF),
    secondary = PEACH_ACCENT,
    surface = DARK_COZY_CREAM,
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
