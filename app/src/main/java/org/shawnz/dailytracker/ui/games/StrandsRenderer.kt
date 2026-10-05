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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.parse.StrandsResult

object StrandsRenderer : GameRenderer<StrandsResult> {
    @Composable
    override fun Summary(
        result: StrandsResult,
        modifier: Modifier,
    ) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val hints = result.hints
            Text(
                text =
                    if (hints == null) {
                        stringResource(R.string.strands_themed)
                    } else {
                        pluralStringResource(R.plurals.strands_hints, hints, hints)
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    /**
     * The answers as found, with the theme above them.
     *
     * The emoji are drawn as written, since a few puzzles have their own emoji.
     */
    @Composable
    override fun Detail(
        result: StrandsResult,
        modifier: Modifier,
    ) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            result.clue?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleSmall,
                    fontStyle = FontStyle.Italic,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (result.emoji.isNotEmpty()) EmojiText(result.emoji.joinToString("\n"))
            val hints = result.hints
            val themeWords = result.themeWords
            Text(
                text =
                    if (hints == null || themeWords == null) {
                        stringResource(R.string.strands_themed)
                    } else {
                        pluralStringResource(R.plurals.strands_found, hints, themeWords, hints)
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    @Composable
    override fun Aggregate(
        results: List<StrandsResult>,
        modifier: Modifier,
    ) {
        val hints = results.mapNotNull { it.hints }
        if (hints.isEmpty()) return
        val noHints = hints.count { it == 0 }
        Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.strands_no_hints, noHints * 100 / hints.size),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(
                    R.string.strands_average_hints,
                    "%.1f".format(hints.sum().toDouble() / hints.size),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
