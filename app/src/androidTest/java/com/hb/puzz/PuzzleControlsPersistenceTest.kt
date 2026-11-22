package com.hb.puzz

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.hb.puzz.data.GameSettings
import com.hb.puzz.data.PuzzleSession
import com.hb.puzz.domain.PuzzleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PuzzleControlsPersistenceTest {
    @get:Rule val folder = TemporaryFolder()

    private suspend fun openSettings(block: suspend (GameSettings) -> Unit) {
        val job = SupervisorJob()
        val store = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + job),
            produceFile = { File(folder.root, "puzzle.preferences_pb") }
        )
        try {
            block(GameSettings(store))
        } finally {
            job.cancelAndJoin()
        }
    }

    @Test fun soundAndShiftSurviveReopening() = runBlocking {
        val original = PuzzleSession(
            levelId = 15, gridSize = 8, positions = PuzzleEngine(8, 1).getCurrentPositions(),
            moveCount = 9, elapsedMillis = 24_500, imageIdentity = "bundled-15", started = true
        )
        val smaller = original.copy(gridSize = 5, positions = PuzzleEngine(5, 2).getCurrentPositions())
        openSettings { settings ->
            settings.saveSession(original)
            settings.toggleSoundEnabled()
            assertTrue(settings.commitGridShift(smaller, replayMode = false))
        }
        openSettings { settings ->
            val saved = requireNotNull(settings.loadSession())
            assertFalse(settings.soundEnabledFlow.first())
            assertEquals(original.sessionId, saved.sessionId)
            assertEquals(original.imageIdentity, saved.imageIdentity)
            assertEquals(original.elapsedMillis, saved.elapsedMillis)
            assertEquals(original.moveCount, saved.moveCount)
            assertEquals(original.gridSize, saved.baseGridSize)
            assertArrayEquals(smaller.positions, saved.positions)
            assertEquals(1, settings.crystalBalanceFlow.first())

            assertTrue(settings.commitGridShift(original, replayMode = false))
            assertEquals(1, settings.crystalBalanceFlow.first())
            assertArrayEquals(original.positions, requireNotNull(settings.loadSession()).positions)
            assertTrue(settings.commitGridShift(smaller, replayMode = true))
            assertEquals(0, settings.crystalBalanceFlow.first())
            assertFalse(settings.commitGridShift(smaller, replayMode = false))
            assertArrayEquals(original.positions, requireNotNull(settings.loadSession()).positions)
            settings.toggleSoundEnabled()
            assertTrue(settings.soundEnabledFlow.first())
        }
    }
}
