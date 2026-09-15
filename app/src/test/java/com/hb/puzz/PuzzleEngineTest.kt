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
        assertTrue(engine.restorePositions(intArrayOf(0, 1, 2, 3)))
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
        assertTrue(engine.restorePositions(IntArray(9) { it }))
        val groups = engine.getConnectedGroups()
        
        // Should have connected groups for adjacent tiles
        assertTrue(groups.isNotEmpty())
    }
    
    @Test
    fun `connected_groups_no_cross_boundary`() {
        val engine = PuzzleEngine(3, 12345L)
        
        assertTrue(engine.restorePositions(intArrayOf(4, 5, 0, 1, 8, 6, 2, 7, 3)))
        // IDs 0 and 1 are at positions 2 and 3, on opposite row edges.
        assertFalse(engine.getCorrectConnections().any { it.firstTileId == 0 && it.secondTileId == 1 })
    }
    
    @Test
    fun `copy_engine_creates_independent_state`() {
        val engine1 = PuzzleEngine(2, 12345L)
        val engine2 = engine1.copy()
        
        // Initial positions should match
        assertArrayEquals(engine1.getCurrentPositions(), engine2.getCurrentPositions())
        
        // Modify engine1 and verify engine2 is unchanged
        val copyBefore = engine2.getCurrentPositions()
        engine1.attemptSwap(0, 1)
        assertArrayEquals(copyBefore, engine2.getCurrentPositions())
        assertFalse(engine1.getCurrentPositions().contentEquals(engine2.getCurrentPositions()))
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
    @Test
    fun `correct_connections_detect_relative_neighbors`() {
        val engine = PuzzleEngine(3, 12345L)
        // Tiles 0,1,3,4 form a correct 2x2 block in the upper-left.
        assertTrue(engine.restorePositions(intArrayOf(0, 1, 5, 3, 4, 2, 8, 7, 6)))
        val connections = engine.getCorrectConnections()
        assertTrue(connections.any { it.firstTileId == 0 && it.secondTileId == 1 })
        assertTrue(connections.any { it.firstTileId == 0 && it.secondTileId == 3 })
    }

    @Test
    fun `total_possible_connections_for_3x3_is_12`() {
        val engine = PuzzleEngine(3, 12345L)
        assertEquals(12, engine.getTotalPossibleConnections())
    }

    @Test
    fun `connected tiles move as one rigid group`() {
        val engine = PuzzleEngine(3, 12345L)
        assertTrue(engine.restorePositions(intArrayOf(8, 6, 5, 0, 1, 7, 4, 3, 2)))

        assertEquals(setOf(0, 1), engine.getGroupForTile(0))
        assertTrue(engine.attemptMoveGroup(anchorTileId = 0, targetPosition = 0))

        assertEquals(0, engine.getPositionOf(0))
        assertEquals(1, engine.getPositionOf(1))
        assertTrue(engine.isValidPermutation())
        assertTrue(engine.getCorrectConnections().any {
            it.firstTileId == 0 && it.secondTileId == 1
        })
    }

    @Test
    fun `group move is rejected when rigid shape would leave board`() {
        val engine = PuzzleEngine(3, 12345L)
        assertTrue(engine.restorePositions(intArrayOf(8, 6, 5, 0, 1, 7, 4, 3, 2)))

        // Tile 0 and 1 are a horizontal pair. Anchoring tile 0 at the far-right cell
        // would force tile 1 outside the board.
        assertNull(engine.getGroupMoveTargets(anchorTileId = 0, targetPosition = 2))
        assertFalse(engine.attemptMoveGroup(anchorTileId = 0, targetPosition = 2))
        assertTrue(engine.isValidPermutation())
    }

    @Test
    fun `loose tile can move onto a cell that currently belongs to a merged group`() {
        val engine = PuzzleEngine(3, 12345L)
        // Tiles 0 and 1 are correctly connected at board positions 3 and 4.
        assertTrue(engine.restorePositions(intArrayOf(8, 6, 5, 0, 1, 7, 4, 3, 2)))
        assertEquals(setOf(0, 1), engine.getGroupForTile(0))

        // Tile 8 is loose at board position 0. Moving it to position 3 must be allowed even
        // though position 3 currently belongs to the merged 0-1 fragment.
        assertTrue(engine.attemptMoveGroup(anchorTileId = 8, targetPosition = 3))
        assertEquals(3, engine.getPositionOf(8))
        assertTrue(engine.isValidPermutation())
    }

    @Test
    fun `move does not require creating a correct connection`() {
        val engine = PuzzleEngine(3, 12345L)
        assertTrue(engine.restorePositions(intArrayOf(8, 6, 5, 0, 1, 7, 4, 3, 2)))

        val before = engine.getCorrectConnections().size
        assertTrue(engine.attemptMoveGroup(anchorTileId = 5, targetPosition = 8))
        assertTrue(engine.isValidPermutation())
        // The move itself is legal regardless of whether progress increased.
        assertEquals(8, engine.getPositionOf(5))
        assertTrue(engine.getCorrectConnections().size >= 0)
        assertTrue(before >= 0)
    }

    @Test fun `many group moves preserve every tile and rigid moving group`() {
        val random = kotlin.random.Random(871)
        for (grid in 4..8) {
            val engine = PuzzleEngine(grid, 123L)
            repeat(300) {
                val anchor = random.nextInt(grid * grid)
                val destination = random.nextInt(grid * grid)
                val targets = engine.getGroupMoveTargets(anchor, destination)
                val old = engine.getCurrentPositions()
                if (engine.attemptMoveGroup(anchor, destination)) {
                    assertNotNull(targets)
                    targets!!.forEach { (tile, position) -> assertEquals(position, engine.getPositionOf(tile)) }
                } else assertArrayEquals(old, engine.getCurrentPositions())
                assertTrue(engine.isValidPermutation())
            }
        }
    }

}
