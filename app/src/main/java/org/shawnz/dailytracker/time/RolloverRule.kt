package org.shawnz.dailytracker.time

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
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
 * @property zone The zone of [time] and of the date.
 * @property time The time of day of the change, or null if the next puzzle is published at no
 *   fixed hour. With [LocalTime.MIDNIGHT], the puzzle day is always the date.
 */
data class RolloverRule(
    val zone: RolloverZone,
    val time: LocalTime?,
) {
    /**
     * Returns the range of days the puzzle day can be at the time of [clock].
     *
     * With a known [time], the range is one day. With an unknown time, it is the date in [zone] and
     * the day after, or only the day after if [rolloverObserved].
     */
    fun possiblePuzzleDays(
        rolloverObserved: Boolean,
        clock: Clock = Clock.systemDefaultZone(),
    ): ClosedRange<LocalDate> {
        val now = LocalDateTime.now(clock.withZone(zoneId(clock)))
        val date = now.toLocalDate()
        if (time == null) {
            val nextDay = date.plusDays(1)
            return if (rolloverObserved) nextDay..nextDay else date..nextDay
        }
        val nextDay = time != LocalTime.MIDNIGHT && !now.toLocalTime().isBefore(time)
        val day = if (nextDay) date.plusDays(1) else date
        return day..day
    }

    fun nextRollover(clock: Clock = Clock.systemDefaultZone()): Instant? = time?.let { nextAt(zoneId(clock), it, clock) }

    /**
     * Returns the next instant at which [possiblePuzzleDays] changes without a new result: [time] in
     * [zone], or midnight in [zone] if [time] is null.
     */
    fun nextPossiblePuzzleDaysChange(clock: Clock = Clock.systemDefaultZone()): Instant =
        nextAt(zoneId(clock), time ?: LocalTime.MIDNIGHT, clock)

    private fun zoneId(clock: Clock): ZoneId =
        when (zone) {
            RolloverZone.Device -> clock.zone
            is RolloverZone.Named -> zone.id
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

/** The next instant at which the time of day in [zone] is [time]. */
internal fun nextAt(
    zone: ZoneId,
    time: LocalTime,
    clock: Clock,
): Instant {
    val now = clock.instant()
    val today = now.atZone(zone).toLocalDate()
    val next =
        ZonedDateTime.of(today, time, zone).takeIf { it.toInstant().isAfter(now) }
            ?: ZonedDateTime.of(today.plusDays(1), time, zone)
    return next.toInstant()
}
