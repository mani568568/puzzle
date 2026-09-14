package com.hb.puzz.domain

/**
 * Legacy class from Mosaic Blocks - kept for compatibility.
 */
data class Piece(
    val id: Int,
    val cells: List<Cell>,
    val color: String
)

data class Cell(val x: Int, val y: Int)