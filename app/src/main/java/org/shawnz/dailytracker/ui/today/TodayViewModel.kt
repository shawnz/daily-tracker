package org.shawnz.dailytracker.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import org.shawnz.dailytracker.data.EntryEntity
import org.shawnz.dailytracker.data.Game
import org.shawnz.dailytracker.data.PuzzleStatus
import org.shawnz.dailytracker.data.Repository
import org.shawnz.dailytracker.data.dayChanges
import org.shawnz.dailytracker.time.earliestDateAnywhere
import org.shawnz.dailytracker.ui.STOP_TIMEOUT_MILLIS
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject

data class GameRowState(
    val game: Game,
    val status: PuzzleStatus,
)

/**
 * The games' puzzles for one date.
 *
 * @property day The date shown.
 * @property today The device's date.
 * @property earliestDay The earliest date that can be shown: the earliest of [today], the earliest
 *   puzzle day with a result, and the earliest possible puzzle day of any game.
 * @property latestDay The latest date that can be shown: the latest possible puzzle day of any
 *   game, or [today] if that is later.
 */
data class TodayState(
    val day: LocalDate,
    val today: LocalDate,
    val earliestDay: LocalDate,
    val latestDay: LocalDate,
    val rows: List<GameRowState>,
) {
    val doneCount: Int get() = rows.count { it.status is PuzzleStatus.Done }

    val canGoBack: Boolean get() = day > earliestDay

    val canGoForward: Boolean get() = day < latestDay
}

@HiltViewModel
class TodayViewModel
    @Inject
    constructor(
        private val repo: Repository,
    ) : ViewModel() {
        /** The date chosen by the user, or null to show the device's date. */
        private val chosenDay = MutableStateFlow<LocalDate?>(null)

        /**
         * The page at the current time, computed again at each day change. Recent results are read
         * from [earliestDateAnywhere] onward, because no possible puzzle day can be earlier.
         */
        @OptIn(ExperimentalCoroutinesApi::class)
        private val pages: Flow<Page> =
            combine(
                repo.observeActiveGames(),
                repo.observeRecentEntries(earliestDateAnywhere()),
                repo.observeEarliestPlayedDay(),
                chosenDay,
            ) { games, recent, earliest, chosen -> Inputs(games, recent, earliest, chosen) }
                .flatMapLatest { inputs ->
                    dayChanges(inputs.games).map {
                        val now = ZonedDateTime.now()
                        // A chosen day that has become the device's date is followed from then on.
                        chosenDay.compareAndSet(now.toLocalDate(), null)
                        inputs.page(now)
                    }
                }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), replay = 1)

        /** The results for the page's day, with that day. Queried again only when the day changes. */
        @OptIn(ExperimentalCoroutinesApi::class)
        private val dayEntries: Flow<Pair<LocalDate, List<EntryEntity>>> =
            pages
                .map { it.day }
                .distinctUntilChanged()
                .flatMapLatest { day -> repo.observeEntriesOn(day).map { day to it } }

        /** Null until loaded. There are no rows when no games are tracked. */
        val state: StateFlow<TodayState?> =
            combine(pages, dayEntries) { page, (day, entries) ->
                // A new day's page is held back until the results for that day are read.
                if (page.day == day) page.state(entries) else null
            }.filterNotNull()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

        /** Every puzzle day with a result. */
        val playedDays: StateFlow<Set<LocalDate>> =
            repo
                .observePlayedDays()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptySet())

        /**
         * Shows [day]. When [day] is the device's date, the device's date is shown from then on,
         * also after midnight.
         */
        fun selectDay(day: LocalDate) {
            chosenDay.value = day.takeIf { it != LocalDate.now() }
        }

        fun stepDay(days: Long) {
            val day = state.value?.day ?: return
            selectDay(day.plusDays(days))
        }
    }

internal data class Inputs(
    val games: List<Game>,
    val recent: List<EntryEntity>,
    val earliestPlayed: LocalDate?,
    val chosen: LocalDate?,
) {
    fun page(now: ZonedDateTime): Page {
        val today = now.toLocalDate()
        val earliestPossible = games.minOfOrNull { it.possiblePuzzleDays(recent, now).start } ?: today
        val earliest = minOf(today, earliestPlayed ?: today, earliestPossible)
        val latest = games.maxOfOrNull { it.possiblePuzzleDays(recent, now).endInclusive }?.let { maxOf(today, it) } ?: today
        return Page(
            games = games,
            recent = recent,
            now = now,
            day = (chosen ?: today).coerceIn(earliest, latest),
            earliest = earliest,
            latest = latest,
        )
    }
}

/** The inputs of [TodayState] other than the results for [day]. */
internal data class Page(
    val games: List<Game>,
    val recent: List<EntryEntity>,
    val now: ZonedDateTime,
    val day: LocalDate,
    val earliest: LocalDate,
    val latest: LocalDate,
) {
    fun state(dayEntries: List<EntryEntity>): TodayState {
        val entries = recent + dayEntries
        return TodayState(
            day = day,
            today = now.toLocalDate(),
            earliestDay = earliest,
            latestDay = latest,
            rows = games.map { GameRowState(it, it.statusOn(day, entries, now)) },
        )
    }
}
