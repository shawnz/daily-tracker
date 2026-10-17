package org.shawnz.dailytracker.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.shawnz.dailytracker.data.EntryEntity
import org.shawnz.dailytracker.data.Game
import org.shawnz.dailytracker.data.Repository
import org.shawnz.dailytracker.data.dayChanges
import org.shawnz.dailytracker.time.earliestDateAnywhere
import org.shawnz.dailytracker.ui.STOP_TIMEOUT_MILLIS
import javax.inject.Inject

data class GameRowState(
    val game: Game,
    val entry: EntryEntity?,
) {
    val done: Boolean get() = entry != null
}

data class TodayState(
    val rows: List<GameRowState>,
) {
    val doneCount: Int get() = rows.count { it.done }
}

@HiltViewModel
class TodayViewModel
    @Inject
    constructor(
        repo: Repository,
    ) : ViewModel() {
        /**
         * Null until loaded. There are no rows when no games are tracked. Results are read from
         * [earliestDateAnywhere] onward, because no puzzle day can be earlier.
         */
        @OptIn(ExperimentalCoroutinesApi::class)
        val state: StateFlow<TodayState?> =
            combine(
                repo.observeActiveGames(),
                repo.observeRecentEntries(earliestDateAnywhere()),
            ) { games, entries -> games to entries }
                .flatMapLatest { (games, entries) ->
                    dayChanges(games).map { rowsFor(games, entries) }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)
    }

private fun rowsFor(
    games: List<Game>,
    entries: List<EntryEntity>,
) = TodayState(
    rows =
        games.map { game ->
            GameRowState(game = game, entry = game.currentEntry(entries))
        },
)
