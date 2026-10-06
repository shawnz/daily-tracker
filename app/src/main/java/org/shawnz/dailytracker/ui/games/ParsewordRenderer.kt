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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.parse.ParsewordResult

object ParsewordRenderer : GameRenderer<ParsewordResult> {
    override fun summaryText(
        result: ParsewordResult,
        resources: Resources,
    ): String =
        listOfNotNull(
            result.seconds?.let(::clock) ?: resources.getString(R.string.result_done),
            if (result.perfect) resources.getString(R.string.parseword_perfect) else null,
        ).joinToString(" ")

    @Composable
    override fun Summary(
        result: ParsewordResult,
        modifier: Modifier,
    ) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = result.seconds?.let(::clock) ?: stringResource(R.string.result_done),
                style = MaterialTheme.typography.bodySmall,
            )
            if (result.perfect) {
                Text(
                    stringResource(R.string.parseword_perfect),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    /** The time, with how much help was taken and which difficulty was played. */
    @Composable
    override fun Detail(
        result: ParsewordResult,
        modifier: Modifier,
    ) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                result.seconds?.let {
                    Text(clock(it), style = MaterialTheme.typography.titleMedium)
                }
                if (result.perfect) {
                    Text(
                        stringResource(R.string.parseword_perfect),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            result.assists?.let {
                Text(
                    pluralStringResource(R.plurals.parseword_assists, it, it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val mode =
                when {
                    result.learnMode -> stringResource(R.string.parseword_learn)
                    result.challengeMode -> stringResource(R.string.parseword_challenge)
                    else -> null
                }
            mode?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val found = result.secretsFound + result.easterEggs
            if (found > 0) {
                Text(
                    pluralStringResource(R.plurals.parseword_secrets, found, found),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    @Composable
    override fun Aggregate(
        results: List<ParsewordResult>,
        modifier: Modifier,
    ) {
        val times = results.mapNotNull { it.seconds }
        if (times.isEmpty()) return
        val perfect = results.count { it.perfect }
        Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.parseword_best, clock(times.min())),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(R.string.parseword_perfect_rate, perfect * 100 / results.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    private fun clock(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
}
