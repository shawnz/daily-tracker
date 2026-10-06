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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.parse.FourBySixResult

/** The game's own candy colours, one for each kind of card. */
private val YELLOW = Color(0xFFEEC453)
private val ORANGE = Color(0xFFEF9C4D)
private val GREEN = Color(0xFF9FD6B3)
private val BLUE = Color(0xFF9FB2EE)
private val PURPLE = Color(0xFFB8A4E6)
private val PINK = Color(0xFFEC9BB0)

object FourBySixRenderer : GameRenderer<FourBySixResult> {
    override fun summaryText(
        result: FourBySixResult,
        resources: Resources,
    ): String = listOfNotNull(moves(result, resources), result.medal).joinToString(" ")

    @Composable
    override fun Summary(
        result: FourBySixResult,
        modifier: Modifier,
    ) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = moves(result, LocalResources.current), style = MaterialTheme.typography.bodySmall)
            result.medal?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    /** The shelves, with the moves taken beside them. */
    @Composable
    override fun Detail(
        result: FourBySixResult,
        modifier: Modifier,
    ) {
        val empty = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (result.grid.isNotEmpty()) TileGrid(result.grid) { colourOf(it, empty) }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(moves(result, LocalResources.current), style = MaterialTheme.typography.titleMedium)
                    result.medal?.let {
                        Text(it, style = MaterialTheme.typography.titleMedium)
                    }
                }
                result.points?.let {
                    Text(
                        stringResource(R.string.foursix_points, it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                result.streakDays?.let {
                    Text(
                        stringResource(R.string.foursix_streak, it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    @Composable
    override fun Aggregate(
        results: List<FourBySixResult>,
        modifier: Modifier,
    ) {
        val scores = results.mapNotNull { it.points }
        if (scores.isEmpty()) return
        val perfect = results.count { it.perfect }
        Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.foursix_best, scores.max()),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(R.string.foursix_perfect, perfect),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    private fun colourOf(
        codePoint: Int,
        empty: Color,
    ): Color =
        when (codePoint) {
            0x1F7E8 -> YELLOW
            0x1F7E7 -> ORANGE
            0x1F7E9 -> GREEN
            0x1F7E6 -> BLUE
            0x1F7EA -> PURPLE
            0x1F7E5 -> PINK
            else -> empty
        }

    private fun moves(
        result: FourBySixResult,
        resources: Resources,
    ): String =
        if (result.moves == null || result.par == null) {
            resources.getString(R.string.result_done)
        } else {
            resources.getString(R.string.foursix_moves, result.moves, result.par)
        }
}
