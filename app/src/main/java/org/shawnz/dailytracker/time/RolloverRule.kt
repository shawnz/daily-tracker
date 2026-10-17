package org.shawnz.dailytracker.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** The zone in which a game's puzzle day is computed. */
sealed interface RolloverZone {
    /** The device's zone at the time of each calculation. */
    data object Device : RolloverZone

    /** A fixed zone. A UTC offset such as `UTC-04:00` has no daylight saving. */
    data class Named(
        val id: ZoneId,
    ) : RolloverZone {
        constructor(id: String) : this(ZoneId.of(id))
    }
}

/**
 * The puzzle day of a game is the date in [zone]. From [time] until midnight, it is the day after.
 *
 * `now` is the current time in the device's zone.
 *
 * @property zone The zone of [time] and of the date.
 * @property time The time of day of the change, or null if the next puzzle is published at no
 *   fixed hour. With [LocalTime.MIDNIGHT], the puzzle day is always the date.
 */
data class RolloverRule(
    val zone: RolloverZone,
    val time: LocalTime?,
) {
    /**
     * Returns the range of days the puzzle day can be at [now].
     *
     * With a known [time], the range is one day. With an unknown time, it is the date in [zone] and
     * the day after.
     */
    fun possiblePuzzleDays(now: ZonedDateTime = ZonedDateTime.now()): ClosedRange<LocalDate> {
        val local = inZone(now).toLocalDateTime()
        val date = local.toLocalDate()
        if (time == null) return date..date.plusDays(1)
        val nextDay = time != LocalTime.MIDNIGHT && !local.toLocalTime().isBefore(time)
        val day = if (nextDay) date.plusDays(1) else date
        return day..day
    }

    fun nextRollover(now: ZonedDateTime = ZonedDateTime.now()): Instant? = time?.let { inZone(now).nextAt(it) }

    /**
     * Returns the next instant at which [possiblePuzzleDays] changes without a new result: [time] in
     * [zone], or midnight in [zone] if [time] is null.
     */
    fun nextPossiblePuzzleDaysChange(now: ZonedDateTime = ZonedDateTime.now()): Instant = inZone(now).nextAt(time ?: LocalTime.MIDNIGHT)

    private fun inZone(now: ZonedDateTime): ZonedDateTime =
        when (zone) {
            RolloverZone.Device -> now
            is RolloverZone.Named -> now.withZoneSameInstant(zone.id)
        }

    companion object {
        /** The rule for a game that has no catalog entry. */
        val DEFAULT = RolloverRule(RolloverZone.Device, time = null)

        val LOCAL_MIDNIGHT = RolloverRule(RolloverZone.Device, LocalTime.MIDNIGHT)
        val UTC_MIDNIGHT = RolloverRule(RolloverZone.Named("UTC"), LocalTime.MIDNIGHT)
        val NYC_MIDNIGHT = RolloverRule(RolloverZone.Named("America/New_York"), LocalTime.MIDNIGHT)
        val LA_MIDNIGHT = RolloverRule(RolloverZone.Named("America/Los_Angeles"), LocalTime.MIDNIGHT)
    }
}

/** The first instant after this date-time at which the time of day in its zone is [time]. */
internal fun ZonedDateTime.nextAt(time: LocalTime): Instant {
    val today = with(time)
    return (if (today.isAfter(this)) today else plusDays(1).with(time)).toInstant()
}
