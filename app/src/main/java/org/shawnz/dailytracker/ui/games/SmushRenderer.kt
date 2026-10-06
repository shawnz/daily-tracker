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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.parse.SmushResult

object SmushRenderer : GameRenderer<SmushResult> {
    override fun summaryText(
        result: SmushResult,
        resources: Resources,
    ): String =
        listOfNotNull(
            result.points?.let { resources.getString(R.string.smush_points, it) }
                ?: resources.getString(R.string.result_done),
            when {
                !result.pangram -> null
                result.pangramFirst -> resources.getString(R.string.smush_pangram_first)
                else -> resources.getString(R.string.smush_pangram)
            },
        ).joinToString(" ")

    @Composable
    override fun Summary(
        result: SmushResult,
        modifier: Modifier,
    ) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text =
                    result.points?.let { stringResource(R.string.smush_points, it) }
                        ?: stringResource(R.string.result_done),
                style = MaterialTheme.typography.bodySmall,
            )
            if (result.pangram) {
                Text(
                    text =
                        stringResource(
                            if (result.pangramFirst) {
                                R.string.smush_pangram_first
                            } else {
                                R.string.smush_pangram
                            },
                        ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    /**
     * The chunks as written, with the score beside them.
     *
     * A pancake and a star are pictures, not colours, so the emoji are drawn as written.
     */
    @Composable
    override fun Detail(
        result: SmushResult,
        modifier: Modifier,
    ) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (result.grid.isNotEmpty()) EmojiText(result.grid.joinToString("\n"))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                result.points?.let {
                    Text(
                        stringResource(R.string.smush_points, it),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                result.badges.forEach {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    @Composable
    override fun Aggregate(
        results: List<SmushResult>,
        modifier: Modifier,
    ) {
        val scores = results.mapNotNull { it.points }
        if (scores.isEmpty()) return
        val pangrams = results.count { it.pangram }
        Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.smush_best, scores.max()),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(R.string.smush_pangram_rate, pangrams * 100 / results.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
