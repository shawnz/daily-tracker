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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.parse.ConnectionsResult

/** The game's own colours, one for each category a board can have. */
private val YELLOW = Color(0xFFF9DF6D)
private val GREEN = Color(0xFFA0C35A)
private val BLUE = Color(0xFFB0C4EF)
private val PURPLE = Color(0xFFBA81C5)
private val RED = Color(0xFFE5695B)
private val ORANGE = Color(0xFFF5A623)

object ConnectionsRenderer : GameRenderer<ConnectionsResult> {
    override fun summaryText(
        result: ConnectionsResult,
        resources: Resources,
    ): String =
        resources.getQuantityString(
            R.plurals.connections_mistakes,
            result.mistakes,
            result.mistakes,
        )

    @Composable
    override fun Summary(
        result: ConnectionsResult,
        modifier: Modifier,
    ) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = summaryText(result, LocalResources.current),
                style = MaterialTheme.typography.bodySmall,
                color =
                    if (result.success) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    },
            )
        }
    }

    /** The guesses, with the groups found beside them. */
    @Composable
    override fun Detail(
        result: ConnectionsResult,
        modifier: Modifier,
    ) {
        val absent = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (result.grid.isNotEmpty()) {
                TileGrid(result.grid) { colourOf(it, absent) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text =
                        pluralStringResource(
                            R.plurals.connections_groups,
                            result.groupsSolved,
                            result.groupsSolved,
                        ),
                    style = MaterialTheme.typography.titleMedium,
                    color =
                        if (result.success) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                )
                Text(
                    text =
                        pluralStringResource(
                            R.plurals.connections_mistakes,
                            result.mistakes,
                            result.mistakes,
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
        results: List<ConnectionsResult>,
        modifier: Modifier,
    ) {
        if (results.isEmpty()) return
        val solved = results.count { it.success }
        val perfect = results.count { it.mistakes == 0 }
        Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.connections_solve_rate, solved * 100 / results.size),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(R.string.connections_perfect, perfect),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    private fun colourOf(
        codePoint: Int,
        absent: Color,
    ): Color =
        when (codePoint) {
            0x1F7E8 -> YELLOW
            0x1F7E9 -> GREEN
            0x1F7E6 -> BLUE
            0x1F7EA -> PURPLE
            0x1F7E5 -> RED
            0x1F7E7 -> ORANGE
            else -> absent
        }
}
