package org.shawnz.dailytracker.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val BAR_SHAPE = RoundedCornerShape(2.dp)

/** A bar filled in proportion to [score] out of [max], with the score beside it. */
@Composable
fun ScoreBar(
    score: Int,
    max: Int,
    modifier: Modifier = Modifier,
) {
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val fill = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .width(120.dp)
                .height(8.dp)
                .background(track, BAR_SHAPE),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(score.coerceIn(0, max) / max.toFloat())
                    .fillMaxHeight()
                    .background(fill, BAR_SHAPE),
            )
        }
        Text(
            "%,d".format(score),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
