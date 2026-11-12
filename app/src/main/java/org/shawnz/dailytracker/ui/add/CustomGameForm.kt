package org.shawnz.dailytracker.ui.add

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.data.UrlMatch
import org.shawnz.dailytracker.data.catalog.CatalogGame
import org.shawnz.dailytracker.data.catalogGameTitled
import org.shawnz.dailytracker.data.looksLikeUrl
import org.shawnz.dailytracker.ui.hostLabel

/** The title and link typed for a user-made game. */
@Stable
internal class CustomGameFormState {
    var title by mutableStateOf("")

    /** The text and cursor of the link field. */
    val urlField = TextFieldState()

    val url: String get() = urlField.text.toString()

    // The link is validated once the field loses focus or the form is submitted, so an error
    // doesn't show before the user has finished typing the address.
    //
    // [onFocusChanged] is called when the focus changes and when the field first appears.
    // [urlEntered] is true once the field has had focus, so the first call can be told apart from
    // a loss of focus.
    var urlEntered by mutableStateOf(false)

    /**
     * The link as it was when last validated, or null before that. The error is shown only while
     * the link still has this text, so a correction is not marked wrong on each keystroke.
     */
    private var validatedUrl by mutableStateOf<String?>(null)

    val urlUsable: Boolean get() = looksLikeUrl(url)
    val urlRejected: Boolean get() = validatedUrl == url && url.isNotBlank() && !urlUsable

    fun markUrlValidated() {
        validatedUrl = url
    }

    /**
     * Whether the form is complete. [match] is the game the link matches, or null when there is
     * none.
     */
    fun ready(match: UrlMatch?): Boolean = match != null || (title.isNotBlank() && url.isNotBlank())

    /**
     * Returns true when [match] is not null or the link is usable. Otherwise the link is marked as
     * validated, so its error is shown.
     */
    fun validate(match: UrlMatch?): Boolean {
        if (match == null && !urlUsable) {
            markUrlValidated()
            return false
        }
        return true
    }
}

@Composable
internal fun rememberCustomGameFormState(): CustomGameFormState = remember { CustomGameFormState() }

/**
 * The title and link fields of a user-made game. When the link matches a game, given as [match],
 * a note about that game is shown.
 *
 * While the link doesn't match a game, a catalog game with the typed title is suggested. When the
 * suggestion is selected, the link of that game replaces the typed link.
 *
 * @param onDone Called by the keyboard's Done action when the form is ready.
 */
@Composable
internal fun CustomGameFields(
    state: CustomGameFormState,
    match: UrlMatch?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val urlInvalid = stringResource(R.string.custom_game_url_invalid)
    val suggestion =
        remember(state.title, match) {
            if (match == null) catalogGameTitled(state.title) else null
        }
    val linkNote =
        when {
            match != null -> LinkNote.Matched(match)
            state.urlRejected -> LinkNote.Invalid
            else -> LinkNote.None
        }
    // The notes are drawn under the fields, outside their supporting text slot. Room for the full
    // height of supporting text is set aside before it is measured, so the field would be shrunk
    // while a note in that slot grows.
    val notePadding = Modifier.padding(NotePadding)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            // When the link matches a game, the field shows the title of the matched game and is
            // disabled. The typed title is kept, and shown again if the link stops matching.
            OutlinedTextField(
                value = match?.title ?: state.title,
                onValueChange = { state.title = it },
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
            AnimatedContent(
                targetState = suggestion,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
            ) { game ->
                if (game != null) {
                    TitleSuggestion(
                        game = game,
                        onSelect = { state.urlField.setTextAndPlaceCursorAtEnd(game.url) },
                        modifier = notePadding,
                    )
                }
            }
        }
        Column {
            OutlinedTextField(
                state = state.urlField,
                isError = state.urlRejected,
                label = { Text(stringResource(R.string.custom_game_url_field)) },
                lineLimits = TextFieldLineLimits.SingleLine,
                // The last field, so Done submits the form. Submitting validates the link.
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done,
                    ),
                onKeyboardAction = { if (state.ready(match)) onDone() },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics { if (state.urlRejected) error(urlInvalid) }
                        .onFocusChanged { focus ->
                            if (focus.isFocused) {
                                state.urlEntered = true
                            } else if (state.urlEntered) {
                                state.markUrlValidated()
                            }
                        },
            )
            AnimatedContent(
                targetState = linkNote,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
            ) { note ->
                when (note) {
                    is LinkNote.Matched -> {
                        MatchNote(note.match, modifier = notePadding)
                    }

                    // The error is given to screen readers by the semantic error on the field and
                    // hidden from them here, so it is read once.
                    LinkNote.Invalid -> {
                        Text(
                            urlInvalid,
                            modifier = notePadding.semantics { hideFromAccessibility() },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    LinkNote.None -> {}
                }
            }
        }
    }
}

/** The padding of a text field's supporting text in Material 3. */
private val NotePadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp)

/** Which note is shown under the link field. */
private sealed interface LinkNote {
    data class Matched(
        val match: UrlMatch,
    ) : LinkNote

    data object Invalid : LinkNote

    data object None : LinkNote
}

/** A note about [match], the game the link matches. */
@Composable
private fun MatchNote(
    match: UrlMatch,
    modifier: Modifier = Modifier,
) {
    // Only a hidden catalog game that isn't tracked yet is shown as a discovery.
    val hidden = (match as? UrlMatch.Known)?.catalogGame?.takeIf { it.hidden }
    if (hidden != null) {
        HiddenGameNote(hidden, modifier)
    } else {
        Text(
            when (match) {
                is UrlMatch.Tracked -> {
                    stringResource(R.string.custom_game_match_tracked, match.title)
                }

                is UrlMatch.Archived -> {
                    stringResource(R.string.custom_game_match_archived, match.title)
                }

                is UrlMatch.Known -> {
                    val publisher = match.catalogGame.publisherLabel()
                    if (publisher == null) {
                        stringResource(R.string.custom_game_match_known, match.title)
                    } else {
                        stringResource(R.string.custom_game_match_known_by, match.title, publisher)
                    }
                }
            },
            modifier = modifier,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A suggestion of [game] for the typed title. The title of [game] is a link, and [onSelect] is
 * called when it is selected.
 */
@Composable
private fun TitleSuggestion(
    game: CatalogGame,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val publisher = game.publisherLabel()
    val message =
        if (publisher == null) {
            stringResource(R.string.custom_game_suggest, game.title)
        } else {
            stringResource(R.string.custom_game_suggest_by, game.title, publisher)
        }
    val linkStyles =
        TextLinkStyles(
            SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
            ),
        )
    val text =
        buildAnnotatedString {
            append(message)
            val start = message.indexOf(game.title)
            addLink(
                LinkAnnotation.Clickable(tag = game.name, styles = linkStyles) { onSelect() },
                start,
                start + game.title.length,
            )
        }
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Returns the publisher of [this], or the host of its URL when the catalog has no publisher for
 * it.
 */
private fun CatalogGame.publisherLabel(): String? = publisher ?: hostLabel(url)

/** A note that [game], a hidden catalog game, has been found. */
@Composable
internal fun HiddenGameNote(
    game: CatalogGame,
    modifier: Modifier = Modifier,
) {
    val publisher = game.publisherLabel()
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
