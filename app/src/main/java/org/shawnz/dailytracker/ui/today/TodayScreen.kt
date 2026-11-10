package org.shawnz.dailytracker.ui.today

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.browser.CustomTabLauncher
import org.shawnz.dailytracker.data.PuzzleStatus
import org.shawnz.dailytracker.ui.DayPagerSelection
import org.shawnz.dailytracker.ui.DayPages
import org.shawnz.dailytracker.ui.DayPickerDialog
import org.shawnz.dailytracker.ui.PageMargin
import org.shawnz.dailytracker.ui.TooltipIconButton
import org.shawnz.dailytracker.ui.animateScrollByPages
import org.shawnz.dailytracker.ui.dayAt
import org.shawnz.dailytracker.ui.dayLabel
import org.shawnz.dailytracker.ui.games.EntrySummary
import org.shawnz.dailytracker.ui.rememberCurrentDay
import org.shawnz.dailytracker.ui.shareText
import org.shawnz.dailytracker.ui.timeText
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.floor
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * The padding that is added to align the title and the last icon of the top bar with
 * [PageMargin]. In the Material top bar they are 16 dp from the edges.
 */
private val TopBarInset = PageMargin - 16.dp

/**
 * The tracked games' puzzles for one date, and the [PuzzleStatus] of each one. A swipe, or an
 * arrow in the top bar, moves to the previous or next date.
 *
 * Every gesture on a row opens the game's detail page on that date. A tap also opens the row's
 * [GameRowState.siteUrl] in a Custom Tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TodayScreen(
    onOpenGame: (gameId: Long, day: LocalDate) -> Unit,
    onAdd: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val frame = viewModel.frame.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    val resources = LocalResources.current
    var pickingDay by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Created once the dates are loaded, because the pager opens on the selected date.
    val pages = frame?.let { remember(it.earliestDay, it.latestDay) { DayPages(it.earliestDay, it.latestDay) } }
    val pagerState = if (frame != null && pages != null) rememberPagerState(pages.pageOf(frame.day)) { pages.count } else null
    if (frame != null && pages != null && pagerState != null) {
        DayPagerSelection(pagerState, pages, frame.day, viewModel::selectDay)
    }

    // The top bar is for the date of the current page.
    val shownDay = pagerState?.let { rememberCurrentDay(it).value } ?: frame?.day
    val shown = shownDay?.let { viewModel.stateOn(it).collectAsStateWithLifecycle().value }

    // Unplayed games, in list order, are the ones most likely to be opened next.
    LaunchedEffect(shown?.rows) {
        val rows = shown?.rows ?: return@LaunchedEffect
        CustomTabLauncher.prepare(rows.filter { it.status is PuzzleStatus.Unplayed }.mapNotNull { it.siteUrl })
    }

    if (pickingDay && frame != null && shownDay != null) {
        val playedDays by viewModel.playedDays.collectAsStateWithLifecycle()
        DayPickerDialog(
            selected = shownDay,
            latest = frame.today,
            last = frame.latestDay,
            playedDays = playedDays,
            onPick = {
                pickingDay = false
                viewModel.selectDay(it)
            },
            onDismiss = { pickingDay = false },
            first = frame.earliestDay,
            latestLabel = R.string.today_title,
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    if (shown != null && frame != null && pagerState != null) {
                        DayTitle(
                            state = shown,
                            canGoBack = shown.day > frame.earliestDay,
                            canGoForward = shown.day < frame.latestDay,
                            onStep = { scope.launch { pagerState.animateScrollByPages(it) } },
                            onPickDay = { pickingDay = true },
                        )
                    }
                },
                actions = {
                    TooltipIconButton(
                        icon = R.drawable.ic_share,
                        description = stringResource(R.string.action_share),
                        onClick = {
                            val state = shown ?: return@TooltipIconButton
                            shareText(
                                context,
                                todayShareText(state.rows, state.day, resources),
                            )
                        },
                        enabled = shown != null && shown.doneCount > 0,
                        position = TooltipAnchorPosition.Below,
                    )
                    TooltipIconButton(
                        icon = R.drawable.ic_leaderboard,
                        description = stringResource(R.string.action_stats),
                        onClick = onStats,
                        enabled = frame?.gamesTracked == true,
                        position = TooltipAnchorPosition.Below,
                    )
                    TooltipIconButton(
                        icon = R.drawable.ic_settings,
                        description = stringResource(R.string.action_settings),
                        onClick = onSettings,
                        modifier = Modifier.padding(end = TopBarInset),
                        position = TooltipAnchorPosition.Below,
                    )
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(painterResource(R.drawable.ic_add), stringResource(R.string.action_add))
            }
        },
    ) { padding ->
        val listTop = with(LocalDensity.current) { padding.calculateTopPadding().toPx() }
        val listStates = remember { mutableStateMapOf<LocalDate, LazyListState>() }
        var height by remember { mutableIntStateOf(0) }
        val coverage by rememberTileCoverage(pagerState, listStates, frame, listTop, height.toFloat())

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .onSizeChanged { height = it.height }
                    .tileBackdrop(
                        color = MaterialTheme.colorScheme.onSurface,
                        position = {
                            pagerState?.position() ?: (frame?.day ?: LocalDate.now()).toEpochDay().toDouble()
                        },
                        coverage = { coverage },
                    ),
        ) {
            when {
                // Nothing is drawn over the backdrop until the games have loaded, so the empty
                // state is only shown when the user doesn't track any games.
                frame == null || pages == null || pagerState == null -> {}

                !frame.gamesTracked -> {
                    EmptyState(modifier = Modifier.padding(padding))
                }

                else -> {
                    // The pages on either side are composed in advance, so that their results are
                    // read before they are scrolled into view.
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize().padding(padding),
                        beyondViewportPageCount = 1,
                        key = pages::key,
                    ) { page ->
                        val day = pages.dayOf(page)
                        val listState = rememberLazyListState()
                        DisposableEffect(day, listState) {
                            listStates[day] = listState
                            onDispose { listStates.remove(day) }
                        }
                        DayList(
                            state = viewModel.stateOn(day).collectAsStateWithLifecycle().value,
                            listState = listState,
                            onOpenGame = onOpenGame,
                        )
                    }
                }
            }
        }
    }
}

/** The rows for the date of [state]. Empty until [state] is loaded. */
@Composable
private fun DayList(
    state: TodayState?,
    listState: LazyListState,
    onOpenGame: (gameId: Long, day: LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val toolbarColor = MaterialTheme.colorScheme.surface.toArgb()
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp),
    ) {
        if (state != null) {
            items(state.rows, key = { it.game.id }) { row ->
                GameRow(
                    row = row,
                    onToday = state.day == state.today,
                    onTap = {
                        row.siteUrl?.let {
                            CustomTabLauncher.launch(context, it, toolbarColor)
                        }
                        onOpenGame(row.game.id, state.day)
                    },
                    onDetails = { onOpenGame(row.game.id, state.day) },
                )
            }
        }
    }
}

