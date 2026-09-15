package com.hb.puzz

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hb.puzz.data.GameSettings
import com.hb.puzz.data.PuzzleSession
import com.hb.puzz.data.images.ImageSourceMode
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Run on a clean emulator: this test writes real game progress. */
@RunWith(AndroidJUnit4::class)
class ProgressPersistenceTest {
    @Test fun duplicateCompletionAndResume() = runBlocking {
        val settings = GameSettings(InstrumentationRegistry.getInstrumentation().targetContext)
        settings.resetProgress()
        val s = PuzzleSession(1, 4, IntArray(16) { it }, 28, 60_000,
            imageSource = ImageSourceMode.PRELOADED, started = true, imageIdentity = "bundled-1")
        settings.saveSession(s)
        assertArrayEquals(s.positions, settings.loadSession()!!.positions)
        assertEquals(s.elapsedMillis, settings.loadSession()!!.elapsedMillis)
        assertEquals(s.imageIdentity, settings.loadSession()!!.imageIdentity)
        val first = async { settings.completeSession(s) }
        val second = async { settings.completeSession(s) }
        val a = first.await(); val b = second.await()
        assertEquals(a.balance, b.balance)
        assertEquals(a.awarded, b.awarded)
        assertEquals(a.balance, settings.coinBalanceFlow.first())
        assertNull(settings.loadSession())
        assertTrue(1 in settings.completedLevelsFlow.first())
        assertEquals(2, settings.highestLevelFlow.first())
        val beforeReset = settings.coinBalanceFlow.first()
        settings.resetProgress()
        assertEquals(beforeReset, settings.coinBalanceFlow.first())
    }
}
