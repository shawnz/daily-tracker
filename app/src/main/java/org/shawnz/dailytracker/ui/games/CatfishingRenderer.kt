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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.parse.CatfishingResult

object CatfishingRenderer : GameRenderer<CatfishingResult> {
    override fun summaryText(
        result: CatfishingResult,
        resources: Resources,
    ): String =
        if (result.score != null && result.total != null) {
            "${points(result.score)}/${result.total}"
        } else {
            resources.getString(R.string.result_done)
        }

    /** The articles as written, with the score beside them. */
    @Composable
    override fun Detail(
        result: CatfishingResult,
        modifier: Modifier,
    ) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (result.grid.isNotEmpty()) EmojiText(result.grid.joinToString("\n"))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
    }

    @Composable
    override fun Aggregate(
        results: List<CatfishingResult>,
        modifier: Modifier,
    ) {
        val scores = results.mapNotNull { it.score }
        if (scores.isEmpty()) return
        Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.catfishing_best, points(scores.max())),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(
                    R.string.catfishing_average,
                    "%.1f".format(scores.average()),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    /** A whole score is written without a decimal point, as in the share text. */
    private fun points(score: Double): String = if (score % 1.0 == 0.0) score.toInt().toString() else score.toString()
}
