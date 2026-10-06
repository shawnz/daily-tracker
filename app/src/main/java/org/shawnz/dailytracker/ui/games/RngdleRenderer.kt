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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.parse.RngdleResult

object RngdleRenderer : GameRenderer<RngdleResult> {
    override fun summaryText(
        result: RngdleResult,
        resources: Resources,
    ): String = listOfNotNull(result.rarity, result.standing).joinToString(" ")

    @Composable
    override fun Summary(
        result: RngdleResult,
        modifier: Modifier,
    ) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            result.rarity?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall)
            }
            result.standing?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    /** The roll, with the badges it earned listed under it. */
    @Composable
    override fun Detail(
        result: RngdleResult,
        modifier: Modifier,
    ) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            result.roll?.let {
                Text(text = it, style = MaterialTheme.typography.headlineSmall)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                result.rarity?.let {
                    Text(text = it, style = MaterialTheme.typography.titleSmall)
                }
                result.standing?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            result.points?.let {
                Text(
                    stringResource(R.string.rngdle_points, "%,d".format(it)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            result.badges.forEach {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (result.moreBadges > 0) {
                Text(
                    pluralStringResource(
                        R.plurals.rngdle_more_badges,
                        result.moreBadges,
                        result.moreBadges,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    @Composable
    override fun Aggregate(
        results: List<RngdleResult>,
        modifier: Modifier,
    ) {
        val points = results.mapNotNull { it.points }
        if (points.isEmpty()) return
        Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.rngdle_best, "%,d".format(points.max())),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                pluralStringResource(
                    R.plurals.rngdle_badges_earned,
                    results.sumOf { it.badgesEarned },
                    results.sumOf { it.badgesEarned },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
