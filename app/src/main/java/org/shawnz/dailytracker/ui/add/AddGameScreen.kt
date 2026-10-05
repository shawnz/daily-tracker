package org.shawnz.dailytracker.ui.add

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.data.searchCatalog
import org.shawnz.dailytracker.data.searchGames
import org.shawnz.dailytracker.ui.PageMargin
import org.shawnz.dailytracker.ui.hostLabel

/** The space between the divider and the first card when the list isn't scrolled. */
private val ListTopPadding = 12.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddGameScreen(
    onBack: () -> Unit,
    onCreateCustom: () -> Unit,
    onAdded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: AddGameViewModel = hiltViewModel()
    val available by vm.available.collectAsStateWithLifecycle()
    val added by vm.added.collectAsStateWithLifecycle()
    LaunchedEffect(added) { if (added) onAdded() }
    val archivedGames by vm.archived.collectAsStateWithLifecycle()

    var query by rememberSaveable { mutableStateOf("") }
    val archived = remember(archivedGames, query) { searchGames(archivedGames.orEmpty(), query) }
    val games = remember(available, query) { available?.let { searchCatalog(it, query) } }

    val listState = rememberLazyListState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_game_title)) },
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
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Outside the list, so the field stays in view as the list scrolls.
            SearchField(
                query = query,
                onQueryChange = { query = it },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = PageMargin, end = PageMargin, bottom = 12.dp),
            )
            // Marks the edge that the cards scroll under. Fades in with the distance scrolled, and
            // is opaque once the first card has reached it.
            HorizontalDivider(
                Modifier.graphicsLayer {
                    alpha =
                        if (listState.firstVisibleItemIndex > 0) {
                            1f
                        } else {
                            val scrolled = listState.firstVisibleItemScrollOffset
                            (scrolled / ListTopPadding.toPx()).coerceAtMost(1f)
                        }
                },
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding =
                    PaddingValues(
                        start = PageMargin,
                        top = ListTopPadding,
                        end = PageMargin,
                        bottom = PageMargin,
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Listed for every search, so a game that isn't found can still be added.
                item {
                    OutlinedCard(
                        onClick = onCreateCustom,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row2(
                                title = stringResource(R.string.add_custom_title),
                                body = stringResource(R.string.add_custom_body),
                            )
                        }
                    }
                }

                // Above the catalog, so archived games are more discoverable. Hidden when no
                // archived games are listed.
                if (archived.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.add_archived_header),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    items(archived, key = { it.id }) { game ->
                        Card(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { vm.restore(game) },
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row2(
                                    title = game.title,
                                    body =
                                        game.catalogGame?.description
                                            ?: hostLabel(game.url).orEmpty(),
                                )
                            }
                        }
                    }
                }

                // Hidden until loaded, so the heading appears together with its entries.
                if (games != null) {
                    item {
                        Text(
                            stringResource(R.string.add_catalog_header),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    if (games.isEmpty()) {
                        item {
                            Text(
                                if (query.isBlank()) {
                                    stringResource(R.string.add_catalog_all_added)
                                } else {
                                    stringResource(
                                        R.string.add_search_no_match,
                                        stringResource(R.string.add_custom_title),
                                    )
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(games, key = { it }) { catalogGame ->
                            Card(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { vm.add(catalogGame) },
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Row2(
                                        title = catalogGame.title,
                                        body = catalogGame.description,
                                    )
                                    // Listed only for a search of its exact title.
                                    if (catalogGame.hidden) {
                                        HiddenGameNote(
                                            catalogGame,
                                            modifier = Modifier.padding(top = 8.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text(stringResource(R.string.add_search_hint)) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
        trailingIcon =
            if (query.isEmpty()) {
                null
            } else {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            painterResource(R.drawable.ic_close),
                            stringResource(R.string.add_search_clear),
                        )
                    }
                }
            },
        singleLine = true,
        // The list is searched on each keystroke, so the Search key only closes the keyboard.
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
    )
}

@Composable
private fun Row2(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
