package org.shawnz.dailytracker.ui.games

import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.parse.FoodGuessrResult

/** What one round is worth at most. */
private const val ROUND_MAX = 5000

private val BAR_SHAPE = RoundedCornerShape(2.dp)

object FoodGuessrRenderer : GameRenderer<FoodGuessrResult> {
    override fun summaryText(
        result: FoodGuessrResult,
        resources: Resources,
    ): String = result.total?.let { "%,d".format(it) }.orEmpty()

    /** A bar for each round, filled in proportion to its score. */
    @Composable
    override fun Detail(
        result: FoodGuessrResult,
        modifier: Modifier,
    ) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            result.total?.let {
                Text(
                    stringResource(R.string.foodguessr_total, "%,d".format(it)),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            result.rounds.forEach { RoundBar(it) }
        }
    }

    @Composable
    private fun RoundBar(
        score: Int,
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
                        .fillMaxWidth(score.coerceIn(0, ROUND_MAX) / ROUND_MAX.toFloat())
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

    @Composable
    override fun Aggregate(
        results: List<FoodGuessrResult>,
        modifier: Modifier,
    ) {
        val totals = results.mapNotNull { it.total }
        if (totals.isEmpty()) return
        Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.foodguessr_best, "%,d".format(totals.max())),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(
                    R.string.foodguessr_average,
                    "%,d".format(totals.average().toInt()),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
