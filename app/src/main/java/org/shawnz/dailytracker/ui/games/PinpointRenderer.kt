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
import org.shawnz.dailytracker.parse.PinpointResult

/** Pinpoint is scored by how many guesses the category took, so no time is drawn. */
object PinpointRenderer : GameRenderer<PinpointResult> {
    override fun summaryText(
        result: PinpointResult,
        resources: Resources,
    ): String =
        when (result.guesses) {
            null -> resources.getString(R.string.pinpoint_missed, result.total)
            else -> resources.getString(R.string.pinpoint_guesses, result.guesses, result.total)
        }

    @Composable
    override fun Summary(
        result: PinpointResult,
        modifier: Modifier,
    ) {
        Text(
            text = summaryText(result, LocalResources.current),
            style = MaterialTheme.typography.bodySmall,
            color =
                if (result.success == false) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            modifier = modifier,
        )
    }

    @Composable
    override fun Detail(
        result: PinpointResult,
        modifier: Modifier,
    ) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(summaryText(result, LocalResources.current), style = MaterialTheme.typography.titleMedium)
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
        results: List<PinpointResult>,
        modifier: Modifier,
    ) {
        val solved = results.mapNotNull { it.guesses }
        if (solved.isEmpty()) return
        Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.pinpoint_average, "%.1f".format(solved.average())),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(
                    R.string.pinpoint_solve_rate,
                    solved.size * 100 / results.size,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
