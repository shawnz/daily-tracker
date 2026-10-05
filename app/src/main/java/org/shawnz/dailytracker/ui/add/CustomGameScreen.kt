package org.shawnz.dailytracker.ui.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.data.UrlMatch
import org.shawnz.dailytracker.data.catalog.CatalogGame
import org.shawnz.dailytracker.data.looksLikeUrl
import org.shawnz.dailytracker.data.matchUrl
import org.shawnz.dailytracker.ui.PageMargin
import org.shawnz.dailytracker.ui.hostLabel

/**
 * Adds a user-made game. Its link is matched against every game already added and the catalog,
 * so each game is added only once.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomGameScreen(
    onBack: () -> Unit,
    onAdded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: CustomGameViewModel = hiltViewModel()
    val games by vm.games.collectAsStateWithLifecycle()
    val added by vm.added.collectAsStateWithLifecycle()
    LaunchedEffect(added) { if (added) onAdded() }
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    val match = remember(url, games) { matchUrl(url, games.orEmpty()) }
    val urlUsable = remember(url) { looksLikeUrl(url) }

    // The link is validated once the field loses focus or the button is pressed, so an error doesn't
    // show before the user has finished typing the address.
    //
    // [onFocusChanged] runs when the focus changes and when the field first appears. [urlEntered]
    // distinguishes the two by recording whether the field has ever had focus.
    var urlEntered by remember { mutableStateOf(false) }
    var urlValidated by remember { mutableStateOf(false) }
    val urlRejected = urlValidated && url.isNotBlank() && !urlUsable
    val urlInvalid = stringResource(R.string.custom_game_url_invalid)

    val ready =
        when (match) {
            null -> title.isNotBlank() && url.isNotBlank()
            else -> true
        }

    val submit: () -> Unit = {
        if (match == null && !urlUsable) {
            urlValidated = true
        } else {
            when (match) {
                null -> vm.addCustom(title, url)

                is UrlMatch.Archived -> vm.restore(match.game)

                is UrlMatch.Known -> vm.addFromCatalog(match.catalogGame)

                // Already tracked, so the button only closes the screen.
                is UrlMatch.Tracked -> onAdded()
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.custom_game_title)) },
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
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(PageMargin),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // When the link matches a game, the field shows the title of the matched game and is
            // disabled. The typed title is kept, and shown again if the link stops matching.
            OutlinedTextField(
                value = match?.title ?: title,
                onValueChange = { title = it },
                enabled = match == null,
                label = { Text(stringResource(R.string.custom_game_title_field)) },
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = url,
                // Editing marks the link as not validated, so a correction is not marked
                // wrong on each keystroke.
                onValueChange = {
                    url = it
                    urlValidated = false
                },
                // The reason is given to screen readers by the semantic error and hidden from
                // them in the printed copy, so it is read once.
                isError = urlRejected,
                label = { Text(stringResource(R.string.custom_game_url_field)) },
                supportingText =
                    if (urlRejected) {
                        {
                            Text(
                                urlInvalid,
                                modifier = Modifier.semantics { hideFromAccessibility() },
                            )
                        }
                    } else {
                        null
                    },
                singleLine = true,
                // The last field, so Done presses the button. The button validates the link.
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done,
                    ),
                keyboardActions = KeyboardActions(onDone = { if (ready) submit() }),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics { if (urlRejected) error(urlInvalid) }
                        .onFocusChanged { state ->
                            if (state.isFocused) {
                                urlEntered = true
                            } else if (urlEntered) {
                                urlValidated = true
                            }
                        },
            )
            if (match != null) {
                // Only a hidden catalog game that isn't tracked yet is shown as a discovery.
                val hidden = (match as? UrlMatch.Known)?.catalogGame?.takeIf { it.hidden }
                if (hidden != null) {
                    HiddenGameNote(hidden)
                } else {
                    Text(
                        stringResource(
                            when (match) {
                                is UrlMatch.Tracked -> R.string.custom_game_match_tracked
                                is UrlMatch.Archived -> R.string.custom_game_match_archived
                                is UrlMatch.Known -> R.string.custom_game_match_known
                            },
                            match.title,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // The button stays enabled for an unusable link, so pressing it can validate the link
            // and show the error.
            Button(
                onClick = submit,
                enabled = ready,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    when (match) {
                        is UrlMatch.Archived -> {
                            stringResource(R.string.custom_game_restore, match.title)
                        }

                        is UrlMatch.Known -> {
                            stringResource(R.string.custom_game_add_known, match.title)
                        }

                        is UrlMatch.Tracked -> {
                            stringResource(R.string.custom_game_back_to_today)
                        }

                        else -> {
                            stringResource(R.string.custom_game_save)
                        }
                    },
                )
            }
        }
    }
}

/** Tells the user that [game], a hidden catalog game, has been found. */
@Composable
internal fun HiddenGameNote(
    game: CatalogGame,
    modifier: Modifier = Modifier,
) {
    val publisher = game.publisher ?: hostLabel(game.url)
    val message =
        if (publisher == null) {
            stringResource(R.string.custom_game_match_hidden, game.title)
        } else {
            stringResource(R.string.custom_game_match_hidden_by, game.title, publisher)
        }
    Text(
        // The sparkles are decoration. A screen reader is given the sentence on its own.
        text = "✨ $message ✨",
        modifier = modifier.semantics { contentDescription = message },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
    )
}