/**
 * The scroll position as a date: the days since the epoch of the current page, with the fraction
 * of a page scrolled. Null when the current page isn't laid out.
 */
private fun PagerState.position(): Double? = dayAt(currentPage)?.let { it.toEpochDay() + currentPageOffsetFraction.toDouble() }

/**
 * The animated [tileCoverage] of the backdrop. Between two pages, it is blended from the coverage
 * of each page by how far the pager is from one to the other.
 *
 * @param listStates The state of the list on each composed page, by date.
 * @param listTop The distance from the top of the backdrop to the top of the lists.
 * @param height The height of the backdrop.
 */
@Composable
private fun rememberTileCoverage(
    pagerState: PagerState?,
    listStates: Map<LocalDate, LazyListState>,
    frame: TodayFrame?,
    listTop: Float,
    height: Float,
): State<Float> {
    val coverage = remember { Animatable(0f) }
    val currentFrame by rememberUpdatedState(frame)
    val currentListTop by rememberUpdatedState(listTop)
    val currentHeight by rememberUpdatedState(height)
    LaunchedEffect(pagerState) {
        if (pagerState == null) return@LaunchedEffect

        // A page with no rows yet is skipped, because its list is not its final length.
        fun coverageOf(page: Int): Float? {
            val list = pagerState.dayAt(page)?.let { listStates[it] } ?: return null
            if (list.layoutInfo.totalItemsCount == 0) return null
            return tileCoverage(currentListTop + list.contentBottom(), currentHeight)
        }

        // The first value is shown without an animation.
        var animate = false
        snapshotFlow {
            val frame = currentFrame
            when {
                frame == null || currentHeight <= 0f -> {
                    null
                }

                !frame.gamesTracked -> {
                    0f
                }

                else -> {
                    val position = pagerState.currentPage + pagerState.currentPageOffsetFraction
                    val page = floor(position).toInt()
                    val start = coverageOf(page)
                    val end = coverageOf(page + 1)
                    if (start != null && end != null) lerp(start, end, position - page) else start ?: end
                }
            }
        }.filterNotNull()
            .collectLatest { target ->
                if (animate) {
                    coverage.animateTo(target)
                } else {
                    coverage.snapTo(target)
                    animate = true
                }
            }
    }
    return coverage.asState()
}

/**
 * The width of the widest label that [DayTitle] shows for a date in [year], or of [todayLabel] if
 * that is wider. A label has its year when [year] isn't the current year.
 */
@Composable
private fun dayLabelWidth(
    year: Int,
    todayLabel: String,
    style: TextStyle,
): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(year, todayLabel, style, density) {
        // Days 22 to 28 of each month include every pairing of a weekday name with a month name. Their
        // numbers have two digits, so they cover the upper bound of possible number widths in a
        // font whose digits all have the same width.
        val dates = (1..12).flatMap { month -> (22..28).map { LocalDate.of(year, month, it) } }
        val labels = dates.map { it.dayLabel() } + todayLabel
        with(density) { labels.maxOf { measurer.measure(it, style, maxLines = 1).size.width }.toDp() }
    }
}

/**
 * The date shown, with buttons for the previous and next dates and for picking a date, and the
 * number of games done on it.
 */
