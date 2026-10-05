package org.shawnz.dailytracker.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** How many days the completion rate covers. */
const val COMPLETION_WINDOW_DAYS = 30

/**
 * Game statistics, based on which days have an entry.
 *
 * [playedInWindow] and [possibleInWindow] cover the last [COMPLETION_WINDOW_DAYS] days, or
 * since the earliest recorded play if that is later. They are kept separate so that several
 * games can be pooled by adding both.
 */
data class GameProgress(
    val plays: Int,
    val streak: Int,
    val playedInWindow: Int,
    val possibleInWindow: Int,
) {
    val completion: Float
        get() = if (possibleInWindow > 0) playedInWindow.toFloat() / possibleInWindow else 0f
}

/**
 * Computes the [GameProgress] for [game] from [entries], which must all belong to that game.
 *
 * A game with no entries has zero for every statistic.
 */
fun gameProgress(
    game: Game,
    entries: List<EntryEntity>,
): GameProgress {
    val earliest =
        entries.minOfOrNull { it.puzzleDay }
            ?: return GameProgress(plays = 0, streak = 0, playedInWindow = 0, possibleInWindow = 0)

    val today = game.currentPuzzleDay()
    val start = windowStart(today, earliest)
    return GameProgress(
        plays = entries.size,
        streak = streak(game, entries),
        playedInWindow = entries.count { it.puzzleDay in start..today },
        possibleInWindow = ChronoUnit.DAYS.between(start, today).toInt() + 1,
    )
}

/**
 * The number of consecutive days with an entry in [entries], counting back from [game]'s
 * current puzzle day.
 *
 * Counting may start at the day before, because the current day is not over yet.
 */
private fun streak(
    game: Game,
    entries: List<EntryEntity>,
): Int {
    val days = entries.mapTo(HashSet()) { it.puzzleDay }
    var day = game.currentPuzzleDay()
    if (day !in days) day = day.minusDays(1)
    var count = 0
    while (day in days) {
        count++
        day = day.minusDays(1)
    }
    return count
}

/**
 * The first day of the window ending at [today]. The window covers [COMPLETION_WINDOW_DAYS]
 * days, or starts at [earliestPlay] when that is more recent.
 *
 * [earliestPlay] can be after [today] when a game's rollover rule changes to one that moves
 * its current day backwards, so the result is capped at [today].
 */
private fun windowStart(
    today: LocalDate,
    earliestPlay: LocalDate,
): LocalDate {
    val floor = today.minusDays((COMPLETION_WINDOW_DAYS - 1).toLong())
    return minOf(today, maxOf(floor, earliestPlay))
}
