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
import org.shawnz.dailytracker.parse.KrillionResult

object KrillionRenderer : GameRenderer<KrillionResult> {
    @Composable
    override fun Summary(
        result: KrillionResult,
        modifier: Modifier,
    ) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = result.score?.toString().orEmpty(),
                style = MaterialTheme.typography.bodySmall,
            )
            result.band?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    /**
     * The prompts in the order they were answered, with the depth beside them.
     *
     * The emoji are drawn as written. Each one indicates how rare that answer was.
     */
    @Composable
    override fun Detail(
        result: KrillionResult,
        modifier: Modifier,
    ) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (result.emoji.isNotEmpty()) EmojiText(result.emoji)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = result.score?.toString().orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                )
                result.band?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (result.misses > 0) {
                    Text(
                        text =
                            pluralStringResource(
                                R.plurals.krillion_misses,
                                result.misses,
                                result.misses,
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    @Composable
    override fun Aggregate(
        results: List<KrillionResult>,
        modifier: Modifier,
    ) {
        val scores = results.mapNotNull { it.score }
        if (scores.isEmpty()) return
        Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.krillion_deepest, scores.max()),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(R.string.krillion_average, scores.average().toInt()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
