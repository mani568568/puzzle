package com.hb.puzz.data

import com.hb.puzz.domain.model.*

/** Pure serialization helpers, independently testable on the JVM. */
object GameStateCodec {
        // Serialize board cells as "x,y;color" pairs separated by "|"
        fun serializeBoard(board: GameBoard): String {
            return board.cells.map { (cell, color) ->
                "${cell.x},${cell.y};${color.hex}"
            }.joinToString(separator = "|")
        }

        // Parse serialized board back
        fun parseBoard(serialized: String): GameBoard {
            val cells = mutableMapOf<Cell, PieceColor>()
            
            serialized.split("|").filter { it.isNotBlank() }.forEach { entry ->
                runCatching {
                    val parts = entry.split(";")
                    val coords = parts[0].split(",")
                    val cell = Cell(coords[0].toInt(), coords[1].toInt())
                    val color = PieceColor.entries.first { it.hex == parts[1] }
                    if (cell.x in 0..7 && cell.y in 0..7) cells[cell] = color
                }
            }

            return GameBoard(cells = cells)
        }

        // Serialize pieces for storage
        fun serializePieces(pieces: List<Piece>): String {
            return pieces.joinToString(separator = "|") { piece ->
                val cellData = piece.cells.map { "${it.x},${it.y}" }.joinToString(",")
                "${piece.id};${cellData};${piece.color.hex}"
            }
        }

        // Parse serialized pieces back
        fun parsePieces(serialized: String): List<Piece> {
            return serialized.split("|").filter { it.isNotBlank() }.mapNotNull { entry ->
                runCatching {
                    val parts = entry.split(";")
                    require(parts.size == 3)
                    val id = parts[0].toInt()
                    val color = PieceColor.entries.first { it.hex == parts[2] }
                    val coords = parts[1].split(",")
                    require(coords.size % 2 == 0)
                    val cells = coords.chunked(2).map { Cell(it[0].toInt(), it[1].toInt()) }
                    require(cells.isNotEmpty() && cells.distinct().size == cells.size)
                    require(cells.all { it.x in 0..7 && it.y in 0..7 })
                    Piece(id, cells, color)
                }.getOrNull()
            }.distinctBy { it.id }
        }
}
