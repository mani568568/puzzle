package com.hb.puzz.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat

/**
 * Light theme colors for Mosaic Blocks.
 */
private val LightColors = lightColorScheme(
    primary = NAVY_LIGHT,
    onPrimary = Color.White,
    secondary = TEAL,
    onSecondary = Color.Black,
    tertiary = CORAL,
    background = PAPER_IVORY,
    surface = PAPER_IVORY,
    onBackground = NAVY_DARK,
    onSurface = NAVY_DARK
)

/**
 * Dark theme colors for Mosaic Blocks.
 */
private val DarkColors = darkColorScheme(
    primary = NAVY_LIGHT,
    onPrimary = Color.White,
    secondary = TEAL,
    onSecondary = Color.Black,
    tertiary = CORAL,
    background = DARK_BOARD_BG,
    surface = DARK_BOARD_BG,
    onBackground = DARK_PAPER_IVORY,
    onSurface = DARK_PAPER_IVORY
)

/**
 * Mosaic Blocks theme.
 */
@Composable
fun MosaicBlocksTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}

/**
 * Sets up window insets for edge-to-edge display.
 */
@Composable
fun SetupWindowInsets(darkTheme: Boolean) {
    val color = if (darkTheme) DARK_BOARD_BG else PAPER_IVORY
    
    val activity = LocalContext.current as? Activity ?: return
    SideEffect {
        val window = activity.window
        window.statusBarColor = color.toArgb()
        window.navigationBarColor = color.toArgb()
        
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
}
