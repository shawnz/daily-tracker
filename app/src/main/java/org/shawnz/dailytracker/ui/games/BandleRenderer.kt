package org.shawnz.dailytracker.ui.games

import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.parse.BandleResult

private val CORRECT = Color(0xFF6AAA64)
private val RIGHT_ARTIST = Color(0xFFD1B036)
private val WRONG = Color(0xFFCA4754)

object BandleRenderer : GameRenderer<BandleResult> {
    override fun summaryText(
        result: BandleResult,
        resources: Resources,
    ): String =
        if (result.instruments == null) {
            resources.getString(R.string.result_done)
        } else {
            "${result.step?.toString() ?: "✗"}/${result.instruments}"
        }

    @Composable
    override fun Summary(
        result: BandleResult,
        modifier: Modifier,
    ) {
        Text(
            text = summaryText(result, LocalResources.current),
            style = MaterialTheme.typography.bodySmall,
            color =
                if (result.success) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
            modifier = modifier,
        )
    }

    /**
     * A tile for every guess the song allowed, with the guess it was named on beside them.
     *
     * The emoji are plain squares, so they are drawn as tiles. A skipped turn and a turn never
     * reached are both blank in the game's own text, and telling them apart is what the two
     * shades are for.
     */
    @Composable
    override fun Detail(
        result: BandleResult,
        modifier: Modifier,
    ) {
        val skipped = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        val unreached = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (result.emoji.isNotEmpty()) {
                TileRow(result.emoji) { colourOf(it, skipped, unreached) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = summaryText(result, LocalResources.current),
                    style = MaterialTheme.typography.titleMedium,
                    color =
                        if (result.success) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                )
                if (result.bonusWon != null && result.bonusAsked != null) {
                    Text(
                        stringResource(
                            R.string.bandle_bonus,
                            result.bonusWon,
                            result.bonusAsked,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                result.puzzleNumber?.let {
                    Text(
                        "#$it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    @Composable
    override fun Aggregate(
        results: List<BandleResult>,
        modifier: Modifier,
    ) {
        val named = results.mapNotNull { it.step }
        if (named.isEmpty()) return
        Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.bandle_average, "%.2f".format(named.average())),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(R.string.bandle_named, named.size * 100 / results.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    private fun colourOf(
        codePoint: Int,
        skipped: Color,
        unreached: Color,
    ): Color =
        when (codePoint) {
            0x1F7E9 -> CORRECT
            0x1F7E8 -> RIGHT_ARTIST
            0x1F7E5 -> WRONG
            0x2B1B -> skipped
            else -> unreached
        }
}
