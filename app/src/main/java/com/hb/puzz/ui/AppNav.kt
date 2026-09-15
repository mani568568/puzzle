package com.hb.puzz.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hb.puzz.data.GameSettings
import com.hb.puzz.data.images.PuzzleImageRepository
import com.hb.puzz.domain.PuzzleLevel
import kotlinx.coroutines.launch

private object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val HOW_TO = "how_to"
    const val HISTORY = "history"
    const val GAME_PATTERN = "game/{levelId}/{gridSize}"

    fun game(levelId: Int, gridSize: Int) = "game/$levelId/$gridSize"
}

@Composable
fun CozyBlocksApp(settings: GameSettings) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val imageRepository = remember(settings) { PuzzleImageRepository(context, settings) }

    val homeState by settings.homeFlow.collectAsState(initial = null)
    val home = homeState
    if (home == null) {
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) { androidx.compose.material3.CircularProgressIndicator() }
        return
    }
    val completedLevels = home.completed
    val highestLevel = home.highest
    val savedSession = home.saved
    val soundEnabled = home.sound
    val hapticsEnabled = home.haptics
    val darkThemeEnabled = home.dark
    val imageSource = home.source

    fun goHome() {
        navController.navigate(Routes.HOME) {
            popUpTo(Routes.HOME) { inclusive = false }
            launchSingleTop = true
        }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            val currentChapter = if (completedLevels.size >= PuzzleLevel.maxLevelId) {
                PuzzleLevel.maxLevelId
            } else {
                highestLevel
            }
            val currentLevel = PuzzleLevel.requireLevel(currentChapter)
            val currentChapterTitle = currentLevel.title
            HomeScreen(
                coinBalance = home.coins,
                completedCount = completedLevels.size,
                hasSavedGame = savedSession != null,
                currentChapter = currentChapter,
                currentChapterTitle = currentChapterTitle,
                currentDifficulty = currentLevel.difficulty.displayLabel,
                currentGridDescription = currentLevel.gridDescription,
                journeyComplete = completedLevels.size >= PuzzleLevel.maxLevelId,
                onContinue = {
                    val chapterId = savedSession?.levelId ?: currentChapter
                    val chapter = PuzzleLevel.requireLevel(chapterId)
                    val gridSize = savedSession?.takeIf { it.levelId == chapterId }?.gridSize
                        ?: chapter.pickGridSize()
                    navController.navigate(Routes.game(chapterId, gridSize))
                },
                onStartJourney = {
                    val gridSize = currentLevel.pickGridSize()
                    navController.navigate(Routes.game(currentChapter, gridSize))
                },
                onJourneyHistory = { navController.navigate(Routes.HISTORY) },
                onHowToPlay = { navController.navigate(Routes.HOW_TO) },
                onSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.HOW_TO) {
            HowToPlayScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.HISTORY) {
            JourneyHistoryScreen(
                home = home,
                onBack = { navController.popBackStack() },
                onOpenAdventure = { adventureId ->
                    val adventure = PuzzleLevel.requireLevel(adventureId)
                    val savedForAdventure = savedSession?.takeIf { it.levelId == adventureId }
                    val gridSize = savedForAdventure?.gridSize ?: adventure.pickGridSize()
                    navController.navigate(Routes.game(adventureId, gridSize))
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                soundEnabled = soundEnabled,
                hapticsEnabled = hapticsEnabled,
                darkThemeEnabled = darkThemeEnabled,
                imageSource = imageSource,
                pexelsConfigured = imageRepository.isPexelsConfigured(),
                onBack = { navController.popBackStack() },
                onSoundChanged = { enabled -> scope.launch { settings.updateSoundEnabled(enabled) } },
                onHapticsChanged = { enabled -> scope.launch { settings.updateHapticsEnabled(enabled) } },
                onDarkThemeChanged = { enabled -> scope.launch { settings.updateDarkTheme(enabled) } },
                onImageSourceChanged = { mode -> scope.launch { settings.updateImageSource(mode) } },
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
            arguments = listOf(
                navArgument("levelId") { type = NavType.IntType },
                navArgument("gridSize") { type = NavType.IntType }
            )
        ) { entry ->
            val levelId = (entry.arguments?.getInt("levelId") ?: 1)
                .coerceIn(1, PuzzleLevel.maxLevelId)
            val level = PuzzleLevel.requireLevel(levelId)
            val requestedGridSize = entry.arguments?.getInt("gridSize") ?: level.gridSize
            val gridSize = requestedGridSize.takeIf(level::acceptsGridSize) ?: level.gridSize

            PicturePuzzleGameScreen(
                levelId = levelId,
                gridSize = gridSize,
                settings = settings,
                imageRepository = imageRepository,
                imageSource = imageSource,
                soundEnabled = soundEnabled,
                hapticsEnabled = hapticsEnabled,
                onBack = { navController.popBackStack() },
                onHome = { goHome() },
                onNextLevel = { nextLevel ->
                    val next = PuzzleLevel.requireLevel(nextLevel)
                    navController.navigate(Routes.game(nextLevel, next.pickGridSize())) {
                        launchSingleTop = true
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                }
            )
        }
    }
}
