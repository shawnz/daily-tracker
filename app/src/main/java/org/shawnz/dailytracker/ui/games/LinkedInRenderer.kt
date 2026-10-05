package org.shawnz.dailytracker.ui.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.parse.LinkedInResult

/** Draws the timed LinkedIn games. */
object LinkedInRenderer : GameRenderer<LinkedInResult> {
    @Composable
    override fun Summary(
        result: LinkedInResult,
        modifier: Modifier,
    ) {
        Text(
            text = result.seconds?.let(::clock) ?: stringResource(R.string.result_done),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
    }

    @Composable
    override fun Detail(
        result: LinkedInResult,
        modifier: Modifier,
    ) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = result.seconds?.let(::clock) ?: stringResource(R.string.result_done),
                style = MaterialTheme.typography.titleMedium,
            )
            result.puzzleNumber?.let {
                Text(
                    "#$it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    @Composable
    override fun Aggregate(
        results: List<LinkedInResult>,
        modifier: Modifier,
    ) {
        val times = results.mapNotNull { it.seconds }
        if (times.isEmpty()) return
        Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.linkedin_best, clock(times.min())),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(R.string.linkedin_average, clock(times.average().toInt())),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    private fun clock(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
}