@Composable
private fun DayTitle(
    state: TodayState,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onStep: (Int) -> Unit,
    onPickDay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val todayLabel = stringResource(R.string.today_title)
    val style = MaterialTheme.typography.titleMedium
    val arrowGap = 8.dp
    val arrowSize = 24.dp
    val direction = LocalLayoutDirection.current
    val defaultPadding = ButtonDefaults.TextButtonContentPadding
    // The arrow's symbol has 6 dp of space on each side, so the start padding is 6 dp larger than the
    // end padding for the space at both ends of the button to look the same.
    val padding =
        PaddingValues(
            start = defaultPadding.calculateStartPadding(direction) + 6.dp,
            top = defaultPadding.calculateTopPadding(),
            end = defaultPadding.calculateEndPadding(direction),
            bottom = defaultPadding.calculateBottomPadding(),
        )
    // The width of the date button with its widest label, so that the next-date button stays in
    // place while the year is the same.
    val minWidth =
        padding.calculateStartPadding(direction) +
            dayLabelWidth(state.day.year, todayLabel, style) +
            arrowGap +
            arrowSize +
            padding.calculateEndPadding(direction)

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        // Moved so that its centre lines up with the status circles of the games.
        IconButton(
            onClick = { onStep(-1) },
            enabled = canGoBack,
            modifier = Modifier.offset(x = (-2).dp),
        ) {
            Icon(
                painterResource(R.drawable.ic_chevron_left),
                stringResource(R.string.detail_day_previous),
            )
        }
        // Weighted, so that on a narrow screen the date is shortened before the next-date button is.
        // Moved so that the date lines up with the names of the games.
        Box(Modifier.weight(1f, fill = false).offset(x = (-14).dp).widthIn(min = minWidth)) {
            TextButton(
                onClick = onPickDay,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                contentPadding = padding,
            ) {
                Column {
                    Text(
                        if (state.day == state.today) todayLabel else state.day.dayLabel(),
                        style = style,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (state.rows.isNotEmpty()) {
                        Text(
                            stringResource(R.string.today_done_count, state.doneCount, state.rows.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Icon(
                    painterResource(R.drawable.ic_expand_more),
                    stringResource(R.string.detail_pick_day),
                    modifier = Modifier.padding(start = arrowGap).size(arrowSize),
                )
            }
        }
        IconButton(onClick = { onStep(1) }, enabled = canGoForward) {
            Icon(
                painterResource(R.drawable.ic_chevron_right),
                stringResource(R.string.detail_day_next),
            )
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

/**
 * One game's row. A long press, or a tap on the trailing button, opens the detail page.
 *
 * @param onToday Whether the row is on the device's date. Only there is a puzzle that can be
 *   played on the next or previous date pointed out.
 */
@Composable
private fun GameRow(
    row: GameRowState,
    onToday: Boolean,
    onTap: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val state =
        stringResource(
            when (row.status) {
                is PuzzleStatus.Done -> R.string.result_done
                is PuzzleStatus.Unreleased -> R.string.today_not_out
                is PuzzleStatus.Unplayed, is PuzzleStatus.Missed -> R.string.today_not_played
            },
        )
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                // Done is drawn as a tick, unreleased as a clock, and unplayed as an empty
                // circle. The state description indicates which one to screen readers.
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
        StatusDot(row.status)
        Column(
            modifier = Modifier.weight(1f).padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(row.game.title, style = MaterialTheme.typography.titleMedium)
            when (val status = row.status) {
                is PuzzleStatus.Done -> {
                    EntrySummary(row.game, status.entry)
                }

                is PuzzleStatus.Unreleased -> {
                    Text(
                        text =
                            when {
                                onToday && status.currentPuzzleUnplayed -> {
                                    stringResource(R.string.today_yesterday_out)
                                }

                                status.nextRollover != null -> {
                                    val time = status.nextRollover.atZone(ZoneId.systemDefault()).toLocalTime()
                                    stringResource(R.string.today_next_puzzle_at, timeText(time))
                                }

                                else -> {
                                    stringResource(R.string.today_not_out)
                                }
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // When the rollover time is unknown, two days can be unplayed and only one of them
                // is current, so the text is the same as for a missed day.
                is PuzzleStatus.Unplayed -> {
                    Text(
                        text =
                            status.nextRollover?.let { timeLeft(it) }
                                ?: stringResource(R.string.today_not_played),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                is PuzzleStatus.Missed -> {
                    Text(
                        text =
                            stringResource(
                                if (onToday && status.currentPuzzleUnplayed) R.string.today_tomorrow_out else R.string.today_not_played,
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
    status: PuzzleStatus,
    modifier: Modifier = Modifier,
) {
    when (status) {
        is PuzzleStatus.Done -> {
            Icon(
                painterResource(R.drawable.ic_check_circle),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = modifier.size(28.dp),
            )
        }

        is PuzzleStatus.Unreleased -> {
            Icon(
                painterResource(R.drawable.ic_schedule),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = modifier.size(28.dp),
            )
        }

        is PuzzleStatus.Unplayed, is PuzzleStatus.Missed -> {
            Box(
                modifier =
                    modifier
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            )
        }
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
