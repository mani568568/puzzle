package com.hb.puzz

import com.hb.puzz.domain.model.PieceColor
import com.hb.puzz.domain.model.PieceGenerator
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

/**
 * Unit tests for PieceGenerator.
 */
class PieceGeneratorTest {
    
    private val random = Random(12345)
    private val generator = PieceGenerator(random)
    
    @Test
    fun `generate_single_piece_returns_non_empty_cells`() {
        val piece = generator.generatePiece(1)
        
        assertTrue(piece.cells.isNotEmpty())
    }
    
    @Test
    fun `generated_pieces_have_valid_bounds`() {
        repeat(20) { id ->
            val piece = generator.generatePiece(id)
            
            // Bounds should be at least 1x1
            assertTrue(piece.bounds.first >= 1)
            assertTrue(piece.bounds.second >= 1)
        }
    }
    
    @Test
    fun `generate_multiple_pieces_returns_unique_shapes`() {
        val pieces = generator.generatePieces(3)
        
        assertEquals(3, pieces.size)
        
        // Verify each piece has unique ID
        val ids = pieces.map { it.id }.toSet()
        assertEquals(3, ids.size)
    }
    
    @Test
    fun `piece_colors_are_valid`() {
        repeat(10) {
            val piece = generator.generatePiece(it)
            
            // All colors should be in the enum
            assertTrue(piece.color in PieceColor.values())
        }
    }
    
    @Test
    fun `pieces_include_various_shapes()`() {
        val shapes = HashSet<String>()
        
        // Generate many pieces and check variety
        repeat(50) { id ->
            val piece = generator.generatePiece(id)
            
            // Create a string representation of shape relative to origin
            val minX = piece.cells.minOfOrNull { it.x } ?: 0
            val minY = piece.cells.minOfOrNull { it.y } ?: 0
            
            val normalizedCells = piece.cells.map { cell ->
                "${cell.x - minX},${cell.y - minY}"
            }.sorted().joinToString(",")
            
            shapes.add(normalizedCells)
        }
        
        // We should have many different shapes
        assertTrue(shapes.size > 10)
    }
    
    @Test
    fun `piece_offset_preserves_shape()`() {
        val originalPiece = generator.generatePiece(1)
        
        val offsetX = 5
        val offsetY = 3
        
        val offsetCells = originalPiece.withOffset(offsetX, offsetY)
        
        // Verify the shape is preserved (relative positions maintained)
        for (i in originalPiece.cells.indices) {
            assertEquals(
                originalPiece.cells[i].x + offsetX,
                offsetCells[i].x
            )
            assertEquals(
                originalPiece.cells[i].y + offsetY,
                offsetCells[i].y
            )
        }
    }
}
