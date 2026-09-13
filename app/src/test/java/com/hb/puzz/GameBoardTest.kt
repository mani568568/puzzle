package com.hb.puzz

import com.hb.puzz.domain.model.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for GameBoard logic.
 */
class GameBoardTest {
    
    @Test
    fun `empty board should have no cells`() {
        val board = GameBoard.createEmptyBoard()
        
        assertEquals(0, board.cells.size)
        assertFalse(board.isFull())
    }
    
    @Test
    fun `board boundary checking`() {
        val board = GameBoard.createEmptyBoard()
        
        // Test piece within bounds
        val validPiece = Piece(
            id = 1,
            cells = listOf(Cell(0, 0), Cell(7, 7)),
            color = PieceColor.CORAL
        )
        
        assertTrue(board.canPlacePiece(validPiece, 0, 0))
    }
    
    @Test
    fun `out of bounds placement detection`() {
        val board = GameBoard.createEmptyBoard()
        
        // Test piece that would go out of bounds
        val invalidPiece = Piece(
            id = 1,
            cells = listOf(Cell(7, 0), Cell(8, 0)), // x=8 is out of 8x8 board
            color = PieceColor.CORAL
        )
        
        assertFalse(board.canPlacePiece(invalidPiece, 0, 0))
    }
    
    @Test
    fun `overlapping placement detection`() {
        val board = GameBoard.createEmptyBoard()
        
        // Place first piece
        val piece1 = Piece(
            id = 1,
            cells = listOf(Cell(2, 2)),
            color = PieceColor.CORAL
        )
        
        assertTrue(board.canPlacePiece(piece1, 0, 0))
        
        // Try to place second piece overlapping with the first
        val piece2 = Piece(
            id = 2,
            cells = listOf(Cell(2, 2), Cell(3, 2)),
            color = PieceColor.TEAL
        )
        
        // After placing first piece, position should be occupied
        assertFalse(board.placePiece(listOf(piece1)).canPlacePiece(piece2, 0, 0))
    }
    
    @Test
    fun `row completion detection`() {
        val board = GameBoard.createEmptyBoard()
        
        // Fill a row with cells
        val fullRowCells = (0 until 8).map { x -> Cell(x, 3) to PieceColor.CORAL }.toMap()
        val testBoard = board.copy(cells = fullRowCells)
        
        val (rowsToClear, colsToClear) = testBoard.getCompletedLines()
        
        assertEquals(1, rowsToClear.size)
        assertTrue(rowsToClear.contains(3))
        assertEquals(0, colsToClear.size)
    }
    
    @Test
    fun `column completion_detection`() {
        val board = GameBoard.createEmptyBoard()
        
        // Fill a column with cells
        val fullColCells = (0 until 8).map { y -> Cell(4, y) to PieceColor.CORAL }.toMap()
        val testBoard = board.copy(cells = fullColCells)
        
        val (rowsToClear, colsToClear) = testBoard.getCompletedLines()
        
        assertEquals(1, colsToClear.size)
        assertTrue(colsToClear.contains(4))
        assertEquals(0, rowsToClear.size)
    }
    
    @Test
    fun `clear_rows_and_columns`() {
        val board = GameBoard.createEmptyBoard()
        
        // Create a pattern that completes a row and column simultaneously
        val cells = mutableMapOf<Cell, PieceColor>()
        
        // Fill row 3
        for (x in 0 until 8) {
            cells[Cell(x, 3)] = PieceColor.CORAL
        }
        
        // Fill column 5 except at intersection (already filled)
        for (y in 0 until 8) {
            if (y != 3) {
                cells[Cell(5, y)] = PieceColor.TEAL
            }
        }
        
        val testBoard = board.copy(cells = cells)
        
        val (rowsToClear, colsToClear) = testBoard.getCompletedLines()
        
        assertEquals(1, rowsToClear.size)
        assertEquals(1, colsToClear.size)
    }
    
    @Test
    fun `piece_offset_calculation`() {
        val piece = Piece(
            id = 1,
            cells = listOf(Cell(0, 0), Cell(1, 0), Cell(2, 0)),
            color = PieceColor.CORAL
        )
        
        // Test offset by (3, 4)
        val offsetCells = piece.withOffset(3, 4)
        
        assertEquals(listOf(Cell(3, 4), Cell(4, 4), Cell(5, 4)), offsetCells)
    }
    
    @Test
    fun `piece_bounds_calculation`() {
        val piece1 = Piece(
            id = 1,
            cells = listOf(Cell(0, 0), Cell(0, 1), Cell(0, 2)),
            color = PieceColor.CORAL
        )
        
        assertEquals(1 to 3, piece1.bounds)
    }
    
    @Test
    fun `piece_bounds_wide_shape`() {
        val piece = Piece(
            id = 1,
            cells = listOf(Cell(0, 0), Cell(1, 0), Cell(2, 0), Cell(3, 0)),
            color = PieceColor.CORAL
        )
        
        assertEquals(4 to 1, piece.bounds)
    }
    
    @Test
    fun `piece_bounds_shifted_shape`() {
        val piece = Piece(
            id = 1,
            cells = listOf(Cell(2, 3), Cell(3, 3), Cell(4, 3)),
            color = PieceColor.CORAL
        )
        
        assertEquals(5 to 4, piece.bounds) // Bounds include the offset from the piece origin.
    }
}
