package org.shawnz.dailytracker.ui.today

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.random.Random

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

private const val SEED = 7

private class Tile(
    val topLeft: Offset,
    val outlined: Boolean,
    val alpha: Float,
)

/**
 * Draws a faint grid of the launcher icon's tiles in [color] behind the content. Each cell is
 * filled, outlined or empty. The tiles at the left, right and bottom edges are cut off, so the
 * grid looks like part of a larger one.
 *
 * The tiles fade out towards the top. [contentBottom] is the distance from the top edge to the
 * bottom of the content. The larger it is, the faster the fade, so that fewer tiles are visible
 * behind the content.
 */
internal fun Modifier.tileBackdrop(
    color: Color,
    contentBottom: Density.() -> Float,
): Modifier =
    drawWithCache {
        // The proportions of art/icon-monochrome.svg, where a tile is 128 units.
        val tile = TileSize.toPx()
        val gutter = tile / 4
        val pitch = tile + gutter
        val stroke = tile / 8
        val filledRadius = CornerRadius(tile * 30 / 128)
        val outlinedRadius = CornerRadius(tile * 22 / 128)

        // Centred, so the same width is cut off the first and last columns. The same height is
        // cut off the bottom row.
        val columns = ceil((size.width + gutter) / pitch).toInt()
        val overhang = (columns * pitch - gutter - size.width) / 2
        val firstColumnLeft = -overhang
        val bottomRowTop = size.height - tile + overhang

        val band = size.height * BAND_HEIGHT
        val rows = ceil(band / pitch).toInt()
        val covered = ((contentBottom() - (size.height - band)) / band).coerceIn(0f, 1f)
        val gamma = MIN_GAMMA + (MAX_GAMMA - MIN_GAMMA) * covered

        val random = Random(SEED)
        val drawn = FILLED_FREQUENCY + OUTLINED_FREQUENCY
        val tiles =
            buildList {
                for (row in 0 until rows) {
                    val alpha = MAX_ALPHA * ((rows - row).toFloat() / rows).pow(gamma)
                    for (column in 0 until columns) {
                        val kind = random.nextInt(drawn + EMPTY_FREQUENCY)
                        if (kind >= drawn) continue
                        add(
                            Tile(
                                topLeft =
                                    Offset(
                                        x = firstColumnLeft + column * pitch,
                                        y = bottomRowTop - row * pitch,
                                    ),
                                outlined = kind >= FILLED_FREQUENCY,
                                alpha = alpha,
                            ),
                        )
                    }
                }
            }

        onDrawBehind {
            for (t in tiles) {
                if (t.outlined) {
                    // Stroked inside the tile's square, so it occupies the same area as a
                    // filled one.
                    drawRoundRect(
                        color = color,
                        topLeft = t.topLeft + Offset(stroke / 2, stroke / 2),
                        size = Size(tile - stroke, tile - stroke),
                        cornerRadius = outlinedRadius,
                        style = Stroke(stroke),
                        alpha = t.alpha,
                    )
                } else {
                    drawRoundRect(
                        color = color,
                        topLeft = t.topLeft,
                        size = Size(tile, tile),
                        cornerRadius = filledRadius,
                        alpha = t.alpha,
                    )
                }
            }
        }
    }
