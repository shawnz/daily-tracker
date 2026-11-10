package org.shawnz.dailytracker.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.data.EntryEntity
import org.shawnz.dailytracker.data.Game
import org.shawnz.dailytracker.ui.games.EntryAggregate

/** One game's completion, plays and streak, followed by its [EntryAggregate]. */
@Composable
internal fun GameStatLines(
    game: Game,
    completion: Float,
    plays: Int,
    streak: Int,
    entries: List<EntryEntity>,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        StatLine(
            stringResource(R.string.stats_completion),
            "${(completion * 100).toInt()}%",
        )
        StatLine(
            stringResource(R.string.stats_recorded),
            pluralStringResource(R.plurals.stats_plays, plays, plays),
        )
        StatLine(
            stringResource(R.string.stats_streak),
            pluralStringResource(R.plurals.stats_days, streak, streak),
        )
        EntryAggregate(game, entries, Modifier.padding(top = 2.dp))
    }
}

@Composable
internal fun StatLine(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
