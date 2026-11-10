package org.shawnz.dailytracker.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.job
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

/**
 * One game's row on the page for a date.
 *
 * @property siteUrl The address a tap opens in a Custom Tab, from [Game.playUrl]. Null when a
 *   tap opens only the detail page.
 */
data class GameRowState(
    val game: Game,
    val status: PuzzleStatus,
    val siteUrl: String?,
)

/**
 * The games' puzzles for one date.
 *
 * @property day The date of the page.
 * @property today The device's date.
 */
data class TodayState(
    val day: LocalDate,
    val today: LocalDate,
    val rows: List<GameRowState>,
) {
    val doneCount: Int get() = rows.count { it.status is PuzzleStatus.Done }
}

/**
 * The dates that the Today screen can show, and the selected one.
 *
 * @property day The selected date.
 * @property today The device's date.
 * @property earliestDay The earliest date that can be shown: the earliest of [today], the earliest
 *   puzzle day with a result, and the earliest possible puzzle day of any game.
 * @property latestDay The latest date that can be shown: the latest possible puzzle day of any
 *   game, or [today] if that is later.
 * @property gamesTracked Whether any game is tracked.
 */
data class TodayFrame(
    val day: LocalDate,
    val today: LocalDate,
    val earliestDay: LocalDate,
    val latestDay: LocalDate,
    val gamesTracked: Boolean,
)

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

        /** Null until loaded. */
        val frame: StateFlow<TodayFrame?> =
            pages
                .map { it.frame() }
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

        /** A flow from [stateOn], and the scope that it is shared in. */
        private class DayState(
            val scope: CoroutineScope,
            val flow: StateFlow<TodayState?>,
        )

        /**
         * The flows from [stateOn], in order from the least recently used. When there are more than
         * [DAY_STATES_KEPT], the oldest is removed and its scope is cancelled.
         */
        private val dayStates =
            object : LinkedHashMap<LocalDate, DayState>(DAY_STATES_KEPT, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<LocalDate, DayState>): Boolean {
                    if (size <= DAY_STATES_KEPT) return false
                    eldest.value.scope.cancel()
                    return true
                }
            }

        /**
         * The state for [day], independent of the selected date. Null until loaded. It is null
         * again after [STOP_TIMEOUT_MILLIS] with no collector, so that an out-of-date value isn't
         * shown. One flow is kept for each recent date, so every collector of a date has the same
         * value.
         */
        fun stateOn(day: LocalDate): StateFlow<TodayState?> =
            dayStates
                .getOrPut(day) {
                    val parent = viewModelScope.coroutineContext
                    val scope = CoroutineScope(parent + Job(parent.job))
                    val flow =
                        combine(pages, repo.observeEntriesOn(day)) { page, entries -> page.copy(day = day).state(entries) }
                            .stateIn(
                                scope,
                                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS, replayExpirationMillis = 0),
                                null,
                            )
                    DayState(scope, flow)
                }.flow
    }

/**
 * The number of dates whose [TodayViewModel.stateOn] flows are kept. It is more than the number of
 * pages that are composed at once, so the flows in use are never the least recently used.
 */
internal const val DAY_STATES_KEPT = 16

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

/** The inputs of [TodayFrame], and of [TodayState] other than the results for [day]. */
internal data class Page(
    val games: List<Game>,
    val recent: List<EntryEntity>,
    val now: ZonedDateTime,
    val day: LocalDate,
    val earliest: LocalDate,
    val latest: LocalDate,
) {
    fun frame(): TodayFrame = TodayFrame(day, now.toLocalDate(), earliest, latest, gamesTracked = games.isNotEmpty())

    fun state(dayEntries: List<EntryEntity>): TodayState {
        val entries = recent + dayEntries
        return TodayState(
            day = day,
            today = now.toLocalDate(),
            rows =
                games.map { game ->
                    val status = game.statusOn(day, entries, now)
                    GameRowState(game, status, game.playUrl(day, status, game.possiblePuzzleDays(entries, now)))
                },
        )
    }
}
