package org.shawnz.dailytracker.data

import java.time.Instant

sealed interface PuzzleStatus {
    /**
     * The puzzle has a result.
     *
     * @property entry The result.
     */
    data class Done(
        val entry: EntryEntity,
    ) : PuzzleStatus

    /**
     * The puzzle isn't released yet.
     *
     * @property nextRollover When the puzzle is released, or null when that time isn't known.
     * @property currentPuzzleUnplayed Whether the game's current puzzle, which is older than this one,
     *   has no result.
     */
    data class Unreleased(
        val nextRollover: Instant?,
        val currentPuzzleUnplayed: Boolean,
    ) : PuzzleStatus

    /**
     * The puzzle has no result, and its day is one of the game's possible puzzle days. When the
     * rollover time is unknown, there are two possible days, and only one of them is current.
     *
     * @property nextRollover The game's next rollover, or null when its rollover time is unknown.
     */
    data class Unplayed(
        val nextRollover: Instant?,
    ) : PuzzleStatus

    /**
     * A newer puzzle was released, and this one has no result saved.
     *
     * @property currentPuzzleUnplayed Whether the game's current puzzle, which is newer than this one,
     *   has no result.
     */
    data class Missed(
        val currentPuzzleUnplayed: Boolean,
    ) : PuzzleStatus
}
