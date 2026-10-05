package org.shawnz.dailytracker.ui.share

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import org.shawnz.dailytracker.data.EntryEntity
import org.shawnz.dailytracker.data.Game
import org.shawnz.dailytracker.data.Repository
import org.shawnz.dailytracker.share.ConfirmShareActivity
import org.shawnz.dailytracker.ui.STOP_TIMEOUT_MILLIS
import java.time.LocalDate
import javax.inject.Inject

/**
 * What a shared result is about to be saved as.
 *
 * @property games Null until the games have loaded.
 * @property day The puzzle day the result will be filed under. Null until a game is chosen.
 * @property entries The chosen game's entries, for the day picker and the replacement warning.
 */
data class ConfirmShareState(
    val games: List<Game>? = null,
    val game: Game? = null,
    val day: LocalDate? = null,
    val entries: List<EntryEntity> = emptyList(),
    val currentDeviceDay: LocalDate = LocalDate.now(),
) {
    /** Every puzzle day with an entry. Highlighted in the day picker. */
    val playedDays: Set<LocalDate> get() = entries.mapTo(HashSet()) { it.puzzleDay }

    /** Whether saving would replace an entry already on [day]. */
    val clash: Boolean
        get() = day != null && entries.any { it.puzzleDay == day }
}

@HiltViewModel
class ConfirmShareViewModel
    @Inject
    constructor(
        private val repo: Repository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val sharedText: String =
            savedStateHandle.get<String>(ConfirmShareActivity.EXTRA_SHARED_TEXT).orEmpty()

        private val pickedGameId =
            MutableStateFlow(
                savedStateHandle
                    .get<Long>(ConfirmShareActivity.EXTRA_MATCHED_GAME_ID)
                    ?.takeIf { it != ConfirmShareActivity.NO_GAME },
            )

        /**
         * Null until a day is picked. Until then, [state] uses the day stated in the text, or else
         * the game's current puzzle day.
         */
        private val chosenDay = MutableStateFlow<LocalDate?>(null)

        @OptIn(ExperimentalCoroutinesApi::class)
        private val entries =
            pickedGameId.flatMapLatest { id ->
                if (id == null) flowOf(emptyList()) else repo.observeEntries(id)
            }

        /**
         * True once saving has started. [state] is not updated after that, so the saved entry is
         * not shown as a clash.
         */
        private var frozen = false

        val state: StateFlow<ConfirmShareState> =
            combine(
                repo.observeActiveGames(),
                pickedGameId,
                chosenDay,
                entries,
            ) { games, id, chosen, rows ->
                val game = games.firstOrNull { it.id == id }
                ConfirmShareState(
                    games = games,
                    game = game,
                    day =
                        chosen ?: game?.let {
                            val today = it.currentPuzzleDay()
                            it.parser
                                ?.parse(sharedText)
                                ?.day
                                ?.takeIf { day -> !day.isAfter(today) }
                                ?: today
                        },
                    entries = rows,
                )
            }.takeWhile { !frozen }
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    ConfirmShareState(),
                )

        fun pickGame(game: Game) {
            pickedGameId.value = game.id
            chosenDay.value = null
        }

        fun pickDay(day: LocalDate) {
            chosenDay.value = day
        }

        private val _saved = MutableStateFlow(false)

        /** True once the result is saved, so the dialog should close. */
        val saved: StateFlow<Boolean> = _saved.asStateFlow()

        fun save(
            game: Game,
            day: LocalDate,
        ) = viewModelScope.launch {
            if (frozen) return@launch
            frozen = true
            repo.record(game, sharedText, day)
            _saved.value = true
        }
    }
