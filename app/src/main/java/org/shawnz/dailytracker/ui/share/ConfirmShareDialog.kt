package org.shawnz.dailytracker.ui.share

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.data.Game
import org.shawnz.dailytracker.ui.DayPickerContent
import org.shawnz.dailytracker.ui.dayLabel
import org.shawnz.dailytracker.ui.games.RawShareText
import java.time.LocalDate

private enum class Pane { Form, Game, Day }

/** Selects the game and/or day for a shared result, starting from what the text states. */
@Composable
fun ConfirmShareDialog(
    sharedText: String,
    matchedGameId: Long?,
    onDone: () -> Unit,
) {
    val vm: ConfirmShareViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    LaunchedEffect(saved) { if (saved) onDone() }

    // Nothing is drawn until the games have loaded. [ReceiveShareActivity] only opens this when
    // there are games, so the list will never be empty.
    val available = state.games ?: return

    var pane by rememberSaveable {
        mutableStateOf(if (matchedGameId == null) Pane.Game else Pane.Form)
    }
    val game = state.game
    val day = state.day

    // The form and day panes are specific to a game and day, so draw nothing until both are
    // selected.
    if (pane != Pane.Game && (game == null || day == null)) return

    AlertDialog(
        onDismissRequest = onDone,
        title = {
            Text(
                stringResource(
                    when (pane) {
                        Pane.Form -> R.string.share_save_result
                        Pane.Game -> R.string.share_pick_game
                        Pane.Day -> R.string.detail_pick_day
                    },
                ),
            )
        },
        text = {
            when (pane) {
                Pane.Game -> {
                    GameListPane(
                        sharedText = sharedText,
                        games = available,
                        onPick = {
                            vm.pickGame(it)
                            pane = Pane.Form
                        },
                    )
                }

                Pane.Day -> {
                    if (game != null && day != null) {
                        DayPickerContent(
                            selected = day,
                            last = game.currentPuzzleDay(),
                            playedDays = state.playedDays,
                            onPick = {
                                vm.pickDay(it)
                                pane = Pane.Form
                            },
                        )
                    }
                }

                Pane.Form -> {
                    if (game != null && day != null) {
                        FormPane(
                            sharedText = sharedText,
                            game = game,
                            day = day,
                            currentDeviceDay = state.currentDeviceDay,
                            clash = state.clash,
                            onGame = { pane = Pane.Game },
                            onDay = { pane = Pane.Day },
                        )
                    }
                }
            }
        },
        confirmButton = {
            when (pane) {
                Pane.Form -> {
                    if (game != null && day != null) {
                        TextButton(onClick = { vm.save(game, day) }) {
                            Text(stringResource(R.string.share_confirm_save))
                        }
                    }
                }

                Pane.Day -> {
                    if (game != null && day != null) {
                        TextButton(
                            onClick = {
                                vm.pickDay(game.currentPuzzleDay())
                                pane = Pane.Form
                            },
                            enabled = day != game.currentPuzzleDay(),
                        ) { Text(stringResource(R.string.detail_latest)) }
                    }
                }

                Pane.Game -> {}
            }
        },
        dismissButton = {
            when (pane) {
                Pane.Form -> Unit

                Pane.Day -> BackButton(onClick = { pane = Pane.Form })

                // The form needs a game, so there is nothing to go back to until one is picked.
                Pane.Game -> if (game != null) BackButton(onClick = { pane = Pane.Form })
            }
        },
    )
}

@Composable
private fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(stringResource(R.string.action_back))
    }
}

/**
 * A preview of the shared text, with the game and day it will be saved under. Tapping the game
 * or day opens the pane for changing it.
 */
@Composable
private fun FormPane(
    sharedText: String,
    game: Game,
    day: LocalDate,
    currentDeviceDay: LocalDate,
    clash: Boolean,
    onGame: () -> Unit,
    onDay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SharePreview(sharedText)
        Field(
            label = stringResource(R.string.share_confirm_game),
            value = game.title,
            onClick = onGame,
        )
        Field(
            label = stringResource(R.string.share_confirm_day),
            value =
                if (day == currentDeviceDay) {
                    stringResource(R.string.detail_today)
                } else {
                    day.dayLabel()
                },
            onClick = onDay,
        )
        if (clash) {
            Text(
                stringResource(R.string.share_confirm_replaces),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

/**
 * The games a result can be saved to, with a preview of the shared text, since this pane is
 * shown first when no game was matched.
 */
@Composable
private fun GameListPane(
    sharedText: String,
    games: List<Game>,
    onPick: (Game) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SharePreview(sharedText)
        // Only the list scrolls, so the shared text stays in view. With `fill = false`, a short
        // list is only as tall as its items.
        Column(
            modifier =
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
        ) {
            games.forEach { game ->
                GameRow(game = game, onClick = { onPick(game) })
            }
        }
    }
}

@Composable
private fun GameRow(
    game: Game,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        game.title,
        style = MaterialTheme.typography.bodyLarge,
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 12.dp),
    )
}

/** A truncated preview of the shared text. */
@Composable
private fun SharePreview(
    text: String,
    modifier: Modifier = Modifier,
) {
    RawShareText(text.trim(), modifier = modifier.fillMaxWidth(), maxLines = 4)
}

/** A labelled value that opens a pane for changing it when tapped. */
@Composable
private fun Field(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
