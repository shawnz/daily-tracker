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
import org.shawnz.dailytracker.ui.STOP_TIMEOUT_MILLIS
import java.time.LocalDate
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
         * Null until loaded. An empty [TodayState.rows] means the user doesn't track any games.
         *
         * Entries from two days before the device's date are also read, because time zones are
         * at most 26 hours apart, so a game's current day can be up to two days behind.
         */
        @OptIn(ExperimentalCoroutinesApi::class)
        val state: StateFlow<TodayState?> =
            combine(
                repo.observeActiveGames(),
                repo.observeRecentEntries(LocalDate.now().minusDays(2)),
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
            val day = game.currentPuzzleDay()
            GameRowState(
                game = game,
                entry = entries.firstOrNull { it.gameId == game.id && it.puzzleDay == day },
            )
        },
)
