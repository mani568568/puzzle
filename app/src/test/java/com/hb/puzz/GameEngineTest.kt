package com.hb.puzz

import com.hb.puzz.domain.GameEngine
import com.hb.puzz.domain.model.*
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class GameEngineTest {
    private fun single(id: Int = 0) = Piece(id, listOf(Cell(0, 0)), PieceColor.CORAL)
    private fun engine(pieces: List<Piece>) = GameEngine(PieceGenerator(Random(42)), pieces)

    @Test fun initialState() {
        val engine = GameEngine()
        assertEquals(3, engine.state.pieces.size)
        assertTrue(engine.state.board.cells.isEmpty())
        assertFalse(engine.state.isGameOver)
    }
    @Test fun rejectsOutOfBoundsAndOverlap() {
        val piece = single()
        val other = single(1)
        val engine = engine(listOf(piece, other))
        assertFalse(engine.tryPlacePiece(piece, -1, 0))
        assertFalse(engine.tryPlacePiece(piece, 8, 0))
        assertTrue(engine.tryPlacePiece(piece, 7, 7))
        assertFalse(engine.tryPlacePiece(other, 7, 7))
        assertEquals(1, engine.state.score)
    }
    @Test fun rejectsDifferentPieceWithSameId() {
        val piece = single()
        val engine = engine(listOf(piece))
        assertFalse(engine.tryPlacePiece(piece.copy(cells = listOf(Cell(0, 0), Cell(1, 0))), 0, 0))
    }
    @Test fun rowClearsAndScores() {
        val piece = Piece(0, (0..7).map { Cell(it, 0) }, PieceColor.CORAL)
        val engine = engine(listOf(piece))
        assertTrue(engine.tryPlacePiece(piece, 0, 0))
        assertEquals(18, engine.state.score)
        assertTrue(engine.state.board.cells.isEmpty())
        assertEquals(3, engine.state.pieces.size)
        assertFalse(engine.state.isGameOver)
    }
    @Test fun columnClearsAndScores() {
        val piece = Piece(0, (0..7).map { Cell(0, it) }, PieceColor.TEAL)
        val engine = engine(listOf(piece))
        assertTrue(engine.tryPlacePiece(piece, 0, 0))
        assertEquals(18, engine.state.score)
        assertTrue(engine.state.board.cells.isEmpty())
    }
    @Test fun crossingLinesClearOnceAndScoreBoth() {
        val piece = single()
        val engine = engine(listOf(piece))
        val cells = ((1..7).map { Cell(it, 0) } + (1..7).map { Cell(0, it) })
            .associateWith { PieceColor.BLUE }
        engine.restoreState(GameState(GameBoard(cells = cells), listOf(piece)))
        assertTrue(engine.tryPlacePiece(piece, 0, 0))
        assertEquals(31, engine.state.score)
        assertTrue(engine.state.board.cells.isEmpty())
    }
    @Test fun lastPieceRefillsWithoutFalseGameOver() {
        val piece = single()
        val engine = engine(listOf(piece))
        assertTrue(engine.tryPlacePiece(piece, 3, 3))
        assertEquals(3, engine.state.pieces.size)
        assertFalse(engine.state.isGameOver)
    }
    @Test fun findsMoveAtBottomRightOfBoard() {
        val cells = (0..7).flatMap { y -> (0..7).map { x -> Cell(x, y) } }
            .filterNot { it == Cell(7, 7) }.associateWith { PieceColor.CORAL }
        assertTrue(GameState(GameBoard(cells = cells), listOf(single())).hasValidMoves())
    }
    @Test fun detectsNoMoves() {
        val cells = (0..7).flatMap { y -> (0..7).map { x -> Cell(x, y) } }
            .associateWith { PieceColor.CORAL }
        val engine = engine(listOf(single()))
        engine.restoreState(GameState(GameBoard(cells = cells), listOf(single())))
        assertTrue(engine.state.isGameOver)
    }
    @Test fun bestScoreSurvivesResetAndCannotDecrease() {
        val piece = single()
        val engine = engine(listOf(piece))
        engine.updateBestScore(100)
        engine.tryPlacePiece(piece, 0, 0)
        engine.updateBestScore(1)
        engine.resetGame()
        assertEquals(100, engine.state.bestScore)
        assertEquals(0, engine.state.score)
    }
    @Test fun scoringFormula() { assertEquals(154, GameEngine.calculateTotalScore(4, 5)) }
}
