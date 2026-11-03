package org.shawnz.dailytracker.ui.games

import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.parse.TimeGuessrResult

/** What one round is worth at most. */
private const val ROUND_MAX = 10000

object TimeGuessrRenderer : GameRenderer<TimeGuessrResult> {
    override fun summaryText(
        result: TimeGuessrResult,
        resources: Resources,
    ): String = result.total?.let { "%,d".format(it) }.orEmpty()

    /** A bar for each round, filled in proportion to its score. */
    @Composable
    override fun Detail(
        result: TimeGuessrResult,
        modifier: Modifier,
    ) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            result.total?.let {
                Text(
                    stringResource(R.string.timeguessr_total, "%,d".format(it)),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            result.rounds.forEach { ScoreBar(it, ROUND_MAX) }
        }
    }

    @Composable
    override fun Aggregate(
        results: List<TimeGuessrResult>,
        modifier: Modifier,
    ) {
        val totals = results.mapNotNull { it.total }
        if (totals.isEmpty()) return
        Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.timeguessr_best, "%,d".format(totals.max())),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(
                    R.string.timeguessr_average,
                    "%,d".format(totals.average().toInt()),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
