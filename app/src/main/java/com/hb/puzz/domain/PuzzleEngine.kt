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
    /** Rectangle layout: columns are fewer than rows so the board is portrait rather than square. */
    val gridColumns: Int = columnsForSize(gridSize)
    val gridRows: Int = rowsForSize(gridSize)
    private val totalTiles: Int = gridColumns * gridRows
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
     * Creates a fresh board with exactly zero correct neighbor connections.
     *
     * A plain random permutation can accidentally place matching source-image neighbors together,
     * which made a brand-new Adventure open at a non-zero completion percentage. We instead
     * independently scramble source rows and columns while rejecting any adjacent +1 step.
     * That guarantees no horizontal or vertical pair starts in its correct image orientation.
     */
    fun shuffle() {
        val rowOrder = shuffledAxisWithoutForwardStep(gridRows)
        val colOrder = shuffledAxisWithoutForwardStep(gridColumns)

        for (row in 0 until gridRows) {
            for (col in 0 until gridColumns) {
                val sourceRow = rowOrder[row]
                val sourceCol = colOrder[col]
                positions[row * gridColumns + col] = sourceRow * gridColumns + sourceCol
            }
        }

        check(!isSolved()) { "Fresh puzzle must not start solved" }
        check(getCorrectConnections().isEmpty()) { "Fresh puzzle must start at zero completion" }
    }

    private fun shuffledAxisWithoutForwardStep(axisSize: Int): IntArray {
        repeat(64) {
            val candidate = IntArray(axisSize) { it }
            for (i in candidate.lastIndex downTo 1) {
                val j = random.nextInt(i + 1)
                val temp = candidate[i]
                candidate[i] = candidate[j]
                candidate[j] = temp
            }
            if ((0 until candidate.lastIndex).none { candidate[it + 1] - candidate[it] == 1 }) {
                return candidate
            }
        }
        // Always valid for gridSize >= 2 and still produces zero forward-oriented neighbors.
        return IntArray(axisSize) { axisSize - 1 - it }
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
    fun getTotalPossibleConnections(): Int = gridRows * (gridColumns - 1) + gridColumns * (gridRows - 1)

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

            val tileARow = tileA / gridColumns
            val tileACol = tileA % gridColumns
            val tileBRow = tileB / gridColumns
            val tileBCol = tileB % gridColumns

            val posARow = posA / gridColumns
            val posACol = posA % gridColumns
            val posBRow = posB / gridColumns
            val posBCol = posB % gridColumns

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

        for (row in 0 until gridRows) {
            for (col in 0 until gridColumns) {
                val pos = row * gridColumns + col
                if (col + 1 < gridColumns) inspect(pos, pos + 1)
                if (row + 1 < gridRows) inspect(pos, pos + gridColumns)
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

        val anchorRow = anchorPosition / gridColumns
        val anchorCol = anchorPosition % gridColumns
        val targetRow = targetPosition / gridColumns
        val targetCol = targetPosition % gridColumns
        val deltaRow = targetRow - anchorRow
        val deltaCol = targetCol - anchorCol

        val group = getGroupForTile(anchorTileId)
        val result = linkedMapOf<Int, Int>()

        for (tileId in group) {
            val sourcePosition = getPositionOf(tileId)
            if (sourcePosition < 0) return null
            val sourceRow = sourcePosition / gridColumns
            val sourceCol = sourcePosition % gridColumns
            val movedRow = sourceRow + deltaRow
            val movedCol = sourceCol + deltaCol

            if (movedRow !in 0 until gridRows || movedCol !in 0 until gridColumns) return null
            result[tileId] = movedRow * gridColumns + movedCol
        }

        // Movement is intentionally independent from merging. A loose tile or an already
        // connected cluster may be dropped on any grid destination where its own rigid shape
        // fits. Tiles already occupying the destination footprint are reflowed into the cells
        // vacated by the moving piece in [attemptMoveGroup]. If that changes another cluster's
        // correct adjacency, connectivity is simply recalculated after the move.
        return result
    }

    /**
     * Returns true when the dragged block can be translated to [targetPosition] without leaving
     * the board.
     *
     * The dragged group itself is always rigid and can never split. Destination pieces are allowed
     * to reflow into the cells vacated by the moving group. If those destination pieces belonged to
     * another merged group, that other group may break apart as part of the reflow. This makes
     * merged-vs-merged interaction feel physical instead of blocking the player's drag.
     */
    fun canMoveGroupTo(anchorTileId: Int, targetPosition: Int): Boolean =
        getGroupMoveTargets(anchorTileId, targetPosition) != null

    /**
     * Moves an already-connected cluster as one rigid block. Tiles occupying the new footprint
     * are shifted into the cells vacated by the cluster, preserving a valid full-board permutation.
     * Existing internal connections cannot break because every member receives the same offset.
     */
    fun attemptMoveGroup(anchorTileId: Int, targetPosition: Int): Boolean {
        val anchorPosition = getPositionOf(anchorTileId)
        if (anchorPosition < 0 || targetPosition == anchorPosition) return false
        if (!canMoveGroupTo(anchorTileId, targetPosition)) return false

        val targetsByTile = getGroupMoveTargets(anchorTileId, targetPosition) ?: return false
        val group = targetsByTile.keys
        val sourcePositions = group.map { getPositionOf(it) }.toSet()
        val destinationPositions = targetsByTile.values.toSet()

        val vacated = (sourcePositions - destinationPositions).sorted()
        val incoming = (destinationPositions - sourcePositions).sorted()
        if (vacated.size != incoming.size) return false

        val before = positions.clone()
        val updated = positions.clone()

        /*
         * Reflow anything already occupying the destination footprint into the cells released by
         * the dragged block. We first try to keep an obstructing merged block rigid when its full
         * shape can fit inside the released footprint. If that is not possible, only the pieces
         * actually in the way are redistributed to the nearest available released cells.
         *
         * This is intentionally geometry-aware rather than incoming.sorted().zip(vacated.sorted()):
         * the old index-based swap could make displaced pieces jump to visually unrelated rows.
         * The new assignment minimizes travel and makes merged-vs-merged drops feel much more
         * physical while still preserving a valid full-board permutation.
         */
        val availableVacated = vacated.toMutableSet()
        val displacedTileIds = incoming.map { before[it] }.toMutableSet()
        val relocationByTile = linkedMapOf<Int, Int>()

        // Preserve a destination merged block as one rigid shape whenever the entire group is in
        // the incoming footprint and that shape can be translated into the released cells.
        val visitedBlockers = mutableSetOf<Int>()
        displacedTileIds.toList().forEach blockerLoop@ { tileId ->
            if (tileId in visitedBlockers || tileId in group) return@blockerLoop
            val blockerGroup = getGroupForTile(tileId)
            visitedBlockers.addAll(blockerGroup)
            if (blockerGroup.size <= 1 || !displacedTileIds.containsAll(blockerGroup)) return@blockerLoop

            val sourceByTile = blockerGroup.associateWith { getPositionOf(it) }
            val anchorBlocker = blockerGroup.minOrNull() ?: return@blockerLoop
            val anchorSource = sourceByTile[anchorBlocker] ?: return@blockerLoop
            val anchorSourceRow = anchorSource / gridColumns
            val anchorSourceCol = anchorSource % gridColumns

            var bestTargets: Map<Int, Int>? = null
            var bestScore = Int.MAX_VALUE
            availableVacated.forEach candidateLoop@ { candidateAnchor ->
                val candidateRow = candidateAnchor / gridColumns
                val candidateCol = candidateAnchor % gridColumns
                val deltaRow = candidateRow - anchorSourceRow
                val deltaCol = candidateCol - anchorSourceCol
                val candidateTargets = linkedMapOf<Int, Int>()
                var valid = true
                var score = 0

                blockerGroup.forEach memberLoop@ { memberTileId ->
                    val memberSource = sourceByTile[memberTileId] ?: run {
                        valid = false
                        return@memberLoop
                    }
                    val memberRow = memberSource / gridColumns
                    val memberCol = memberSource % gridColumns
                    val targetRow = memberRow + deltaRow
                    val targetCol = memberCol + deltaCol
                    if (targetRow !in 0 until gridRows || targetCol !in 0 until gridColumns) {
                        valid = false
                        return@memberLoop
                    }
                    val target = targetRow * gridColumns + targetCol
                    if (target !in availableVacated) {
                        valid = false
                        return@memberLoop
                    }
                    candidateTargets[memberTileId] = target
                    score += kotlin.math.abs(targetRow - memberRow) + kotlin.math.abs(targetCol - memberCol)
                }

                if (valid && candidateTargets.size == blockerGroup.size && score < bestScore) {
                    bestScore = score
                    bestTargets = candidateTargets
                }
            }

            bestTargets?.forEach { (memberTileId, target) ->
                relocationByTile[memberTileId] = target
                availableVacated.remove(target)
                displacedTileIds.remove(memberTileId)
            }
        }

        // Anything that could not move as a rigid block gets the nearest free released cell.
        displacedTileIds
            .sortedBy { getPositionOf(it) }
            .forEach { tileId ->
                val source = getPositionOf(tileId)
                val sourceRow = source / gridColumns
                val sourceCol = source % gridColumns
                val target = availableVacated.minWithOrNull(
                    compareBy<Int> {
                        val row = it / gridColumns
                        val col = it % gridColumns
                        kotlin.math.abs(row - sourceRow) + kotlin.math.abs(col - sourceCol)
                    }.thenBy { it }
                ) ?: return false
                relocationByTile[tileId] = target
                availableVacated.remove(target)
            }

        if (availableVacated.isNotEmpty() || relocationByTile.size != incoming.size) return false

        relocationByTile.forEach { (tileId, targetPosition) ->
            updated[targetPosition] = tileId
        }

        // Finally place every member of the rigid dragged cluster at its translated destination.
        targetsByTile.forEach { (tileId, destinationPosition) ->
            updated[destinationPosition] = tileId
        }

        if (!isValidPermutation(updated)) return false
        positions = updated
        return true
    }


    /**
     * Moves exactly one current block/group into its solved absolute location.
     * A connected group is treated as one puzzle block, so a hint never tears a merged
     * shape apart. Repeated calls monotonically lock more tiles into their true positions.
     * Returns the tile IDs moved by this assisted step, or an empty set when already solved.
     */
    fun applyHintStep(): Set<Int> {
        if (isSolved()) return emptySet()

        val seen = mutableSetOf<Int>()
        val groups = mutableListOf<Set<Int>>()
        for (tileId in 0 until totalTiles) {
            if (tileId in seen) continue
            val group = getGroupForTile(tileId)
            seen.addAll(group)
            groups += group
        }

        // Do not solve in board order. Each Hint chooses a random unsolved visual block/group,
        // so repeated hints can jump naturally around the picture instead of progressing
        // top-to-bottom or bottom-to-top. Connected pieces still move as one rigid block.
        val candidates = groups
            .filter { group -> group.any { tileId -> getPositionOf(tileId) != tileId } }
            .shuffled(random)

        for (group in candidates) {
            val misplacedTiles = group
                .filter { tileId -> getPositionOf(tileId) != tileId }
                .shuffled(random)
            val anchorTileId = misplacedTiles.firstOrNull() ?: continue
            // Tile IDs are their solved board positions, so translating this randomly chosen
            // block to its true offset creates the next assisted merge without a fixed direction.
            if (attemptMoveGroup(anchorTileId, anchorTileId)) return group
        }
        return emptySet()
    }

    fun copy(): PuzzleEngine = PuzzleEngine(gridSize).also {
        it.restorePositions(positions)
    }

    companion object {
        /**
         * Milestone portrait-grid presets. Each block is intentionally narrower than it is tall.
         * 4 -> 4x6, 5 -> 5x7, 6 -> 6x9, 7 -> 7x9, 8 -> 7x8.
         */
        fun columnsForSize(gridSize: Int): Int = when (gridSize) {
            4 -> 4
            5 -> 5
            6 -> 6
            7 -> 7
            8 -> 7
            else -> gridSize.coerceAtLeast(2)
        }

        fun rowsForSize(gridSize: Int): Int = when (gridSize) {
            4 -> 6
            5 -> 7
            6 -> 9
            7 -> 9
            8 -> 8
            else -> gridSize.coerceAtLeast(2) + 2
        }

        fun tileCountForSize(gridSize: Int): Int = columnsForSize(gridSize) * rowsForSize(gridSize)
    }
}
