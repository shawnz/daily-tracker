package org.shawnz.dailytracker.ui.today

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt

private val TileSize = 40.dp

/** The fraction of the height, measured from the bottom, that has tiles. */
private const val BAND_HEIGHT = 0.75f

/** The opacity of the bottom row. */
private const val MAX_ALPHA = 0.08f

/**
 * The exponents of the fade when the content is above all of the tiles, and when it is drawn
 * over all of them. With a higher exponent, fewer rows are visible.
 */
private const val MIN_GAMMA = 2f
private const val MAX_GAMMA = 12f

/** How often a cell is filled, outlined or empty, relative to each other. */
private const val FILLED_FREQUENCY = 4
private const val OUTLINED_FREQUENCY = 2
private const val EMPTY_FREQUENCY = 2

private const val SEED = 7L

/**
 * Draws a faint grid of the launcher icon's tiles in [color] behind the content. Each cell is
 * filled, outlined or empty.
 *
 * The grid continues without end to both sides. [position] is the horizontal scroll position, in
 * widths of the content. A column is centred on each side edge and a row on the bottom edge, so the
 * tiles there are cut in half and the grid looks like part of a larger one.
 *
 * The tiles fade out towards the top. [coverage] is the fraction of the band of tiles that is
 * behind the content, from [tileCoverage]. The larger it is, the faster the fade, so that fewer
 * tiles are visible behind the content.
 */
internal fun Modifier.tileBackdrop(
    color: Color,
    position: () -> Double,
    coverage: () -> Float,
): Modifier =
    drawWithCache {
        // A whole number of columns fits the width, so the tile size is scaled to the width.
        val columns = (size.width / (TileSize.toPx() * 5 / 4)).roundToInt().coerceAtLeast(1)
        val pitch = size.width / columns
        // The proportions of art/icon-monochrome.svg, where a tile is 128 units and a gutter 32.
        val tile = pitch * 4 / 5
        val stroke = tile / 8
        val filledRadius = CornerRadius(tile * 30 / 128)
        val outlinedRadius = CornerRadius(tile * 22 / 128)
        val rows = ceil(size.height * BAND_HEIGHT / pitch).toInt()
        val mirrored = layoutDirection == LayoutDirection.Rtl

        onDrawBehind {
            val gamma = MIN_GAMMA + (MAX_GAMMA - MIN_GAMMA) * coverage()
            val scroll = position() * columns
            val firstColumn = floor(scroll).toLong()
            val shift = ((scroll - firstColumn) * pitch).toFloat()
            for (row in 0 until rows) {
                val alpha = MAX_ALPHA * ((rows - row).toFloat() / rows).pow(gamma)
                val top = size.height - tile / 2 - row * pitch
                // Without a scroll, columns 0 and [columns] are centred on the edges. During a
                // scroll, column [columns] + 1 can also be partly visible.
                for (i in 0..columns + 1) {
                    val kind = cellKind(row, firstColumn + i)
                    if (kind >= FILLED_FREQUENCY + OUTLINED_FREQUENCY) continue
                    val centre = i * pitch - shift
                    val topLeft = Offset((if (mirrored) size.width - centre else centre) - tile / 2, top)
                    if (kind >= FILLED_FREQUENCY) {
                        // Stroked inside the tile's square, so it occupies the same area as a
                        // filled one.
                        drawRoundRect(
                            color = color,
                            topLeft = topLeft + Offset(stroke / 2, stroke / 2),
                            size = Size(tile - stroke, tile - stroke),
                            cornerRadius = outlinedRadius,
                            style = Stroke(stroke),
                            alpha = alpha,
                        )
                    } else {
                        drawRoundRect(
                            color = color,
                            topLeft = topLeft,
                            size = Size(tile, tile),
                            cornerRadius = filledRadius,
                            alpha = alpha,
                        )
                    }
                }
            }
        }
    }

/**
 * The fraction of the band of tiles of a [tileBackdrop] that is behind its content. [contentBottom]
 * is the distance from the top of the backdrop to the bottom of the content, and [height] is the
 * height of the backdrop.
 */
internal fun tileCoverage(
    contentBottom: Float,
    height: Float,
): Float {
    if (height <= 0f) return 0f
    val band = height * BAND_HEIGHT
    return ((contentBottom - (height - band)) / band).coerceIn(0f, 1f)
}

/**
 * The kind of the cell at [row] and [column]: filled below [FILLED_FREQUENCY], then outlined below
 * the sum with [OUTLINED_FREQUENCY], then empty. It depends only on the place of the cell, so the
 * cell is the same at every scroll position.
 */
private fun cellKind(
    row: Int,
    column: Long,
): Int {
    // One step of SplitMix64 with the place of the cell as its state, so that the kinds of
    // neighbouring cells are unrelated.
    var z = (column shl 32) + row + SEED + 0x9E3779B97F4A7C15uL.toLong()
    z = (z xor (z ushr 30)) * 0xBF58476D1CE4E5B9uL.toLong()
    z = (z xor (z ushr 27)) * 0x94D049BB133111EBuL.toLong()
    z = z xor (z ushr 31)
    return z.mod(FILLED_FREQUENCY + OUTLINED_FREQUENCY + EMPTY_FREQUENCY)
}
