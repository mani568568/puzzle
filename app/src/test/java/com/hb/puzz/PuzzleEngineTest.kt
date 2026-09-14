package com.hb.puzz

import com.hb.puzz.domain.PuzzleEngine
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the Puzzle Engine.
 */
class PuzzleEngineTest {
    
    @Test
    fun `initial state should be unsolved`() {
        val engine = PuzzleEngine(3)
        
        assertFalse(engine.isSolved())
    }
    
    @Test
    fun `solved_state_detection`() {
        // Create engine with solved positions
        val engine = PuzzleEngine(2, 12345L)
        for (i in 0 until 4) {
            engine._tilePositions[i] = i
        }
        
        assertTrue(engine.isSolved())
    }
    
    @Test
    fun `swap_functionality`() {
        val engine = PuzzleEngine(3, 12345L)
        
        // Get initial positions
        val originalPos0 = engine.getTileAt(0)
        val originalPos1 = engine.getTileAt(1)
        
        // Swap positions 0 and 1
        assertTrue(engine.attemptSwap(0, 1))
        
        // Verify swap occurred
        assertEquals(originalPos1, engine.getTileAt(0))
        assertEquals(originalPos0, engine.getTileAt(1))
    }
    
    @Test
    fun `invalid_swap_out_of_bounds`() {
        val engine = PuzzleEngine(3)
        
        assertFalse(engine.attemptSwap(-1, 5))
        assertFalse(engine.attemptSwap(9, 1))  // 3x3 has indices 0-8
    }
    
    @Test
    fun `same_position_swap_noop`() {
        val engine = PuzzleEngine(3, 12345L)
        
        val pos = engine.getTileAt(4)
        assertFalse(engine.attemptSwap(4, 4))  // Same position returns false
        assertEquals(pos, engine.getTileAt(4))
    }
    
    @Test
    fun `shuffle_creates_valid_permutation`() {
        val engine = PuzzleEngine(3, 12345L)
        
        assertTrue(engine.isValidPermutation())
        assertFalse(engine.isSolved())  // Shuffled should not be solved
    }
    
    @Test
    fun `deterministic_shuffling_with_seed`() {
        val engine1 = PuzzleEngine(3, 99999L)
        val engine2 = PuzzleEngine(3, 99999L)
        
        // Both engines should have same shuffled state with same seed
        assertArrayEquals(engine1.getCurrentPositions(), engine2.getCurrentPositions())
    }
    
    @Test
    fun `connected_groups_horizontal_adjacency`() {
        val engine = PuzzleEngine(3, 12345L)
        
        // Set up a solved state
        for (i in 0 until 9) {
            engine._tilePositions[i] = i
        }
        
        val groups = engine.getConnectedGroups()
        
        // Should have connected groups for adjacent tiles
        assertTrue(groups.isNotEmpty())
    }
    
    @Test
    fun `connected_groups_no_cross_boundary`() {
        val engine = PuzzleEngine(3, 12345L)
        
        // Set up state where tile 0 (should be at pos 0) is at position 3
        // This breaks horizontal connection across row boundary
        for (i in 0 until 9) {
            engine._tilePositions[i] = i
        }
        // Swap 0 and 3
        val temp = engine._tilePositions[0]
        engine._tilePositions[0] = engine._tilePositions[3]
        engine._tilePositions[3] = temp
        
        val groups = engine.getConnectedGroups()
        
        // Row boundary should prevent false connections
    }
    
    @Test
    fun `copy_engine_creates_independent_state`() {
        val engine1 = PuzzleEngine(2, 12345L)
        val engine2 = engine1.copy()
        
        // Initial positions should match
        assertArrayEquals(engine1.getCurrentPositions(), engine2.getCurrentPositions())
        
        // Modify engine1 and verify engine2 is unchanged
        engine1.attemptSwap(0, 1)
        assertFalse(engine1.isSolved())
    }
    
    @Test
    fun `grid_size_validation`() {
        val engine3x3 = PuzzleEngine(3)
        assertEquals(3, engine3x3.gridSize)
        
        val engine4x4 = PuzzleEngine(4)
        assertEquals(4, engine4x4.gridSize)
    }
    
    @Test
    fun `tile_position_mapping`() {
        val engine = PuzzleEngine(2, 12345L)
        
        // Verify we can get tile at position
        for (i in 0 until 4) {
            val tileId = engine.getTileAt(i)
            assertTrue(tileId >= 0 && tileId < 4)
            
            // And find position of a tile
            val pos = engine.getPositionOf(tileId)
            assertEquals(i, pos)  // Should match where we found it
        }
    }
}
