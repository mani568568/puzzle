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

    /**
     * Legacy single-tile swap retained for tests/backward compatibility.
     * Normal gameplay now uses [attemptMoveGroup].
     */
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
     * too many already-connected neighbors. This keeps the opening state feeling random.
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

    /** Returns groups of two or more tiles joined by correct neighbor relationships. */
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

    /**
     * Returns the rigid cluster containing [tileId]. A loose tile is a one-item cluster.
     * Correctly joined pieces therefore automatically become one movable unit.
     */
    fun getGroupForTile(tileId: Int): Set<Int> {
        if (tileId !in 0 until totalTiles) return emptySet()
        return getConnectedGroups()
            .firstOrNull { tileId in it }
            ?.toSet()
            ?: setOf(tileId)
    }

    /**
     * Calculates the destination board position for every tile in the cluster containing
     * [anchorTileId] if that cluster were translated so the anchor lands on [targetPosition].
     * Returns null when that rigid translation would leave the board.
     */
    fun getGroupMoveTargets(anchorTileId: Int, targetPosition: Int): Map<Int, Int>? {
        if (anchorTileId !in 0 until totalTiles || targetPosition !in 0 until totalTiles) {
            return null
        }

        val anchorPosition = getPositionOf(anchorTileId)
        if (anchorPosition < 0) return null

        val anchorRow = anchorPosition / gridSize
        val anchorCol = anchorPosition % gridSize
        val targetRow = targetPosition / gridSize
        val targetCol = targetPosition % gridSize
        val deltaRow = targetRow - anchorRow
        val deltaCol = targetCol - anchorCol

        val group = getGroupForTile(anchorTileId)
        val result = linkedMapOf<Int, Int>()

        for (tileId in group) {
            val sourcePosition = getPositionOf(tileId)
            if (sourcePosition < 0) return null
            val sourceRow = sourcePosition / gridSize
            val sourceCol = sourcePosition % gridSize
            val movedRow = sourceRow + deltaRow
            val movedCol = sourceCol + deltaCol

            if (movedRow !in 0 until gridSize || movedCol !in 0 until gridSize) return null
            result[tileId] = movedRow * gridSize + movedCol
        }

        return result
    }

    /**
     * Moves an already-connected cluster as one rigid block. Tiles occupying the new footprint
     * are shifted into the cells vacated by the cluster, preserving a valid full-board permutation.
     * Existing internal connections cannot break because every member receives the same offset.
     */
    fun attemptMoveGroup(anchorTileId: Int, targetPosition: Int): Boolean {
        val anchorPosition = getPositionOf(anchorTileId)
        if (anchorPosition < 0 || targetPosition == anchorPosition) return false

        val targetsByTile = getGroupMoveTargets(anchorTileId, targetPosition) ?: return false
        val group = targetsByTile.keys
        val sourcePositions = group.map { getPositionOf(it) }.toSet()
        val destinationPositions = targetsByTile.values.toSet()

        val vacated = (sourcePositions - destinationPositions).sorted()
        val incoming = (destinationPositions - sourcePositions).sorted()
        if (vacated.size != incoming.size) return false

        val before = positions.clone()
        val updated = positions.clone()

        // Move displaced loose/other-group tiles into the cells the moving cluster leaves behind.
        incoming.zip(vacated).forEach { (incomingPosition, vacatedPosition) ->
            updated[vacatedPosition] = before[incomingPosition]
        }

        // Finally place every member of the rigid cluster at its translated destination.
        targetsByTile.forEach { (tileId, destinationPosition) ->
            updated[destinationPosition] = tileId
        }

        if (!isValidPermutation(updated)) return false
        positions = updated
        return true
    }

    fun copy(): PuzzleEngine = PuzzleEngine(gridSize).also {
        it.restorePositions(positions)
    }
}
