package org.shawnz.dailytracker.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.ui.PageMargin
import org.shawnz.dailytracker.ui.games.GameModule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StatsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: StatsViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_back),
                            stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        val stats = state ?: return@Scaffold

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(PageMargin),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Scores don't compare across games, so the overall figures are only completion,
            // plays and streak.
            item {
                Text(
                    stringResource(R.string.stats_overall_header),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            item {
                Card {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatLine(
                            stringResource(R.string.stats_completion),
                            "${(stats.overallCompletion * 100).toInt()}%",
                        )
                        LinearProgressIndicator(
                            progress = { stats.overallCompletion },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        StatLine(
                            stringResource(R.string.stats_recorded),
                            stats.totalPlays.toString(),
                        )
                        StatLine(
                            stringResource(R.string.stats_streak),
                            pluralStringResource(
                                R.plurals.stats_days,
                                stats.bestStreak,
                                stats.bestStreak,
                            ),
                        )
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.stats_per_game_header),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(stats.perGame, key = { it.game.id }) { stat ->
                Card {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(stat.game.title, style = MaterialTheme.typography.titleMedium)
                        StatLine(
                            stringResource(R.string.stats_completion),
                            "${(stat.completion * 100).toInt()}%",
                        )
                        StatLine(
                            stringResource(R.string.stats_recorded),
                            pluralStringResource(
                                R.plurals.stats_plays,
                                stat.plays,
                                stat.plays,
                            ),
                        )
                        StatLine(
                            stringResource(R.string.stats_streak),
                            pluralStringResource(
                                R.plurals.stats_days,
                                stat.streak,
                                stat.streak,
                            ),
                        )
                        GameModule.forGame(stat.game)?.Aggregate(
                            entries = stat.entries,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatLine(
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
