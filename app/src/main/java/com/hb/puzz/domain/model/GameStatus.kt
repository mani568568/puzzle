package com.hb.puzz.domain.model

/**
 * Represents the current game state.
 */
data class GameState(
    val board: GameBoard = GameBoard.createEmptyBoard(),
    val pieces: List<Piece> = emptyList(),
    val score: Int = 0,
    val bestScore: Int = 0,
    val isGameOver: Boolean = false
) {
    /**
     * Creates a new game state with updated pieces and resets gameOver status.
     */
    fun refillPieces(pieces: List<Piece>): GameState {
        return copy(
            pieces = pieces,
            isGameOver = false
        )
    }

    /**
     * Checks if there are valid moves left for any of the current pieces.
     */
    fun hasValidMoves(): Boolean {
        if (pieces.isEmpty() || board.isFull()) {
            return false
        }

        return pieces.any { piece ->
            (0 until board.height).any { y ->
                (0 until board.width).any { x -> board.canPlacePiece(piece, x, y) }
            }
        }
    }

    /**
     * Calculates score for placing a piece.
     */
    fun calculateScoreForPlacement(pieces: List<Piece>): Int {
        // 1 point per placed cell
        val placementPoints = pieces.sumOf { it.cells.size }

        return placementPoints
    }

    /**
     * Calculates bonus points for clearing lines.
     */
    fun calculateLineClearBonus(linesCleared: Int): Int {
        if (linesCleared <= 0) return 0
        
        // L lines award additional 5 × L × (L - 1) points
        return 5 * linesCleared * (linesCleared - 1)
    }

    /**
     * Returns total score including placement and line clear bonus.
     */
    fun calculateTotalScore(placementPieces: List<Piece>, linesCleared: Int): Int {
        val placementPoints = calculateScoreForPlacement(placementPieces)
        val lineBonus = calculateLineClearBonus(linesCleared)
        
        return placementPoints + (10 * linesCleared) + lineBonus
    }
}
