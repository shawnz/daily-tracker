package org.shawnz.dailytracker.ui.share

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import org.shawnz.dailytracker.data.EntryEntity
import org.shawnz.dailytracker.data.Game
import org.shawnz.dailytracker.data.Repository
import org.shawnz.dailytracker.data.catalog.CatalogGame
import org.shawnz.dailytracker.share.ConfirmShareActivity
import org.shawnz.dailytracker.ui.STOP_TIMEOUT_MILLIS
import java.time.LocalDate
import javax.inject.Inject

/**
 * What a shared result is about to be saved as.
 *
 * @property targets The games to choose from. Null until they have loaded.
 * @property day The puzzle day the result will be filed under. Null until a game is chosen.
 * @property entries The chosen game's entries, for the day picker and the replacement warning.
 * @property allGames Every tracked game, archived ones included. The link of a new game is
 *   matched against these.
 */
data class ConfirmShareState(
    val targets: List<ShareTarget>? = null,
    val allGames: List<Game> = emptyList(),
    val target: ShareTarget? = null,
    val day: LocalDate? = null,
    val entries: List<EntryEntity> = emptyList(),
    val possiblePuzzleDays: ClosedRange<LocalDate>? = null,
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

        private val matched: CatalogGame? =
            savedStateHandle.get<String>(ConfirmShareActivity.EXTRA_MATCHED_GAME)?.let { name ->
                CatalogGame.entries.firstOrNull { it.name == name }
            }

        /** The target for [matched]. A [ShareTarget.New] when that game is not tracked. */
        private val matchedTarget: Flow<ShareTarget?> =
            if (matched == null) {
                flowOf(null)
            } else {
                repo.observeAllGames().map { all ->
                    all.firstOrNull { it.catalogGame == matched }?.let { ShareTarget.Tracked(it) }
                        ?: ShareTarget.New(matched)
                }
            }

        /** The target the user picked, or null if the user hasn't picked one. */
        private val picked = MutableStateFlow<ShareTarget?>(null)

        /** The active games, then [matchedTarget] and [picked] when they are not already listed. */
        private val targets: Flow<List<ShareTarget>> =
            combine(repo.observeActiveGames(), matchedTarget, picked) { active, matched, picked ->
                val tracked = active.map { ShareTarget.Tracked(it) }
                (tracked + listOfNotNull(matched, picked)).distinctBy { target ->
                    if (target is ShareTarget.Tracked) target.game.id else target
                }
            }

        /** The picked target, or [matchedTarget] until one is picked. */
        private val target: Flow<ShareTarget?> =
            combine(matchedTarget, picked) { matched, picked -> picked ?: matched }

        /**
         * The day the user picked, or null if the user hasn't picked one. Until then, the day of
         * the result is the day in the shared text. If the text has no day, or its day is after
         * the end of the game's possible puzzle days, the start of that range is used.
         */
        private val chosenDay = MutableStateFlow<LocalDate?>(null)

        @OptIn(ExperimentalCoroutinesApi::class)
        private val entries =
            target.map { (it as? ShareTarget.Tracked)?.game?.id }.distinctUntilChanged().flatMapLatest { id ->
                if (id == null) flowOf(emptyList()) else repo.observeEntries(id)
            }

        /**
         * True once saving has started. [state] is not updated after that, so the saved entry is
         * not shown as a clash.
         */
        private var frozen = false

        val state: StateFlow<ConfirmShareState> =
            combine(
                targets,
                repo.observeAllGames(),
                target,
                chosenDay,
                entries,
            ) { targets, all, target, chosen, rows ->
                val possible =
                    when (target) {
                        null -> null
                        is ShareTarget.Tracked -> target.game.possiblePuzzleDays(rows)
                        is ShareTarget.New -> target.source.rolloverRule.possiblePuzzleDays()
                    }
                ConfirmShareState(
                    targets = targets,
                    allGames = all,
                    target = target,
                    day =
                        chosen ?: possible?.let { target?.catalogGame?.dayIn(sharedText) ?: it.start },
                    entries = rows,
                    possiblePuzzleDays = possible,
                )
            }.takeWhile { !frozen }
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    ConfirmShareState(),
                )

        fun pickTarget(target: ShareTarget) {
            picked.value = target
            chosenDay.value = null
        }

        fun pickDay(day: LocalDate) {
            chosenDay.value = day
        }

        private val _saved = MutableStateFlow(false)

        /** True once the result is saved, so the dialog should close. */
        val saved: StateFlow<Boolean> = _saved.asStateFlow()

        fun save(
            target: ShareTarget,
            day: LocalDate,
        ) = viewModelScope.launch {
            if (frozen) return@launch
            frozen = true
            val game =
                when (target) {
                    is ShareTarget.Tracked -> {
                        if (target.game.archived) repo.setArchived(target.game, false)
                        target.game
                    }

                    is ShareTarget.New -> {
                        when (val source = target.source) {
                            is CatalogGame -> repo.activate(source)
                            else -> checkNotNull(repo.getGame(repo.addCustom(source.title, source.url)))
                        }
                    }
                }
            repo.record(game, sharedText, day)
            _saved.value = true
        }
    }
