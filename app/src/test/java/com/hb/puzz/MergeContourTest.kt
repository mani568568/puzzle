package com.hb.puzz

import com.hb.puzz.domain.ContourPoint
import com.hb.puzz.domain.mergeContours
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class MergeContourTest {
    @Test fun rectangleHasNoInternalEdges() {
        val paths = mergeContours(setOf(0, 1, 4, 5), 4)
        assertEquals(1, paths.size)
        assertEquals(8, paths.single().size - 1)
        assertEquals(8, signedDoubleArea(paths.single()))
    }

    @Test fun lShapeFollowsTheIndentInsteadOfItsBoundingBox() {
        val paths = mergeContours(setOf(0, 4, 5), 4)
        assertEquals(1, paths.size)
        assertTrue(ContourPoint(1, 1) in paths.single())
        assertEquals(6, signedDoubleArea(paths.single()))
    }

    @Test fun ringKeepsAnInnerContour() {
        val paths = mergeContours((0..8).filter { it != 4 }.toSet(), 3)
        assertEquals(2, paths.size)
        assertEquals(listOf(-2, 18), paths.map(::signedDoubleArea).sorted())
    }

    @Test fun cornerTouchingCellsAndRowBoundariesStaySeparate() {
        assertEquals(2, mergeContours(setOf(0, 4), 3).size)
        assertEquals(2, mergeContours(setOf(2, 3), 3).size)
    }

    @Test fun emptyShapeHasNoContour() {
        assertTrue(mergeContours(emptySet(), 4).isEmpty())
    }

    @Test fun randomShapesAreClosedWithCorrectAreaAndPerimeter() {
        val random = Random(812)
        repeat(1000) {
            val grid = random.nextInt(2, 9)
            val cells = (0 until grid * grid).filter { random.nextBoolean() }.toSet()
            val paths = mergeContours(cells, grid)
            var edges = 0
            val unique = mutableSetOf<Pair<ContourPoint, ContourPoint>>()
            paths.forEach { path ->
                assertEquals(path.first(), path.last())
                path.zipWithNext().forEach { (a, b) ->
                    assertEquals(1, abs(a.x - b.x) + abs(a.y - b.y))
                    assertTrue(unique.add(a to b))
                    edges++
                }
            }
            val neighborPairs = cells.sumOf { cell ->
                val horizontal: Int = if (cell % grid < grid - 1 && cell + 1 in cells) 1 else 0
                val vertical: Int = if (cell + grid in cells) 1 else 0
                horizontal + vertical
            }
            assertEquals(cells.size * 4 - neighborPairs * 2, edges)
            assertEquals(cells.size * 2, paths.sumOf(::signedDoubleArea))
        }
    }

    private fun signedDoubleArea(path: List<ContourPoint>): Int =
        path.zipWithNext().sumOf { (a, b) -> a.x * b.y - b.x * a.y }
}
