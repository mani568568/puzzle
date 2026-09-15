package com.hb.puzz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.DisposableEffect
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.hb.puzz.data.GameSettings
import com.hb.puzz.ui.CozyBlocksApp
import com.hb.puzz.ui.theme.CozyBlocksTheme

/** Single-activity entry point for Cozy Picture Blocks. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val settings = remember { GameSettings(applicationContext) }
            val darkTheme by settings.darkThemeFlow.collectAsState(initial = false)

            DisposableEffect(darkTheme) {
                val transparent = android.graphics.Color.TRANSPARENT
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent),
                    navigationBarStyle = if (darkTheme) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
                )
                onDispose { }
            }
            CozyBlocksTheme(darkTheme = darkTheme) {
                CozyBlocksApp(settings = settings)
            }
        }
    }
}
