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
import org.shawnz.dailytracker.parse.HuedResult

object HuedRenderer : GameRenderer<HuedResult> {
    override fun summaryText(
        result: HuedResult,
        resources: Resources,
    ): String = result.accuracy?.let { resources.getString(R.string.hued_accuracy, it) }.orEmpty()

    @Composable
    override fun Detail(
        result: HuedResult,
        modifier: Modifier,
    ) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                listOfNotNull(summaryText(result, LocalResources.current).ifEmpty { null }, result.colorName)
                    .joinToString(" "),
                style = MaterialTheme.typography.titleMedium,
            )
            result.guesses?.let {
                Text(
                    stringResource(R.string.hued_guesses, it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (result.hintUsed) {
                Text(
                    stringResource(R.string.hued_hint_used),
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

    @Composable
    override fun Aggregate(
        results: List<HuedResult>,
        modifier: Modifier,
    ) {
        val accuracies = results.mapNotNull { it.accuracy }
        if (accuracies.isEmpty()) return
        Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.hued_best, accuracies.max()),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(R.string.hued_average, accuracies.average().toInt()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
