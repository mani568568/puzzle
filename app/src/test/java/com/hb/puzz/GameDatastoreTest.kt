package com.hb.puzz

import com.hb.puzz.data.GameStateCodec
import com.hb.puzz.domain.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for GameDatastore serialization functions.
 */
class GameDatastoreTest {
    
    @Test
    fun `serialize_and_parse_board`() = runBlocking {
        val board = GameBoard.createEmptyBoard()
        
        // Add some cells to the board
        val cells = mapOf(
            Cell(0, 0) to PieceColor.CORAL,
            Cell(3, 4) to PieceColor.TEAL,
            Cell(7, 7) to PieceColor.BLUE
        )
        
        val testBoard = board.copy(cells = cells)
        
        // Serialize
        val serialized = GameStateCodec.serializeBoard(testBoard)
        
        // Parse back
        val parsedBoard = GameStateCodec.parseBoard(serialized)
        
        // Verify cells match
        assertEquals(testBoard.cells.size, parsedBoard.cells.size)
        
        testBoard.cells.forEach { (cell, color) ->
            assertEquals(color, parsedBoard.cells[cell])
        }
    }
    
    @Test
    fun `serialize_and_parse_piece`() = runBlocking {
        val piece = Piece(
            id = 42,
            cells = listOf(Cell(0, 0), Cell(1, 0), Cell(2, 0)),
            color = PieceColor.CORAL
        )
        
        // Serialize
        val serialized = GameStateCodec.serializePieces(listOf(piece))
        
        // Parse back
        val parsedPieces = GameStateCodec.parsePieces(serialized)
        
        assertEquals(1, parsedPieces.size)
        assertEquals(piece.id, parsedPieces[0].id)
        assertEquals(piece.cells.size, parsedPieces[0].cells.size)
        assertEquals(piece.color, parsedPieces[0].color)
    }
    
    @Test
    fun `serialize_and_parse_multiple_pieces`() = runBlocking {
        val pieces = listOf(
            Piece(1, listOf(Cell(0, 0)), PieceColor.CORAL),
            Piece(2, listOf(Cell(0, 0), Cell(0, 1)), PieceColor.TEAL),
            Piece(3, listOf(Cell(0, 0), Cell(1, 0), Cell(2, 0)), PieceColor.BLUE)
        )
        
        // Serialize
        val serialized = GameStateCodec.serializePieces(pieces)
        
        // Parse back
        val parsedPieces = GameStateCodec.parsePieces(serialized)
        
        assertEquals(pieces.size, parsedPieces.size)
    }
    
    @Test
    fun `serialize_empty_board`() = runBlocking {
        val board = GameBoard.createEmptyBoard()
        
        val serialized = GameStateCodec.serializeBoard(board)
        
        // Empty map should serialize to empty string or handle gracefully
        assertNotNull(serialized)
    }
    
    @Test
    fun `parse_invalid_serialization_returns_graceful_result`() = runBlocking {
        // Test parsing malformed data
        val result = GameStateCodec.parsePieces("invalid;data")
        
        // Should return empty list for invalid input
        assertEquals(0, result.size)
    }
}
