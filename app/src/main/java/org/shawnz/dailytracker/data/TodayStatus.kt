package org.shawnz.dailytracker.data

import java.time.Instant

sealed interface TodayStatus {
    /**
     * The current entry's text was saved on the device's current date.
     *
     * @property entry The current entry.
     */
    data class Done(
        val entry: EntryEntity,
    ) : TodayStatus

    /**
     * The current entry's text was saved on another date, and the next puzzle isn't out. Either the
     * game's rollover time is known, or the next puzzle's day isn't a possible puzzle day yet.
     *
     * @property nextRollover When the next puzzle is out, or null when the rollover time is unknown.
     */
    data class Unreleased(
        val nextRollover: Instant?,
    ) : TodayStatus

    /**
     * Neither [Done] nor [Unreleased].
     *
     * @property nextRollover The game's next rollover, or null when its rollover time is unknown.
     */
    data class Unplayed(
        val nextRollover: Instant?,
    ) : TodayStatus
}
