package org.shawnz.dailytracker.ui.games

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

val EmojiTextSize: TextUnit = 18.sp

/**
 * Draws a game's emoji as they were written.
 *
 * Use this where the emoji is a picture. When the emoji is a coloured square, draw it with
 * [TileGrid] instead.
 */
@Composable
fun EmojiText(
    emoji: String,
    modifier: Modifier = Modifier,
    size: TextUnit = EmojiTextSize,
) {
    Text(
        text = emoji,
        fontSize = size,
        lineHeight = size * 1.4f,
        modifier = modifier,
    )
}
