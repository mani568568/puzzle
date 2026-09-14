package com.hb.puzz.domain

import kotlin.random.Random

/** A correct relationship between two neighboring source-image tiles. */
data class TileConnection(
    val firstTileId: Int,
    val secondTileId: Int
)

/** Pure puzzle model. Tile IDs represent their solved positions. */
class PuzzleEngine(
    val gridSize: Int,
    seed: Long? = null
) {
    private val totalTiles: Int = gridSize * gridSize
    private val random = seed?.let { Random(it) } ?: Random.Default
    private var positions: IntArray = IntArray(totalTiles) { it }

    init {
        require(gridSize >= 2) { "gridSize must be at least 2" }
        shuffle()
    }

    fun getTotalTiles(): Int = totalTiles

    fun attemptSwap(posA: Int, posB: Int): Boolean {
        if (posA !in 0 until totalTiles || posB !in 0 until totalTiles || posA == posB) {
            return false
        }

        val temp = positions[posA]
        positions[posA] = positions[posB]
        positions[posB] = temp
        return true
    }

    fun isSolved(): Boolean = positions.indices.all { positions[it] == it }

    /**
     * Shuffles until the puzzle is unsolved and, when practical, avoids starting with
     * too many already-connected neighbors. This keeps the opening state feeling random
     * and gives the player more satisfying connections to discover.
     */
    fun shuffle() {
        val maxStartingConnections = (getTotalPossibleConnections() / 6).coerceAtLeast(1)
        var attempts = 0

        do {
            for (i in totalTiles - 1 downTo 1) {
                val j = random.nextInt(i + 1)
                val temp = positions[i]
                positions[i] = positions[j]
                positions[j] = temp
            }
            attempts++
        } while (
            attempts < 24 &&
            (isSolved() || getCorrectConnections().size > maxStartingConnections)
        )

        // Extremely defensive: a tiny board could theoretically still land solved.
        if (isSolved() && totalTiles > 1) {
            val temp = positions[0]
            positions[0] = positions[1]
            positions[1] = temp
        }
    }

    fun getTileAt(position: Int): Int =
        if (position in 0 until totalTiles) positions[position] else -1

    fun getPositionOf(tileId: Int): Int = positions.indexOf(tileId)

    fun getCurrentPositions(): IntArray = positions.clone()

    fun restorePositions(savedPositions: IntArray): Boolean {
        if (!isValidPermutation(savedPositions)) return false
        positions = savedPositions.clone()
        return true
    }

    fun isValidPermutation(candidate: IntArray = positions): Boolean {
        if (candidate.size != totalTiles) return false
        val seen = BooleanArray(totalTiles)
        for (tile in candidate) {
            if (tile !in 0 until totalTiles || seen[tile]) return false
            seen[tile] = true
        }
        return true
    }

    /** Number of neighbor relationships present in a completely solved grid. */
    fun getTotalPossibleConnections(): Int = 2 * gridSize * (gridSize - 1)

    /**
     * Returns every pair of tiles currently touching in the same orientation they have
     * in the original image. A pair can therefore be correctly connected even while the
     * whole group is temporarily located elsewhere on the board.
     */
    fun getCorrectConnections(): Set<TileConnection> {
        val connections = linkedSetOf<TileConnection>()

        fun inspect(posA: Int, posB: Int) {
            val tileA = positions[posA]
            val tileB = positions[posB]

            val tileARow = tileA / gridSize
            val tileACol = tileA % gridSize
            val tileBRow = tileB / gridSize
            val tileBCol = tileB % gridSize

            val posARow = posA / gridSize
            val posACol = posA % gridSize
            val posBRow = posB / gridSize
            val posBCol = posB % gridSize

            if (
                tileBRow - tileARow == posBRow - posARow &&
                tileBCol - tileACol == posBCol - posACol
            ) {
                connections += TileConnection(
                    firstTileId = minOf(tileA, tileB),
                    secondTileId = maxOf(tileA, tileB)
                )
            }
        }

        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val pos = row * gridSize + col
                if (col + 1 < gridSize) inspect(pos, pos + 1)
                if (row + 1 < gridSize) inspect(pos, pos + gridSize)
            }
        }

        return connections
    }

    /**
     * Returns groups of two or more tiles joined by correct neighbor relationships.
     */
    fun getConnectedGroups(): List<List<Int>> {
        val adjacency = Array(totalTiles) { mutableSetOf<Int>() }
        getCorrectConnections().forEach { connection ->
            adjacency[connection.firstTileId].add(connection.secondTileId)
            adjacency[connection.secondTileId].add(connection.firstTileId)
        }

        val visited = BooleanArray(totalTiles)
        val groups = mutableListOf<List<Int>>()
        for (tile in 0 until totalTiles) {
            if (visited[tile] || adjacency[tile].isEmpty()) continue
            val queue = ArrayDeque<Int>()
            val group = mutableListOf<Int>()
            queue.add(tile)
            visited[tile] = true
            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                group.add(current)
                adjacency[current].forEach { next ->
                    if (!visited[next]) {
                        visited[next] = true
                        queue.add(next)
                    }
                }
            }
            if (group.size > 1) groups.add(group.sorted())
        }
        return groups
    }

    fun copy(): PuzzleEngine = PuzzleEngine(gridSize).also {
        it.restorePositions(positions)
    }
}
