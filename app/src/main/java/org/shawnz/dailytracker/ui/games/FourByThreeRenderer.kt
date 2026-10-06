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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.parse.FourByThreeResult

object FourByThreeRenderer : GameRenderer<FourByThreeResult> {
    override fun summaryText(
        result: FourByThreeResult,
        resources: Resources,
    ): String =
        when {
            result.calledWrongHub -> {
                resources.getString(R.string.fourbythree_wrong_hub)
            }

            result.points == null -> {
                resources.getString(R.string.fourbythree_out_of_guesses)
            }

            else -> {
                resources.getQuantityString(
                    R.plurals.fourbythree_points,
                    result.points,
                    result.points,
                )
            }
        }

    @Composable
    override fun Summary(
        result: FourByThreeResult,
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
     * The guesses as written, with the score beside them.
     *
     * A star marks the word shared by every category, which is the point of the board, so the
     * emoji are drawn as written.
     */
    @Composable
    override fun Detail(
        result: FourByThreeResult,
        modifier: Modifier,
    ) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (result.grid.isNotEmpty()) EmojiText(result.grid.joinToString("\n"))
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
                result.mistakes?.let {
                    Text(
                        pluralStringResource(R.plurals.fourbythree_mistakes, it, it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (result.calledIt) Badge(stringResource(R.string.fourbythree_called_it))
                if (result.ruleBreaker) Badge(stringResource(R.string.fourbythree_rule_breaker))
            }
        }
    }

    @Composable
    override fun Aggregate(
        results: List<FourByThreeResult>,
        modifier: Modifier,
    ) {
        val scored = results.mapNotNull { it.points }
        if (scored.isEmpty()) return
        val solved = results.count { it.success }
        Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                pluralStringResource(
                    R.plurals.fourbythree_best,
                    scored.max(),
                    scored.max(),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(R.string.fourbythree_solved, solved * 100 / results.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    @Composable
    private fun Badge(
        text: String,
        modifier: Modifier = Modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = modifier,
        )
    }
}
