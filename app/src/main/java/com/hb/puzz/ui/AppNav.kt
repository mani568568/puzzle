package com.hb.puzz.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hb.puzz.data.GameSettings
import com.hb.puzz.domain.PuzzleLevel
import kotlinx.coroutines.launch

private object Routes {
    const val HOME = "home"
    const val LEVELS = "levels"
    const val SETTINGS = "settings"
    const val HOW_TO = "how_to"
    const val GAME_PATTERN = "game/{levelId}"

    fun game(levelId: Int) = "game/$levelId"
}

@Composable
fun CozyBlocksApp(settings: GameSettings) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val completedLevels by settings.completedLevelsFlow.collectAsState(initial = emptySet())
    val highestLevel by settings.highestLevelFlow.collectAsState(initial = 1)
    val savedSession by settings.savedSessionFlow.collectAsState(initial = null)
    val soundEnabled by settings.soundEnabledFlow.collectAsState(initial = true)
    val hapticsEnabled by settings.hapticsEnabledFlow.collectAsState(initial = true)
    val darkThemeEnabled by settings.darkThemeFlow.collectAsState(initial = false)

    fun goHome() {
        navController.navigate(Routes.HOME) {
            popUpTo(Routes.HOME) { inclusive = false }
            launchSingleTop = true
        }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                hasSavedGame = savedSession != null,
                onContinue = {
                    val levelId = savedSession?.levelId ?: highestLevel
                    navController.navigate(Routes.game(levelId))
                },
                onStartNewGame = { navController.navigate(Routes.LEVELS) },
                onHowToPlay = { navController.navigate(Routes.HOW_TO) },
                onSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.LEVELS) {
            LevelSelectionScreen(
                highestUnlockedLevel = highestLevel,
                completedLevels = completedLevels,
                onBack = { navController.popBackStack() },
                onLevelSelected = { levelId ->
                    scope.launch {
                        settings.clearSession()
                        navController.navigate(Routes.game(levelId))
                    }
                }
            )
        }

        composable(Routes.HOW_TO) {
            HowToPlayScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                soundEnabled = soundEnabled,
                hapticsEnabled = hapticsEnabled,
                darkThemeEnabled = darkThemeEnabled,
                onBack = { navController.popBackStack() },
                onSoundChanged = { enabled -> scope.launch { settings.updateSoundEnabled(enabled) } },
                onHapticsChanged = { enabled -> scope.launch { settings.updateHapticsEnabled(enabled) } },
                onDarkThemeChanged = { enabled -> scope.launch { settings.updateDarkTheme(enabled) } },
                onResetProgress = {
                    scope.launch {
                        settings.resetProgress()
                        goHome()
                    }
                }
            )
        }

        composable(
            route = Routes.GAME_PATTERN,
            arguments = listOf(navArgument("levelId") { type = NavType.IntType })
        ) { entry ->
            val levelId = (entry.arguments?.getInt("levelId") ?: 1)
                .coerceIn(1, PuzzleLevel.maxLevelId)

            PicturePuzzleGameScreen(
                levelId = levelId,
                settings = settings,
                soundEnabled = soundEnabled,
                hapticsEnabled = hapticsEnabled,
                onBack = { navController.popBackStack() },
                onHome = { goHome() },
                onNextLevel = { nextLevel ->
                    navController.navigate(Routes.game(nextLevel)) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                }
            )
        }
    }
}
