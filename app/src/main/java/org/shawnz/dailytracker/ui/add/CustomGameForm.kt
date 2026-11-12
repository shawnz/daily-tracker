package org.shawnz.dailytracker.ui.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.data.UrlMatch
import org.shawnz.dailytracker.data.catalog.CatalogGame
import org.shawnz.dailytracker.data.looksLikeUrl
import org.shawnz.dailytracker.ui.hostLabel

/** The title and link typed for a user-made game. */
@Stable
internal class CustomGameFormState {
    var title by mutableStateOf("")
    var url by mutableStateOf("")

    // The link is validated once the field loses focus or the form is submitted, so an error
    // doesn't show before the user has finished typing the address.
    //
    // [onFocusChanged] runs when the focus changes and when the field first appears. [urlEntered]
    // distinguishes the two by recording whether the field has ever had focus.
    var urlEntered by mutableStateOf(false)
    var urlValidated by mutableStateOf(false)

    val urlUsable: Boolean get() = looksLikeUrl(url)
    val urlRejected: Boolean get() = urlValidated && url.isNotBlank() && !urlUsable

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
            urlValidated = true
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
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
        OutlinedTextField(
            value = state.url,
            // Editing marks the link as not validated, so a correction is not marked
            // wrong on each keystroke.
            onValueChange = {
                state.url = it
                state.urlValidated = false
            },
            // The reason is given to screen readers by the semantic error and hidden from
            // them in the printed copy, so it is read once.
            isError = state.urlRejected,
            label = { Text(stringResource(R.string.custom_game_url_field)) },
            supportingText =
                if (state.urlRejected) {
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
            // The last field, so Done submits the form. Submitting validates the link.
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions = KeyboardActions(onDone = { if (state.ready(match)) onDone() }),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics { if (state.urlRejected) error(urlInvalid) }
                    .onFocusChanged { focus ->
                        if (focus.isFocused) {
                            state.urlEntered = true
                        } else if (state.urlEntered) {
                            state.urlValidated = true
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
