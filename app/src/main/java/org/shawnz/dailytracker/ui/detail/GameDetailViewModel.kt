package org.shawnz.dailytracker.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import org.shawnz.dailytracker.data.EntryEntity
import org.shawnz.dailytracker.data.Game
import org.shawnz.dailytracker.data.GameProgress
import org.shawnz.dailytracker.data.PuzzleStatus
import org.shawnz.dailytracker.data.Repository
import org.shawnz.dailytracker.data.dayChanges
import org.shawnz.dailytracker.data.gameProgress
import org.shawnz.dailytracker.ui.STOP_TIMEOUT_MILLIS
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * The state of the detail screen for one game on one day.
 *
 * @property selectedDay The day selected on the day card. Null until the game loads.
 * @property playedDays Every puzzle day with an entry. Highlighted in the day picker.
 * @property entries All of this game's entries, for its aggregate figures.
 * @property possiblePuzzleDays The range of days the game's puzzle day can be. Null until the game
 *   is loaded. The start is the day selected with the "Latest" button. The end is the last day of
 *   the day card's pages and of the day picker.
 * @property now The time that the state was computed for. The day cards are computed for the same
 *   time. Null until the game loads.
 */
data class DetailState(
    val game: Game? = null,
    val selectedDay: LocalDate? = null,
    val playedDays: Set<LocalDate> = emptySet(),
    val entries: List<EntryEntity> = emptyList(),
    val progress: GameProgress? = null,
    val possiblePuzzleDays: ClosedRange<LocalDate>? = null,
    val now: ZonedDateTime? = null,
) {
    val currentDeviceDay: LocalDate? get() = now?.toLocalDate()

    /** The day card for [day]. Null until the game loads. */
    fun dayCard(day: LocalDate): DayCard? {
        val game = game ?: return null
        val possible = possiblePuzzleDays ?: return null
        val now = now ?: return null
        return DayCard(game, day, game.statusOn(day, entries, now), possible)
    }

    /** The day card for [selectedDay]. Null until the game loads. */
    val selected: DayCard? get() = selectedDay?.let { dayCard(it) }
}

/**
 * The contents of the day card for one day.
 *
 * @property status The state of the puzzle for [day].
 * @property possiblePuzzleDays The range of days the game's puzzle day can be.
 */
data class DayCard(
    val game: Game,
    val day: LocalDate,
    val status: PuzzleStatus,
    val possiblePuzzleDays: ClosedRange<LocalDate>,
) {
    val entry: EntryEntity? get() = (status as? PuzzleStatus.Done)?.entry

    /** The address the Play button opens, from [Game.playUrl]. Null when the puzzle can't be played. */
    val playUrl: String? get() = game.playUrl(day, status, possiblePuzzleDays)

    /**
     * The address the Open puzzle button opens: the page for the puzzle, when it is done. Null when
     * the puzzle isn't done or the game has no such page.
     */
    val puzzleUrl: String? get() = if (status is PuzzleStatus.Done) game.catalogGame?.dayUrl(day) else null
}

/**
 * A write in which a saved entry or its text was lost.
 *
 * @property previous The entry as it was before the write.
 * @property deleted Whether the whole entry was deleted. Otherwise only the text was replaced or
 *   removed.
 */
data class UndoableChange(
    val previous: EntryEntity,
    val deleted: Boolean,
)

