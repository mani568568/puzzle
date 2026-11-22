package com.hb.puzz

import com.hb.puzz.domain.PuzzleEngine
import com.hb.puzz.domain.PuzzleLevel
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleLevelTest {
    @Test
    fun `first five chapters use size four rectangular preset`() {
        (1..5).forEach { id ->
            val level = PuzzleLevel.requireLevel(id)
            assertEquals(4, level.gridSize)
            assertTrue(level.acceptsGridSize(4))
        }
    }

    @Test
    fun `progression reaches size eight rectangular preset`() {
        assertEquals(5, PuzzleLevel.requireLevel(6).gridSize)
        assertEquals(6, PuzzleLevel.requireLevel(9).gridSize)
        assertEquals(7, PuzzleLevel.requireLevel(12).gridSize)
        assertEquals(8, PuzzleLevel.requireLevel(15).gridSize)
        assertEquals(8, PuzzleLevel.requireLevel(20).gridSize)
    }

    @Test
    fun `surprise chapters choose only 6 through 8`() {
        val random = Random(2026)
        (16..19).forEach { id ->
            val level = PuzzleLevel.requireLevel(id)
            repeat(25) {
                val size = level.pickGridSize(random)
                assertTrue(size in 6..8)
                assertTrue(level.acceptsGridSize(size))
            }
        }
    }

    @Test
    fun `difficulty labels use journey language`() {
        assertEquals("Easy · Cozy Start", PuzzleLevel.Difficulty.EASY.displayLabel)
        assertEquals("Medium · Focus Flow", PuzzleLevel.Difficulty.MEDIUM.displayLabel)
        assertEquals("Hard · Master Quest", PuzzleLevel.Difficulty.HARD.displayLabel)
    }
    @Test
    fun `grid shift uses smaller rectangles and can still open legacy shifts`() {
        val easy = PuzzleLevel.requireLevel(1)
        assertTrue(easy.acceptsSessionGridSize(4, 3))
        assertTrue(easy.acceptsSessionGridSize(4, 2))
        assertTrue(!easy.acceptsSessionGridSize(4, 1))
        assertTrue(!easy.acceptsSessionGridSize(4, 5))

        val six = PuzzleLevel.requireLevel(9)
        assertTrue(six.acceptsSessionGridSize(6, 5))
        assertTrue(six.acceptsSessionGridSize(6, 4))
        assertTrue(!six.acceptsSessionGridSize(6, 3))

        val max = PuzzleLevel.requireLevel(20)
        assertTrue(max.acceptsSessionGridSize(8, 7))
        assertTrue(max.acceptsSessionGridSize(8, 6))
        assertEquals(listOf(5, 4), PuzzleLevel.smallerGridSizes(8))
        assertTrue(!max.acceptsSessionGridSize(8, 3))

        assertTrue(PuzzleLevel.smallerGridSizes(PuzzleLevel.MIN_GRID_SIZE).isEmpty())
        for (level in PuzzleLevel.ALL_LEVELS) {
            for (base in level.randomGridSizes ?: (level.gridSize..level.gridSize)) {
                val candidates = PuzzleLevel.smallerGridSizes(base)
                assertTrue(candidates.size in 1..2)
                for (target in candidates) {
                    assertTrue(PuzzleEngine.rowsForSize(target) <= PuzzleEngine.rowsForSize(base))
                    assertTrue(PuzzleEngine.columnsForSize(target) <= PuzzleEngine.columnsForSize(base))
                    assertTrue(PuzzleEngine.tileCountForSize(target) < PuzzleEngine.tileCountForSize(base))
                    assertTrue(level.acceptsSessionGridSize(base, target))
                    assertTrue(level.acceptsGridSize(target))
                }
            }
        }
    }

}
