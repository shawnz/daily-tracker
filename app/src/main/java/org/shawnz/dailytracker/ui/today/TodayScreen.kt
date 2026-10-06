package org.shawnz.dailytracker.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.browser.CustomTabLauncher
import org.shawnz.dailytracker.ui.PageMargin
import org.shawnz.dailytracker.ui.games.EntrySummary
import org.shawnz.dailytracker.ui.shareText
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * The padding that is added to align the title and the last icon of the top bar with
 * [PageMargin]. In the Material top bar they are 16 dp from the edges.
 */
private val TopBarInset = PageMargin - 16.dp

/**
 * The tracked games, and whether each one is done.
 *
 * Every gesture on a row opens the game's detail page. A tap on a game that isn't done also opens
 * the game.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TodayScreen(
    onOpenGame: (Long) -> Unit,
    onAdd: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: TodayViewModel = hiltViewModel()
    val today = vm.state.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    val toolbarColor = MaterialTheme.colorScheme.surface.toArgb()

    // Unplayed games, in list order, are the ones most likely to be opened next.
    LaunchedEffect(today?.rows) {
        val rows = today?.rows ?: return@LaunchedEffect
        CustomTabLauncher.prepare(rows.filterNot { it.done }.map { it.game.url })
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.padding(start = TopBarInset)) {
                        Text(stringResource(R.string.today_title))
                        if (today != null && today.rows.isNotEmpty()) {
                            Text(
                                stringResource(
                                    R.string.today_done_count,
                                    today.doneCount,
                                    today.rows.size,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val rows = today?.rows.orEmpty()
                            shareText(
                                context,
                                todayShareText(rows, LocalDate.now(), context.resources),
                            )
                        },
                        enabled = today != null && today.doneCount > 0,
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_share),
                            stringResource(R.string.action_share),
                        )
                    }
                    IconButton(onClick = onStats, enabled = !today?.rows.isNullOrEmpty()) {
                        Icon(
                            painterResource(R.drawable.ic_leaderboard),
                            stringResource(R.string.action_stats),
                        )
                    }
                    IconButton(
                        onClick = onSettings,
                        modifier = Modifier.padding(end = TopBarInset),
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_settings),
                            stringResource(R.string.action_settings),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(painterResource(R.drawable.ic_add), stringResource(R.string.action_add))
            }
        },
    ) { padding ->
        val rows = today?.rows
        val listState = rememberLazyListState()
        val listBottom by remember(listState) { derivedStateOf { listState.contentBottom() } }
        val listTop = padding.calculateTopPadding()

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .tileBackdrop(MaterialTheme.colorScheme.onSurface) {
                        if (rows.isNullOrEmpty()) 0f else listTop.toPx() + listBottom
                    },
        ) {
            when {
                // Nothing is drawn over the backdrop until the games have loaded, so the empty
                // state is only shown when the user doesn't track any games.
                rows == null -> {}

                rows.isEmpty() -> {
                    EmptyState(modifier = Modifier.padding(padding))
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(bottom = 88.dp),
                    ) {
                        items(rows, key = { it.game.id }) { row ->
                            GameRow(
                                row = row,
                                onTap = {
                                    if (!row.done) {
                                        CustomTabLauncher.launch(
                                            context,
                                            row.game.url,
                                            toolbarColor,
                                        )
                                    }
                                    onOpenGame(row.game.id)
                                },
                                onDetails = { onOpenGame(row.game.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The distance from the top of the list to the bottom of its last row, or infinity when the
 * list can scroll. In a list that can scroll, the distance depends on the scroll position.
 */
private fun LazyListState.contentBottom(): Float {
    val last = layoutInfo.visibleItemsInfo.lastOrNull()
    return if (last == null || canScrollBackward || canScrollForward) {
        Float.POSITIVE_INFINITY
    } else {
        (last.offset + last.size).toFloat()
    }
}

/** One game's row. A long press, or a tap on the trailing button, opens the detail page. */
@Composable
private fun GameRow(
    row: GameRowState,
    onTap: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val state =
        stringResource(
            if (row.done) R.string.result_done else R.string.today_not_played,
        )
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                // Done is drawn as a tick, and not done as an empty circle. The state
                // description indicates which one to screen readers.
                .semantics { stateDescription = state }
                .combinedClickable(
                    onClick = onTap,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDetails()
                    },
                )
                // Aligns the trailing icon with the page margin.
                .padding(start = PageMargin, top = 12.dp, bottom = 12.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(done = row.done)
        Column(
            modifier = Modifier.weight(1f).padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(row.game.title, style = MaterialTheme.typography.titleMedium)
            val entry = row.entry
            val deadline = row.game.nextRollover()
            if (entry != null) {
                EntrySummary(row.game, entry)
            } else {
                Text(
                    text =
                        if (deadline == null) {
                            stringResource(R.string.today_not_played)
                        } else {
                            timeLeft(deadline)
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onDetails) {
            Icon(
                painterResource(R.drawable.ic_chevron_right),
                stringResource(R.string.action_details),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * How long is left until [deadline], in hours and minutes.
 *
 * Each delay ends when the minute on screen changes, so a row redraws once a minute
 * whatever its deadline is.
 */
@Composable
private fun timeLeft(deadline: Instant): String {
    val minutes by produceState(minutesUntil(deadline), deadline) {
        while (true) {
            val left = Duration.between(Instant.now(), deadline)
            value = left.toMinutes()
            delay((left.toMillis().mod(60_000L) + 1).milliseconds)
        }
    }
    return if (minutes < 1) {
        stringResource(R.string.today_time_left_soon)
    } else {
        stringResource(R.string.today_time_left, minutes.minutes.toString())
    }
}

private fun minutesUntil(deadline: Instant): Long = Duration.between(Instant.now(), deadline).toMinutes()

@Composable
private fun StatusDot(
    done: Boolean,
    modifier: Modifier = Modifier,
) {
    if (done) {
        Icon(
            painterResource(R.drawable.ic_check_circle),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = modifier.size(28.dp),
        )
    } else {
        Box(
            modifier =
                modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
        )
    }
}

/** Shown in place of the list when the user doesn't track any games. */
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.today_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.today_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
