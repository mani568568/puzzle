package com.hb.puzz.domain

/**
 * Legacy class from Mosaic Blocks - kept for compatibility.
 */
data class GameBoard(
    val width: Int = 8,
    val height: Int = 8,
    val cells: Map<Int, String> = emptyMap()
) {
    fun isFull(): Boolean = cells.size == width * height
}
