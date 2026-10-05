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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.parse.RaddleResult

object RaddleRenderer : GameRenderer<RaddleResult> {
    @Composable
    override fun Summary(
        result: RaddleResult,
        modifier: Modifier,
    ) {
        Text(
            text =
                result.percentage?.let { stringResource(R.string.raddle_percent, it) }
                    ?: stringResource(R.string.result_done),
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

    /**
     * The ladder as written, with its first and last words above it.
     *
     * A light bulb and an eye are pictures, not colours, so the rungs are drawn as written.
     */
    @Composable
    override fun Detail(
        result: RaddleResult,
        modifier: Modifier,
    ) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (result.from != null && result.to != null) {
                Text(
                    text = "${result.from} → ${result.to}",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (result.emoji.isNotEmpty()) EmojiText(result.emoji)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                result.percentage?.let {
                    Text(
                        stringResource(R.string.raddle_percent, it),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Text(
                    pluralStringResource(
                        R.plurals.raddle_rungs,
                        result.rungs,
                        result.rungs,
                        result.hinted + result.revealed,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    @Composable
    override fun Aggregate(
        results: List<RaddleResult>,
        modifier: Modifier,
    ) {
        if (results.isEmpty()) return
        val full = results.count { it.percentage == 100 }
        val unaided = results.count { it.hinted == 0 && it.revealed == 0 }
        Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.raddle_full, full * 100 / results.size),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(R.string.raddle_unaided, unaided * 100 / results.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
