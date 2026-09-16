package com.hb.puzz

import com.hb.puzz.domain.PuzzleEngine
import org.junit.Assert.*
import org.junit.Test

class PuzzleEngineTest {
    @Test
    fun `rectangular geometry always has more rows than columns`() {
        for (size in 2..8) {
            val engine = PuzzleEngine(size, 123L)
            assertTrue(engine.gridRows > engine.gridColumns)
            assertEquals(engine.gridRows * engine.gridColumns, engine.getTotalTiles())
        }
    }

    @Test
    fun `size four produces four by five puzzle`() {
        val engine = PuzzleEngine(4, 123L)
        assertEquals(4, engine.gridColumns)
        assertEquals(5, engine.gridRows)
        assertEquals(20, engine.getTotalTiles())
        assertEquals(31, engine.getTotalPossibleConnections())
    }

    @Test
    fun `fresh rectangular puzzle begins at zero completion`() {
        for (size in 2..8) {
            for (seed in 1L..20L) {
                val engine = PuzzleEngine(size, seed)
                assertFalse(engine.isSolved())
                assertTrue(engine.getCorrectConnections().isEmpty())
                assertTrue(engine.isValidPermutation())
            }
        }
    }

    @Test
    fun `solved rectangular positions are detected`() {
        val engine = PuzzleEngine(4, 123L)
        assertTrue(engine.restorePositions(IntArray(engine.getTotalTiles()) { it }))
        assertTrue(engine.isSolved())
    }

    @Test
    fun `group moves preserve a valid rectangular permutation`() {
        val random = kotlin.random.Random(871)
        for (size in 3..8) {
            val engine = PuzzleEngine(size, 123L)
            repeat(250) {
                val anchor = random.nextInt(engine.getTotalTiles())
                val destination = random.nextInt(engine.getTotalTiles())
                engine.attemptMoveGroup(anchor, destination)
                assertTrue(engine.isValidPermutation())
            }
        }
    }

    @Test
    fun `copy keeps rectangular geometry and independent state`() {
        val first = PuzzleEngine(5, 42L)
        val copy = first.copy()
        assertEquals(first.gridColumns, copy.gridColumns)
        assertEquals(first.gridRows, copy.gridRows)
        assertArrayEquals(first.getCurrentPositions(), copy.getCurrentPositions())
        first.attemptSwap(0, 1)
        assertFalse(first.getCurrentPositions().contentEquals(copy.getCurrentPositions()))
    }

    @Test
    fun `hint eventually solves rectangular puzzle`() {
        val engine = PuzzleEngine(4, 99L)
        var steps = 0
        while (!engine.isSolved() && steps < engine.getTotalTiles() * 2) {
            val moved = engine.applyHintStep()
            assertTrue(moved.isNotEmpty())
            assertTrue(engine.isValidPermutation())
            steps++
        }
        assertTrue(engine.isSolved())
    }
}
