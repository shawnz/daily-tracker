package org.shawnz.dailytracker.ui.games

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.data.EntryEntity
import org.shawnz.dailytracker.parse.GameResult

/** Draws the results of one game. */
interface GameRenderer<in R : GameResult> {
    /** One line, for dense lists. */
    @Composable
    fun Summary(
        result: R,
        modifier: Modifier = Modifier,
    )

    /** The full form, for the day card on the detail page. */
    @Composable
    fun Detail(
        result: R,
        modifier: Modifier = Modifier,
    )

    /** Drawn on the stats screen and the detail page. Draws nothing unless overridden. */
    @Suppress("EmptyMethod")
    @Composable
    fun Aggregate(
        results: List<R>,
        modifier: Modifier = Modifier,
    ) {
    }
}

/** Draws the entries of a game with no parser, and entries whose text doesn't parse. */
object DefaultEntryRenderer {
    @Composable
    fun Summary(modifier: Modifier = Modifier) {
        Text(
            stringResource(R.string.result_done),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
    }

    @Composable
    fun Detail(
        entry: EntryEntity,
        modifier: Modifier = Modifier,
    ) {
        val raw = entry.rawShareText?.trim()
        if (raw.isNullOrEmpty()) {
            Summary(modifier)
        } else {
            RawShareText(raw, modifier)
        }
    }
}

/**
 * Shows share text as the user saved it.
 *
 * The font is monospace and the line height is small, so that grids of emoji stay in
 * columns. [maxLines] trims the text to a preview.
 */
@Composable
fun RawShareText(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style =
                MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp,
                ),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(10.dp),
        )
    }
}
