package com.hb.puzz.domain

/**
 * Legacy class from Mosaic Blocks - kept for compatibility.
 */
data class GameState(
    val board: GameBoard = GameBoard(),
    val pieces: List<Piece> = emptyList(),
    val score: Int = 0,
    val isGameOver: Boolean = false
)
