package com.hb.puzz.domain.model

/** Represents the current block-game state. */
data class GameState(
    val board: GameBoard = GameBoard.createEmptyBoard(),
    val pieces: List<Piece> = emptyList(),
    val score: Int = 0,
    val bestScore: Int = 0,
    val isGameOver: Boolean = false
) {
    fun refillPieces(pieces: List<Piece>): GameState =
        copy(pieces = pieces, isGameOver = false)

    /** Checks whether at least one current piece can be legally placed. */
    fun hasValidMoves(): Boolean {
        if (pieces.isEmpty() || board.isFull()) return false

        return pieces.any { piece ->
            (0 until board.height).any { y ->
                (0 until board.width).any { x -> board.canPlacePiece(piece, x, y) }
            }
        }
    }

    fun calculateScoreForPlacement(pieces: List<Piece>): Int =
        pieces.sumOf { it.cells.size }

    fun calculateLineClearBonus(linesCleared: Int): Int {
        if (linesCleared <= 0) return 0
        return 5 * linesCleared * (linesCleared - 1)
    }

    fun calculateTotalScore(placementPieces: List<Piece>, linesCleared: Int): Int {
        val placementPoints = calculateScoreForPlacement(placementPieces)
        return placementPoints + (10 * linesCleared) + calculateLineClearBonus(linesCleared)
    }
}
