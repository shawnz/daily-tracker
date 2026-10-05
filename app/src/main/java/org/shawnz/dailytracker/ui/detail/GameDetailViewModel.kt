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
import org.shawnz.dailytracker.data.Repository
import org.shawnz.dailytracker.data.dayChanges
import org.shawnz.dailytracker.data.gameProgress
import org.shawnz.dailytracker.ui.STOP_TIMEOUT_MILLIS
import java.time.LocalDate

/**
 * The state of the detail screen for one game on one day.
 *
 * @property selectedDay The day shown on the day card, the only part of the screen that
 *   depends on it. Null until the game loads.
 * @property playedDays Every puzzle day with an entry. Highlighted in the day picker.
 * @property entries All of this game's entries, for its aggregate figures.
 * @property currentPuzzleDay This game's current puzzle day, which is also the latest selectable
 *   one.
 */
data class DetailState(
    val game: Game? = null,
    val selectedDay: LocalDate? = null,
    val selectedEntry: EntryEntity? = null,
    val playedDays: Set<LocalDate> = emptySet(),
    val entries: List<EntryEntity> = emptyList(),
    val progress: GameProgress? = null,
    val currentPuzzleDay: LocalDate? = null,
    val currentDeviceDay: LocalDate? = null,
) {
    val isToday: Boolean get() = selectedDay != null && selectedDay == currentDeviceDay

    val canGoForward: Boolean
        get() = selectedDay != null && currentPuzzleDay != null && selectedDay < currentPuzzleDay
}

@HiltViewModel(assistedFactory = GameDetailViewModel.Factory::class)
class GameDetailViewModel
    @AssistedInject
    constructor(
        private val repo: Repository,
        @Assisted private val gameId: Long,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(gameId: Long): GameDetailViewModel
        }

        /**
         * The day shown on the day card. Null until the game has loaded, then that game's
         * current puzzle day. A rollover changes [DetailState.currentPuzzleDay] but not this, so
         * the card stays on the day the user chose.
         */
        private val chosenDay = MutableStateFlow<LocalDate?>(null)

        init {
            viewModelScope.launch {
                repo.getGame(gameId)?.let { chosenDay.compareAndSet(null, it.currentPuzzleDay()) }
            }
        }

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
                    dayChanges(listOfNotNull(game)).map { detailState(game, entries, chosen) }
                }.takeWhile { !frozen }
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    DetailState(),
                )

        fun selectDay(day: LocalDate) {
            chosenDay.value = day
        }

        /**
         * Moves by [days], never past the current puzzle day.
         *
         * There is no earliest day, because a game may have been played for years before it was
         * added here.
         */
        fun stepDay(days: Long) {
            val now = state.value
            val day = now.selectedDay ?: return
            if (days > 0 && !now.canGoForward) return
            chosenDay.value = day.plusDays(days)
        }

        /** Saves the selected day. Blank text records a play with no result. */
        fun record(text: String?) =
            viewModelScope.launch {
                val game = repo.getGame(gameId) ?: return@launch
                val day = state.value.selectedDay ?: return@launch
                repo.record(game, text?.takeIf { it.isNotBlank() }, day)
            }

        fun clearSelected() =
            viewModelScope.launch {
                val game = repo.getGame(gameId) ?: return@launch
                val day = state.value.selectedDay ?: return@launch
                repo.entryFor(game, day)?.let { repo.deleteEntry(it) }
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
): DetailState {
    val current = game?.currentPuzzleDay()
    val day = chosen ?: current
    return DetailState(
        game = game,
        selectedDay = day,
        selectedEntry = day?.let { on -> entries.firstOrNull { it.puzzleDay == on } },
        playedDays = entries.mapTo(HashSet()) { it.puzzleDay },
        entries = entries,
        progress = game?.let { gameProgress(it, entries) },
        currentPuzzleDay = current,
        currentDeviceDay = LocalDate.now(),
    )
}
