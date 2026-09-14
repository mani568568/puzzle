package com.hb.puzz

import com.hb.puzz.domain.PuzzleLevel
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleLevelTest {
    @Test
    fun `first five chapters are 4x4`() {
        (1..5).forEach { id ->
            val level = PuzzleLevel.requireLevel(id)
            assertEquals(4, level.gridSize)
            assertTrue(level.acceptsGridSize(4))
        }
    }

    @Test
    fun `progression reaches 8x8`() {
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
}
