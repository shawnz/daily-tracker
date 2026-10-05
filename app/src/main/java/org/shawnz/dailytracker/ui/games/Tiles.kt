package org.shawnz.dailytracker.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val TileSize = 14.dp
val TileGap = 3.dp
val TileShape: Shape = RoundedCornerShape(3.dp)

/**
 * Draws a line of emoji as tiles of one size, coloured by [colour] from each code point.
 *
 * Use this when each emoji is a coloured square, and [EmojiText] when it is a picture.
 */
@Composable
fun TileRow(
    emoji: String,
    modifier: Modifier = Modifier,
    size: Dp = TileSize,
    gap: Dp = TileGap,
    shape: Shape = TileShape,
    colour: (Int) -> Color,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(gap)) {
        emoji.codePoints().toArray().forEach { point ->
            Box(Modifier.size(size).background(colour(point), shape))
        }
    }
}

/** One [TileRow] per line of [rows]. */
@Composable
fun TileGrid(
    rows: List<String>,
    modifier: Modifier = Modifier,
    size: Dp = TileSize,
    gap: Dp = TileGap,
    shape: Shape = TileShape,
    colour: (Int) -> Color,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(gap)) {
        rows.forEach { TileRow(it, size = size, gap = gap, shape = shape, colour = colour) }
    }
}
