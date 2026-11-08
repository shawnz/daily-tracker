package org.shawnz.dailytracker.parse

/**
 * Whether the line is drawn out of [tiles] and nothing else.
 *
 * Compares code points, because most of these squares are above the Basic Multilingual Plane
 * and do not fit in one Char.
 */
fun isGridRow(
    line: String,
    tiles: Set<Int>,
): Boolean {
    if (line.isEmpty()) return false
    val points = line.codePoints().toArray()
    return points.isNotEmpty() && points.all { it in tiles }
}

/** How many times [tile] is drawn across [lines]. */
fun countTiles(
    lines: List<String>,
    tile: Int,
): Int =
    lines.sumOf { line ->
        line
            .codePoints()
            .filter { it == tile }
            .count()
            .toInt()
    }
