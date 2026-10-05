package org.shawnz.dailytracker.ui.detail

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.browser.CustomTabLauncher
import org.shawnz.dailytracker.data.COMPLETION_WINDOW_DAYS
import org.shawnz.dailytracker.data.EntryEntity
import org.shawnz.dailytracker.data.Game
import org.shawnz.dailytracker.data.GameProgress
import org.shawnz.dailytracker.share.ReceiveShareActivity
import org.shawnz.dailytracker.ui.DayPickerDialog
import org.shawnz.dailytracker.ui.PageMargin
import org.shawnz.dailytracker.ui.dayLabel
import org.shawnz.dailytracker.ui.games.EntryAggregate
import org.shawnz.dailytracker.ui.games.EntryDetail
import org.shawnz.dailytracker.ui.hostLabel
import kotlin.math.roundToInt

/** The details of one game, a button to play it, and its result for a selected day. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GameDetailScreen(
    gameId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: GameDetailViewModel =
        hiltViewModel<GameDetailViewModel, GameDetailViewModel.Factory> { it.create(gameId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val closed by vm.closed.collectAsStateWithLifecycle()
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

    Scaffold(
        modifier = modifier,
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
                        IconButton(onClick = { vm.setReminders(!game.remindersEnabled) }) {
                            ReminderIcon(enabled = game.remindersEnabled)
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(
                                    painterResource(R.drawable.ic_more_vert),
                                    stringResource(R.string.action_more),
                                )
                            }
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
                                        vm.setArchived(true)
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

            item {
                Button(
                    onClick = { CustomTabLauncher.launch(context, game.url, toolbarColor) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(painterResource(R.drawable.ic_play_arrow), contentDescription = null)
                    Text(
                        stringResource(R.string.detail_play),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            item(key = "day") {
                DayCard(
                    modifier = Modifier.padding(top = 16.dp),
                    game = game,
                    state = state,
                    draft = draft,
                    editing = editing,
                    onDraftChange = { draft = it },
                    onStep = { vm.stepDay(it) },
                    onPickDay = { pickingDay = true },
                    onEdit = {
                        draft = state.selectedEntry?.rawShareText.orEmpty()
                        editing = true
                    },
                    onCancelEdit = { editing = false },
                    onSave = {
                        vm.record(draft)
                        editing = false
                    },
                    onShare = {
                        state.selectedEntry?.rawShareText?.let { shareResult(context, it) }
                    },
                    onClear = {
                        vm.clearSelected()
                        // The card is about to show the empty state for this day, so the
                        // field must not still contain the text that was just deleted.
                        draft = ""
                    },
                )
            }

            val progress = state.progress
            if (progress != null && progress.plays > 0) {
                item(key = "progress") {
                    // Aligned to the page margin, like the header and Play, so these figures
                    // start to the left of the grid inside the card above.
                    Progress(
                        progress = progress,
                        game = game,
                        entries = state.entries,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
        }
    }

    val pickSelected = state.selectedDay
    val pickLast = state.currentPuzzleDay
    if (pickingDay && pickSelected != null && pickLast != null) {
        DayPickerDialog(
            selected = pickSelected,
            last = pickLast,
            playedDays = state.playedDays,
            onPick = {
                pickingDay = false
                vm.selectDay(it)
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
                    vm.delete()
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
 * The selected day's entry, with controls to change it. The drawn result is shown, and the
 * saved text only while editing.
 */
@Composable
private fun DayCard(
    game: Game,
    state: DetailState,
    draft: String,
    editing: Boolean,
    onDraftChange: (String) -> Unit,
    onStep: (Long) -> Unit,
    onPickDay: () -> Unit,
    onEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val entry = state.selectedEntry
    Card(modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DayStepper(
                label =
                    when {
                        state.isToday -> stringResource(R.string.detail_today)
                        else -> state.selectedDay?.dayLabel().orEmpty()
                    },
                canGoForward = state.canGoForward,
                onStep = onStep,
                onPickDay = onPickDay,
            )

            HorizontalDivider()

            if (entry != null && !editing) {
                EntryDetail(game, entry)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onEdit) {
                        Text(stringResource(R.string.action_edit))
                    }
                    TextButton(
                        onClick = onShare,
                        enabled = !entry.rawShareText.isNullOrBlank(),
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_share),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            stringResource(R.string.action_share),
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = onClear,
                        colors =
                            ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                    ) { Text(stringResource(R.string.detail_clear)) }
                }
            } else {
                if (entry == null) {
                    Text(
                        stringResource(R.string.detail_nothing_recorded),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (game.parser == null) {
                    Text(
                        stringResource(R.string.detail_no_parser),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.detail_record_hint)) },
                    minLines = 3,
                    maxLines = 8,
                )

                if (editing) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onCancelEdit) {
                            Text(stringResource(R.string.action_cancel))
                        }
                        Button(
                            onClick = onSave,
                            modifier = Modifier.padding(start = 8.dp),
                        ) { Text(stringResource(R.string.detail_save)) }
                    }
                } else {
                    Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.detail_mark_done))
                    }
                }
            }
        }
    }
}

/** The game's streak, completion and plays, followed by its [EntryAggregate]. */
@Composable
private fun Progress(
    progress: GameProgress,
    game: Game,
    entries: List<EntryEntity>,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Figure(
                value = progress.streak.toString(),
                label = stringResource(R.string.detail_streak),
                modifier = Modifier.weight(1f),
            )
            Figure(
                value = "${(progress.completion * 100).roundToInt()}%",
                label = stringResource(R.string.detail_completion, COMPLETION_WINDOW_DAYS),
                modifier = Modifier.weight(1f),
            )
            Figure(
                value = progress.plays.toString(),
                label = stringResource(R.string.detail_plays),
                modifier = Modifier.weight(1f),
            )
        }
        EntryAggregate(game, entries)
    }
}

@Composable
private fun Figure(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Moves the card by a day at a time, or opens the picker for a longer jump. */
@Composable
private fun DayStepper(
    label: String,
    canGoForward: Boolean,
    onStep: (Long) -> Unit,
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
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                painterResource(R.drawable.ic_expand_more),
                stringResource(R.string.detail_pick_day),
                modifier = Modifier.padding(start = 4.dp),
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

/**
 * Sends a saved result to another app.
 *
 * Daily Tracker is itself an `ACTION_SEND` target, so it is excluded from the chooser.
 */
private fun shareResult(
    context: Context,
    text: String,
) {
    val send =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
    val chooser =
        Intent.createChooser(send, null).apply {
            putExtra(
                Intent.EXTRA_EXCLUDE_COMPONENTS,
                arrayOf(ComponentName(context, ReceiveShareActivity::class.java)),
            )
        }
    runCatching { context.startActivity(chooser) }
}

/** Shows whether reminders are enabled for this game. */
@Composable
private fun ReminderIcon(
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Icon(
        painterResource(
            if (enabled) R.drawable.ic_notifications else R.drawable.ic_notifications_off,
        ),
        stringResource(
            if (enabled) R.string.detail_reminders_on else R.string.detail_reminders_off,
        ),
        modifier = modifier,
    )
}