@HiltViewModel(assistedFactory = GameDetailViewModel.Factory::class)
class GameDetailViewModel
    @AssistedInject
    constructor(
        private val repo: Repository,
        @Assisted private val gameId: Long,
        @Assisted private val openingDay: LocalDate,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(
                gameId: Long,
                openingDay: LocalDate,
            ): GameDetailViewModel
        }

        /**
         * The day chosen by the user, or null to show [openingDay]. It is changed only by the user,
         * so the card stays on the same day when the possible days change.
         */
        private val chosenDay = MutableStateFlow<LocalDate?>(null)

        /**
         * True once archiving or deleting has started. [state] is not updated after that, so a
         * deleted game is still shown.
         */
        private var frozen = false

        @OptIn(ExperimentalCoroutinesApi::class)
        val state: StateFlow<DetailState> =
            combine(
                repo.observeGame(gameId),
                repo.observeEntries(gameId),
                chosenDay,
            ) { game, entries, chosen -> Triple(game, entries, chosen) }
                .flatMapLatest { (game, entries, chosen) ->
                    dayChanges(listOfNotNull(game)).map { detailState(game, entries, chosen, openingDay) }
                }.takeWhile { !frozen }
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    DetailState(),
                )

        fun selectDay(day: LocalDate) {
            chosenDay.value = day
        }

        private val _undoable = MutableStateFlow<UndoableChange?>(null)

        /** The last [UndoableChange], until it is undone or dismissed. */
        val undoable: StateFlow<UndoableChange?> = _undoable.asStateFlow()

        /**
         * Saves [text] as the result for the selected day. With blank text, a play is recorded
         * with no result. Replacing or removing saved text sets [undoable].
         */
        fun record(text: String?) =
            viewModelScope.launch {
                val game = repo.getGame(gameId) ?: return@launch
                val day = state.value.selectedDay ?: return@launch
                // A puzzle that isn't released yet can't have a result.
                if (state.value.selected?.status is PuzzleStatus.Unreleased) return@launch
                val saved = text?.takeIf { it.isNotBlank() }
                val previous = repo.entryFor(game, day)
                repo.record(game, saved, day)
                if (previous?.rawShareText != null && !previous.hasText(saved)) {
                    _undoable.value = UndoableChange(previous, deleted = false)
                }
            }

        /** Deletes the selected day's entry and sets [undoable]. */
        fun deleteSelected() =
            viewModelScope.launch {
                val game = repo.getGame(gameId) ?: return@launch
                val day = state.value.selectedDay ?: return@launch
                val previous = repo.entryFor(game, day) ?: return@launch
                repo.deleteEntry(previous)
                _undoable.value = UndoableChange(previous, deleted = true)
            }

        /** Writes back the entry from before [change], if [change] is still [undoable]. */
        fun undo(change: UndoableChange) =
            viewModelScope.launch {
                if (!_undoable.compareAndSet(change, null)) return@launch
                repo.restoreEntry(change.previous)
            }

        /** Clears [undoable], if it is still [change]. */
        fun dismissUndo(change: UndoableChange) {
            _undoable.compareAndSet(change, null)
        }

        fun setReminders(enabled: Boolean) =
            viewModelScope.launch {
                val game = repo.getGame(gameId) ?: return@launch
                repo.setReminders(game, enabled)
            }

        private val _closed = MutableStateFlow(false)

        /** True once the game has been archived or deleted, so the screen should close. */
        val closed: StateFlow<Boolean> = _closed.asStateFlow()

        fun setArchived(archived: Boolean) =
            viewModelScope.launch {
                if (frozen) return@launch
                val game = repo.getGame(gameId) ?: return@launch
                frozen = true
                repo.setArchived(game, archived)
                _closed.value = true
            }

        fun delete() =
            viewModelScope.launch {
                if (frozen) return@launch
                val game = repo.getGame(gameId) ?: return@launch
                frozen = true
                repo.deleteGame(game)
                _closed.value = true
            }
    }

private fun detailState(
    game: Game?,
    entries: List<EntryEntity>,
    chosen: LocalDate?,
    openingDay: LocalDate,
): DetailState {
    val now = ZonedDateTime.now()
    val day = chosen ?: openingDay.takeIf { game != null }
    return DetailState(
        game = game,
        selectedDay = day,
        playedDays = entries.mapTo(HashSet()) { it.puzzleDay },
        entries = entries,
        progress = game?.let { gameProgress(it, entries) },
        possiblePuzzleDays = game?.possiblePuzzleDays(entries, now),
        now = now.takeIf { game != null },
    )
}
