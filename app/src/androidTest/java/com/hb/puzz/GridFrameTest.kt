package com.hb.puzz

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.hb.puzz.data.GameSettings
import com.hb.puzz.ui.CozyBlocksApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class GridFrameTest {
    @get:Rule val ui = createComposeRule()
    @get:Rule val folder = TemporaryFolder()
    private val job = SupervisorJob()

    @After fun closeStore() = runBlocking { job.cancelAndJoin() }

    @Test fun reduceAndRestoreKeepTheSamePhotoBoundsWhilePaused() {
        val settings = GameSettings(PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + job),
            produceFile = { File(folder.root, "game.preferences_pb") }
        ))
        ui.setContent { CozyBlocksApp(settings) }

        fun awaitNode(matcher: SemanticsMatcher): SemanticsNodeInteraction {
            ui.waitUntil(15_000) { ui.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
            return ui.onNode(matcher)
        }
        awaitNode(hasText("NEW GAME")).performClick()
        val original = hasContentDescription("Puzzle board, 4 columns by 6 rows")
        val boardSize = awaitNode(original).fetchSemanticsNode().size
        assertEquals(boardSize.width * 2000.0 / 1280.0, boardSize.height.toDouble(), 1.0)
        ui.onNodeWithContentDescription("Pause the puzzle timer").performScrollTo().performClick()
        ui.onNodeWithContentDescription("Mute game sound").performScrollTo().performClick()
        awaitNode(hasContentDescription("Unmute game sound")).assertIsOff()

        ui.onNode(hasContentDescription("Spend one Crystal", substring = true)).performScrollTo().performClick()
        val restore = awaitNode(hasContentDescription("Restore the original puzzle grid for free"))
        val smaller = hasContentDescription("Puzzle board,", substring = true) and !original
        assertEquals(boardSize, awaitNode(smaller).fetchSemanticsNode().size)
        restore.performScrollTo().performClick()
        assertEquals(boardSize, awaitNode(original).fetchSemanticsNode().size)
        ui.onNodeWithContentDescription("Resume the puzzle timer").assertExists()
        ui.onNodeWithContentDescription("Save and go back").performScrollTo().performClick()
        awaitNode(hasText("CONTINUE"))
    }
}
