package org.shawnz.dailytracker.ui.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.parse.CluesBySamResult

object CluesBySamRenderer : GameRenderer<CluesBySamResult> {
    @Composable
    override fun Summary(
        result: CluesBySamResult,
        modifier: Modifier,
    ) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = result.seconds?.let(::clock) ?: stringResource(R.string.result_done),
                style = MaterialTheme.typography.bodySmall,
            )
            result.difficulty?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    /**
     * The board as written, with the time beside it.
     *
     * The emoji are not all squares. A hint is a circle and a mistake is a square, and that
     * difference is part of the board.
     */
    @Composable
    override fun Detail(
        result: CluesBySamResult,
        modifier: Modifier,
    ) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (result.grid.isNotEmpty()) EmojiText(result.grid.joinToString("\n"))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                result.seconds?.let {
                    Text(clock(it), style = MaterialTheme.typography.titleMedium)
                }
                result.difficulty?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    listOf(
                        pluralStringResource(
                            R.plurals.cluesbysam_mistakes,
                            result.mistakes,
                            result.mistakes,
                        ),
                        pluralStringResource(
                            R.plurals.cluesbysam_hints,
                            result.hints + result.doubleHints,
                            result.hints + result.doubleHints,
                        ),
                    ).joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                result.percentile?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }

    @Composable
    override fun Aggregate(
        results: List<CluesBySamResult>,
        modifier: Modifier,
    ) {
        val times = results.mapNotNull { it.seconds }
        if (times.isEmpty()) return
        val clean = results.count { it.mistakes == 0 && it.hints == 0 && it.doubleHints == 0 }
        Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.cluesbysam_best, clock(times.min())),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(R.string.cluesbysam_clean, clean * 100 / results.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    private fun clock(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
}
