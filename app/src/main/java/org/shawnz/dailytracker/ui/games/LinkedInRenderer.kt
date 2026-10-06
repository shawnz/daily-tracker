package org.shawnz.dailytracker.ui.games

import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.parse.LinkedInResult

/** Draws the timed LinkedIn games. */
object LinkedInRenderer : GameRenderer<LinkedInResult> {
    override fun summaryText(
        result: LinkedInResult,
        resources: Resources,
    ): String = result.seconds?.let(::clock) ?: resources.getString(R.string.result_done)

    @Composable
    override fun Detail(
        result: LinkedInResult,
        modifier: Modifier,
    ) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = summaryText(result, LocalResources.current),
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
