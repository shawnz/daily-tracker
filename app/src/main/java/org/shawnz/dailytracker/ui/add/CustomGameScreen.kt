package org.shawnz.dailytracker.ui.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.data.UrlMatch
import org.shawnz.dailytracker.data.matchUrl
import org.shawnz.dailytracker.ui.PageMargin

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
    viewModel: CustomGameViewModel = hiltViewModel(),
) {
    val games by viewModel.games.collectAsStateWithLifecycle()
    val added by viewModel.added.collectAsStateWithLifecycle()
    LaunchedEffect(added) { if (added) onAdded() }
    val form = rememberCustomGameFormState()
    val match = remember(form.url, games) { matchUrl(form.url, games.orEmpty()) }

    val submit: () -> Unit = {
        if (form.validate(match)) {
            when (match) {
                null -> viewModel.addCustom(form.title, form.url)

                is UrlMatch.Archived -> viewModel.restore(match.game)

                is UrlMatch.Known -> viewModel.addFromCatalog(match.catalogGame)

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
            CustomGameFields(state = form, match = match, onDone = submit)

            // The button stays enabled for an unusable link, so pressing it can validate the link
            // and show the error.
            Button(
                onClick = submit,
                enabled = form.ready(match),
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
