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
import org.shawnz.dailytracker.parse.KindaHardGolfResult

object KindaHardGolfRenderer : GameRenderer<KindaHardGolfResult> {
    @Composable
    override fun Summary(
        result: KindaHardGolfResult,
        modifier: Modifier,
    ) {
        Text(
            text =
                result.strokes?.let { pluralStringResource(R.plurals.golf_strokes, it, it) }
                    ?: stringResource(R.string.result_done),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
    }

    /** The total, with every hole listed under it in the order printed. */
    @Composable
    override fun Detail(
        result: KindaHardGolfResult,
        modifier: Modifier,
    ) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                result.strokes?.let {
                    Text(
                        pluralStringResource(R.plurals.golf_strokes, it, it),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                if (result.infuriating) {
                    Text(
                        stringResource(R.string.golf_infuriating),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            result.mapName?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (result.holes.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    result.holes.forEach {
                        Text(
                            text = it.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    pluralStringResource(
                        R.plurals.golf_holes,
                        result.holes.size,
                        result.holes.size,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    @Composable
    override fun Aggregate(
        results: List<KindaHardGolfResult>,
        modifier: Modifier,
    ) {
        val totals = results.mapNotNull { it.strokes }
        if (totals.isEmpty()) return
        Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.golf_best, totals.min()),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(R.string.golf_average, totals.average().toInt()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
