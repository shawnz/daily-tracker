package org.shawnz.dailytracker.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.browser.CustomTabLauncher
import org.shawnz.dailytracker.data.Game
import org.shawnz.dailytracker.data.PuzzleStatus
import org.shawnz.dailytracker.ui.DayPagerSelection
import org.shawnz.dailytracker.ui.DayPages
import org.shawnz.dailytracker.ui.DayPickerDialog
import org.shawnz.dailytracker.ui.PageMargin
import org.shawnz.dailytracker.ui.TooltipIconButton
import org.shawnz.dailytracker.ui.animateScrollByPages
import org.shawnz.dailytracker.ui.dayLabel
import org.shawnz.dailytracker.ui.games.EntryDetail
import org.shawnz.dailytracker.ui.hostLabel
import org.shawnz.dailytracker.ui.rememberCurrentDay
import org.shawnz.dailytracker.ui.shareText
import org.shawnz.dailytracker.ui.stats.GameStatLines
import org.shawnz.dailytracker.ui.timeText
import java.time.LocalDate
import java.time.ZoneId

/** The details of one game, a button to play it, and its result for a selected day. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GameDetailScreen(
    gameId: Long,
    day: LocalDate,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameDetailViewModel =
        hiltViewModel<GameDetailViewModel, GameDetailViewModel.Factory> { it.create(gameId, day) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val closed by viewModel.closed.collectAsStateWithLifecycle()
    LaunchedEffect(closed) { if (closed) onBack() }
    val context = LocalContext.current
    val toolbarColor = MaterialTheme.colorScheme.surface.toArgb()
    val game = state.game

    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var pickingDay by remember { mutableStateOf(false) }

    // Editing applies to the selected day, so it ends when another day is selected.
    var editing by remember(state.selectedDay) { mutableStateOf(false) }

    // Keyed on the selected day, so text typed for one day is not offered for another. Set
    // from the saved text when editing starts, because the share target can save a result
    // while this screen is open.
    var draft by rememberSaveable(state.selectedDay) { mutableStateOf("") }

    val listState = rememberLazyListState()
    val showTitle by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val undoable by viewModel.undoable.collectAsStateWithLifecycle()
    val deletedMessage = stringResource(R.string.detail_result_deleted)
    val changedMessage = stringResource(R.string.detail_result_changed)
    val undoLabel = stringResource(R.string.action_undo)
    LaunchedEffect(undoable) {
        val change = undoable ?: return@LaunchedEffect
        val result =
            snackbarHostState.showSnackbar(
                message = if (change.deleted) deletedMessage else changedMessage,
                actionLabel = undoLabel,
            )
        if (result == SnackbarResult.ActionPerformed) viewModel.undo(change) else viewModel.dismissUndo(change)
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    AnimatedVisibility(
                        visible = showTitle,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Text(
                            game?.title.orEmpty(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_back),
                            stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (game != null) {
                        TooltipIconButton(
                            icon = R.drawable.ic_open_in_new,
                            description = stringResource(R.string.detail_open_site),
                            onClick = { CustomTabLauncher.launch(context, game.url, toolbarColor) },
                            position = TooltipAnchorPosition.Below,
                        )
                        TooltipIconButton(
                            icon = if (game.remindersEnabled) R.drawable.ic_notifications else R.drawable.ic_notifications_off,
                            description =
                                stringResource(
                                    if (game.remindersEnabled) R.string.detail_reminders_mute else R.string.detail_reminders_unmute,
                                ),
                            onClick = { viewModel.setReminders(!game.remindersEnabled) },
                            position = TooltipAnchorPosition.Below,
                        )
                        Box {
                            TooltipIconButton(
                                icon = R.drawable.ic_more_vert,
                                description = stringResource(R.string.action_more),
                                onClick = { menuOpen = true },
                                position = TooltipAnchorPosition.Below,
                            )
                            DropdownMenu(
                                expanded = menuOpen,
                                onDismissRequest = { menuOpen = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.detail_archive)) },
                                    onClick = {
                                        menuOpen = false
                                        // An archived game is off the Today list, so this
                                        // page has nothing left to show. Unarchiving is on
                                        // the Add screen.
                                        viewModel.setArchived(true)
                                    },
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(R.string.detail_delete_action),
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    onClick = {
                                        menuOpen = false
                                        confirmDelete = true
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (game == null) return@Scaffold

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(PageMargin),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { GameHeader(game = game) }

            item(key = "day") {
                DayCard(
                    modifier = Modifier.padding(top = 16.dp),
                    game = game,
                    state = state,
                    draft = draft,
                    editing = editing,
                    onDraftChange = { draft = it },
                    onSelectDay = viewModel::selectDay,
                    onPickDay = { pickingDay = true },
                    onOpen = { CustomTabLauncher.launch(context, it, toolbarColor) },
                    onAdd = {
                        draft = ""
                        editing = true
                    },
                    onEdit = { card ->
                        draft = card.entry?.rawShareText.orEmpty()
                        editing = true
                    },
                    onCancelEdit = { editing = false },
                    onSave = {
                        viewModel.record(draft)
                        editing = false
                    },
                    onShare = { card ->
                        card.entry?.rawShareText?.let { shareText(context, it) }
                    },
                    onDelete = {
                        viewModel.deleteSelected()
                        editing = false
                    },
                )
            }

            // The stats don't depend on the selected day, so they are outside the day card.
            val progress = state.progress
            if (progress != null && progress.plays > 0) {
                item(key = "stats-header") {
                    Text(
                        stringResource(R.string.stats_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
                item(key = "stats") {
                    Card {
                        GameStatLines(
                            game = game,
                            completion = progress.completion,
                            plays = progress.plays,
                            streak = progress.streak,
                            entries = state.entries,
                            modifier = Modifier.padding(16.dp + ButtonOverhang),
                        )
                    }
                }
            }
        }
    }

    val pickSelected = state.selectedDay
    val pickPossible = state.possiblePuzzleDays
    if (pickingDay && pickSelected != null && pickPossible != null) {
        DayPickerDialog(
            selected = pickSelected,
            latest = pickPossible.start,
            last = pickPossible.endInclusive,
            playedDays = state.playedDays,
            onPick = {
                pickingDay = false
                viewModel.selectDay(it)
            },
            onDismiss = { pickingDay = false },
        )
    }

    if (confirmDelete && game != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.detail_delete)) },
            text = { Text(stringResource(R.string.detail_delete_confirm, game.title)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete()
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/** The game's title, publisher and description, drawn below the toolbar as one block. */
@Composable
private fun GameHeader(
    game: Game,
    modifier: Modifier = Modifier,
) {
    val publisher = game.catalogGame?.publisher ?: hostLabel(game.url)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val logo = game.catalogGame?.logo
            if (logo != null) {
                Image(
                    painter = painterResource(logo),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(game.title, style = MaterialTheme.typography.headlineSmall)
                if (publisher != null) {
                    Text(
                        publisher,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        val description = game.catalogGame?.description
        if (description != null) {
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * A stepper for the day, above that day's entry and the controls to change it. A swipe or a step
 * slides the entry to another day, and the stepper stays in place.
 */
@Composable
private fun DayCard(
    game: Game,
    state: DetailState,
    draft: String,
    editing: Boolean,
    onDraftChange: (String) -> Unit,
    onSelectDay: (LocalDate) -> Unit,
    onPickDay: () -> Unit,
    onOpen: (url: String) -> Unit,
    onAdd: () -> Unit,
    onEdit: (DayCard) -> Unit,
    onCancelEdit: () -> Unit,
    onSave: () -> Unit,
    onShare: (DayCard) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = state.selectedDay ?: return
    val last = state.possiblePuzzleDays?.endInclusive ?: return
    // A game may have been played for years before it was added here, so the pages go back a
    // century.
    val pages = remember(last) { DayPages(last.minusYears(100), last) }
    val pagerState = rememberPagerState(pages.pageOf(selected)) { pages.count }
    DayPagerSelection(pagerState, pages, selected, onSelectDay)
    val scope = rememberCoroutineScope()

    // The stepper is for the date of the current page.
    val shownDay by rememberCurrentDay(pagerState)
    val day = shownDay ?: selected

    Card(modifier) {
        DayStepper(
            label = if (day == state.currentDeviceDay) stringResource(R.string.detail_today) else day.dayLabel(),
            canGoForward = day < last,
            onStep = { scope.launch { pagerState.animateScrollByPages(it) } },
            onPickDay = onPickDay,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
        )
        // Each page has its own padding, so that its content slides up to the edges of the card.
        // A drag in the text field doesn't move to another day, because swiping is off while
        // editing. The height is animated inside the card, so that the surface of the card has the
        // animated height.
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.animateContentSize(),
            verticalAlignment = Alignment.Top,
            userScrollEnabled = !editing,
            key = pages::key,
        ) { page ->
            val pageDay = pages.dayOf(page)
            val card = remember(state, pageDay) { state.dayCard(pageDay) } ?: return@HorizontalPager
            DayContent(
                card = card,
                draft = draft,
                editing = editing && pageDay == selected,
                onDraftChange = onDraftChange,
                onOpen = onOpen,
                onAdd = onAdd,
                onEdit = { onEdit(card) },
                onCancelEdit = onCancelEdit,
                onSave = onSave,
                onShare = { onShare(card) },
                onDelete = onDelete,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp),
            )
        }
    }
}

/** The entry for the day of [card], with controls to change it. The saved text is shown only while editing. */
@Composable
private fun DayContent(
    card: DayCard,
    draft: String,
    editing: Boolean,
    onDraftChange: (String) -> Unit,
    onOpen: (url: String) -> Unit,
    onAdd: () -> Unit,
    onEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val game = card.game
    val entry = card.entry
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val status = card.status
        when {
            status is PuzzleStatus.Unreleased -> {
                Text(
                    status.nextRollover?.let {
                        val time = it.atZone(ZoneId.systemDefault()).toLocalTime()
                        stringResource(R.string.today_next_puzzle_at, timeText(time))
                    } ?: stringResource(R.string.today_not_out),
                    modifier = Modifier.padding(horizontal = ButtonOverhang),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            editing -> {
                EntryEditor(
                    game = game,
                    draft = draft,
                    hasEntry = entry != null,
                    onDraftChange = onDraftChange,
                    onCancel = onCancelEdit,
                    onSave = onSave,
                    onDelete = onDelete,
                )
            }

            entry != null -> {
                EntryDetail(game, entry, Modifier.padding(horizontal = ButtonOverhang))
                EntryActions(
                    puzzleUrl = card.puzzleUrl,
                    canShare = !entry.rawShareText.isNullOrBlank(),
                    onOpen = onOpen,
                    onShare = onShare,
                    onEdit = onEdit,
                )
            }

            else -> {
                NoEntry(
                    playUrl = card.playUrl,
                    onOpen = onOpen,
                    onAdd = onAdd,
                )
            }
        }
    }
}

/**
 * How far the touch target of a text button or icon button extends past its label or icon. Other
 * content in the cards is indented by this much, so that it lines up with the labels and icons.
 */
private val ButtonOverhang = 12.dp

/**
 * The actions for a recorded day. They are on a row of their own under the result, because a
 * result can be as wide as the card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryActions(
    puzzleUrl: String?,
    canShare: Boolean,
    onOpen: (url: String) -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // A text button, because the top bar's button for the game's site has the same icon.
        if (puzzleUrl != null) {
            TextButton(onClick = { onOpen(puzzleUrl) }) {
                Icon(
                    painterResource(R.drawable.ic_open_in_new),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    stringResource(R.string.detail_open_puzzle),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        TooltipIconButton(
            icon = R.drawable.ic_share,
            description = stringResource(R.string.action_share),
            onClick = onShare,
            enabled = canShare,
        )
        TooltipIconButton(
            icon = R.drawable.ic_edit,
            description = stringResource(R.string.action_edit),
            onClick = onEdit,
        )
    }
}

/**
 * A day with no result. Play is shown when that day's puzzle can be played.
 *
 * The text is the same on every such day. Play shows whether the puzzle can still be played, and
 * when the rollover time is unknown, the current puzzle can be on either of two days.
 */
@Composable
private fun NoEntry(
    playUrl: String?,
    onOpen: (url: String) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The day stepper above has about 12dp of touch target under its arrows and date, and the
    // buttons have 4dp. The padding above is 8dp less, so the visible space is the same.
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.today_not_played),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (playUrl != null) {
                Button(onClick = { onOpen(playUrl) }) {
                    Icon(
                        painterResource(R.drawable.ic_play_arrow),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        stringResource(R.string.detail_play),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
            TextButton(onClick = onAdd) {
                Icon(
                    painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    stringResource(R.string.detail_add_result),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

/** The field for a day's share text. Saving with the field empty records a play with no result. */
@Composable
private fun EntryEditor(
    game: Game,
    draft: String,
    hasEntry: Boolean,
    onDraftChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (game.parser == null) {
            Text(
                stringResource(R.string.detail_no_parser),
                modifier = Modifier.padding(horizontal = ButtonOverhang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = ButtonOverhang),
            label = { Text(stringResource(R.string.detail_record_hint)) },
            minLines = 3,
            maxLines = 8,
        )

        // Save is drawn to its left and right edges, so the end of the row is inset to line it up.
        Row(
            modifier = Modifier.fillMaxWidth().padding(end = ButtonOverhang),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (hasEntry) {
                TextButton(
                    onClick = onDelete,
                    colors =
                        ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                ) { Text(stringResource(R.string.detail_delete_result)) }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.action_cancel))
            }
            Button(
                onClick = onSave,
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Text(stringResource(if (hasEntry) R.string.detail_save else R.string.detail_mark_done))
            }
        }
    }
}

/** Moves the card by a day at a time, or opens the picker for a longer jump. */
@Composable
private fun DayStepper(
    label: String,
    canGoForward: Boolean,
    onStep: (Int) -> Unit,
    onPickDay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onStep(-1) }) {
            Icon(
                painterResource(R.drawable.ic_chevron_left),
                stringResource(R.string.detail_day_previous),
            )
        }
        TextButton(
            onClick = onPickDay,
            modifier = Modifier.weight(1f),
            colors =
                ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
        ) {
            // As wide as the icon and its padding after the date, so that the date alone is
            // centered.
            Spacer(Modifier.width(4.dp + 24.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                painterResource(R.drawable.ic_expand_more),
                stringResource(R.string.detail_pick_day),
                modifier = Modifier.padding(start = 4.dp).size(24.dp),
            )
        }
        IconButton(onClick = { onStep(1) }, enabled = canGoForward) {
            Icon(
                painterResource(R.drawable.ic_chevron_right),
                stringResource(R.string.detail_day_next),
            )
        }
    }
}
