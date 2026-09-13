package com.hb.puzz.domain

import com.hb.puzz.domain.model.*

/**
 * Core game logic engine that handles moves and state transitions.
 */
class GameEngine(
    private val pieceGenerator: PieceGenerator = PieceGenerator(),
    initialPieces: List<Piece> = emptyList()
) {
    private var _state: GameState = if (initialPieces.isEmpty()) {
        createInitialGameState()
    } else {
        createGameStateFromPieces(initialPieces)
    }

    /**
     * Gets the current game state.
     */
    val state: GameState
        get() = _state

    /**
     * Creates initial game state with fresh board and pieces.
     */
    private fun createInitialGameState(): GameState {
        return GameState(
            board = GameBoard.createEmptyBoard(),
            pieces = pieceGenerator.generatePieces(3),
            score = 0,
            bestScore = 0
        )
    }

    /**
     * Creates a game state from existing pieces (for loading).
     */
    private fun createGameStateFromPieces(pieces: List<Piece>): GameState {
        return GameState(
            board = GameBoard.createEmptyBoard(),
            pieces = pieces,
            score = 0,
            bestScore = 0
        )
    }

    /**
     * Attempts to place the given piece at the specified position.
     * Returns true if placement was successful, false otherwise.
     */
    fun tryPlacePiece(piece: Piece, x: Int, y: Int): Boolean {
        // If game is over, don't allow moves
        if (_state.isGameOver) return false

        // Check if piece exists in current pieces
        val pieceIndex = _state.pieces.indexOfFirst { it.id == piece.id }
        if (pieceIndex == -1 || _state.pieces[pieceIndex] != piece) return false

        // Validate placement position
        if (!_state.board.canPlacePiece(piece, x, y)) {
            return false
        }

        // Create new board with placed piece
        val placedCells = piece.withOffset(x, y)
        val newCells = _state.board.cells.toMutableMap().apply {
            placedCells.forEach { cell ->
                put(cell, piece.color)
            }
        }
        
        val newBoard = GameBoard(
            width = _state.board.width,
            height = _state.board.height,
            cells = newCells
        )

        // Get completed lines
        val (rowsToClear, colsToClear) = newBoard.getCompletedLines()
        val totalLinesCleared = rowsToClear.size + colsToClear.size

        // Calculate score for this move
        val placementScore = piece.cells.size  // 1 point per cell
        val lineBonus = GameEngine.calculateLineClearBonus(totalLinesCleared)
        val linePoints = 10 * totalLinesCleared
        
        val moveScore = placementScore + linePoints + lineBonus

        // Update state with cleared lines and new score
        var updatedState = _state.copy(
            board = newBoard.clearLines(rowsToClear, colsToClear),
            score = _state.score + moveScore,
            pieces = _state.pieces.filterNot { it.id == piece.id }
        )

        // Check if we need to refill pieces
        if (updatedState.pieces.isEmpty()) {
            val newPieces = pieceGenerator.generatePieces(3)
            
            // Update state with new pieces and check game over
            updatedState = updatedState.refillPieces(newPieces)
            updatedState = updatedState.copy(isGameOver = !updatedState.hasValidMoves())
        } else {
            // Check if the current remaining pieces can still be placed
            updatedState = updatedState.copy(
                isGameOver = !updatedState.hasValidMoves()
            )
        }

        _state = updatedState.copy(bestScore = maxOf(updatedState.bestScore, updatedState.score))
        return true
    }

    /**
     * Resets the game to initial state.
     */
    fun resetGame() {
        _state = createInitialGameState().copy(bestScore = _state.bestScore)
    }

    /**
     * Updates the best score if current score is higher.
     */
    fun updateBestScore(newBest: Int) {
        _state = _state.copy(bestScore = maxOf(_state.bestScore, _state.score, newBest))
    }

    fun restoreState(saved: GameState) {
        val restored = if (saved.pieces.isEmpty()) saved.refillPieces(pieceGenerator.generatePieces(3)) else saved
        _state = restored.copy(
            bestScore = maxOf(restored.bestScore, restored.score),
            isGameOver = !restored.hasValidMoves()
        )
    }

    companion object {
        /**
         * Calculates line clear bonus: 5 × L × (L - 1)
         */
        fun calculateLineClearBonus(linesCleared: Int): Int {
            if (linesCleared <= 0) return 0
            return 5 * linesCleared * (linesCleared - 1)
        }

        /**
         * Calculates total score including placement points.
         */
        fun calculateTotalScore(placedCellsCount: Int, linesCleared: Int): Int {
            val lineBonus = calculateLineClearBonus(linesCleared)
            return placedCellsCount + (10 * linesCleared) + lineBonus
        }
    }
}
