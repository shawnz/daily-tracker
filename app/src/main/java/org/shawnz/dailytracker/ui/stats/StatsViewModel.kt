package org.shawnz.dailytracker.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.shawnz.dailytracker.data.EntryEntity
import org.shawnz.dailytracker.data.Game
import org.shawnz.dailytracker.data.Repository
import org.shawnz.dailytracker.data.gameProgress
import org.shawnz.dailytracker.ui.STOP_TIMEOUT_MILLIS
import javax.inject.Inject

/**
 * Statistics for one game, based only on which days were played, so they apply to every game.
 *
 * @property entries The game's entries, which its renderer uses for game-specific figures such as
 *   scores.
 */
data class GameStat(
    val game: Game,
    val plays: Int,
    val streak: Int,
    val completion: Float,
    val entries: List<EntryEntity>,
)

data class StatsState(
    val perGame: List<GameStat>,
    val totalPlays: Int,
    val overallCompletion: Float,
    val bestStreak: Int,
)

@HiltViewModel
class StatsViewModel
    @Inject
    constructor(
        repo: Repository,
    ) : ViewModel() {
        /**
         * Null until loaded.
         */
        val state: StateFlow<StatsState?> =
            combine(
                repo.observeActiveGames(),
                repo.observeAllEntries(),
            ) { games, entries ->
                val byGame = entries.groupBy { it.gameId }
                val progress = games.map { game -> game to gameProgress(game, byGame[game.id].orEmpty()) }

                // Days played over days possible, summed across all games. A game first played a few
                // days ago counts for less than one played all month.
                val possibleTotal = progress.sumOf { it.second.possibleInWindow }
                val doneTotal = progress.sumOf { it.second.playedInWindow }

                StatsState(
                    perGame =
                        progress.map { (game, it) ->
                            GameStat(
                                game = game,
                                plays = it.plays,
                                streak = it.streak,
                                completion = it.completion,
                                entries = byGame[game.id].orEmpty(),
                            )
                        },
                    // Summed over the active games, since [entries] also includes entries for archived games.
                    totalPlays = progress.sumOf { it.second.plays },
                    overallCompletion = if (possibleTotal > 0) doneTotal.toFloat() / possibleTotal else 0f,
                    bestStreak = progress.maxOfOrNull { it.second.streak } ?: 0,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)
    }
