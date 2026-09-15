package com.hb.puzz.domain

/** Grid corners in board coordinates; independent of screen density and Compose. */
internal data class ContourPoint(val x: Int, val y: Int)

/**
 * Closed, ordered contours around occupied cells, including holes. Shared edges are omitted.
 * Keeping the occupied cell on the right gives a deterministic clockwise outer boundary.
 */
internal fun mergeContours(positions: Set<Int>, gridSize: Int): List<List<ContourPoint>> {
    require(gridSize > 0)
    require(positions.all { it in 0 until gridSize * gridSize })
    data class Edge(val start: ContourPoint, val end: ContourPoint, val direction: Int)
    val edges = mutableListOf<Edge>()
    for (position in positions.sorted()) {
        val x = position % gridSize
        val y = position / gridSize
        if (y == 0 || position - gridSize !in positions)
            edges += Edge(ContourPoint(x, y), ContourPoint(x + 1, y), 0)
        if (x == gridSize - 1 || position + 1 !in positions)
            edges += Edge(ContourPoint(x + 1, y), ContourPoint(x + 1, y + 1), 1)
        if (y == gridSize - 1 || position + gridSize !in positions)
            edges += Edge(ContourPoint(x + 1, y + 1), ContourPoint(x, y + 1), 2)
        if (x == 0 || position - 1 !in positions)
            edges += Edge(ContourPoint(x, y + 1), ContourPoint(x, y), 3)
    }
    val remaining = edges.toMutableSet()
    val outgoing = edges.groupBy { it.start }
    val result = mutableListOf<List<ContourPoint>>()
    while (remaining.isNotEmpty()) {
        val first = remaining.first()
        var edge = first
        val contour = mutableListOf(first.start)
        do {
            remaining.remove(edge)
            contour += edge.end
            if (edge.end == first.start) break
            // At a corner-touching junction, turn right to stay on the same boundary.
            edge = outgoing.getValue(edge.end).filter { it in remaining }.minBy { candidate ->
                when ((candidate.direction - edge.direction + 4) % 4) {
                    1 -> 0
                    0 -> 1
                    3 -> 2
                    else -> 3
                }
            }
        } while (true)
        result += contour
    }
    return result
}

/** One shared timeline prevents state cleanup or the result card cutting the effect short. */
internal object MergeMotion {
    const val DURATION_MILLIS = 1600
    const val CLEAR_DELAY_MILLIS = 1800L
    const val RESULT_DELAY_MILLIS = 1900L
}
