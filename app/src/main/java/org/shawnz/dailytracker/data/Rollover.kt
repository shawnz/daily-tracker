package org.shawnz.dailytracker.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.milliseconds

/**
 * When a game's puzzle day changes.
 *
 * [UNSCHEDULED] has no known rollover time. [LOCAL_MIDNIGHT] changes at midnight on the
 * device. [FIXED_ZONE] changes at a time of day in a named zone.
 *
 * [currentPuzzleDay] returns the device date for both [UNSCHEDULED] and [LOCAL_MIDNIGHT].
 * They differ only in [nextRollover], which returns null for [UNSCHEDULED].
 */
enum class RolloverType { UNSCHEDULED, LOCAL_MIDNIGHT, FIXED_ZONE }

/**
 * The puzzle day a game is on now.
 *
 * [minuteOfDay] is the time of day when the next day's puzzle becomes available. Use 0 for
 * midnight, and 1320 for 22:00.
 */
fun currentPuzzleDay(
    type: RolloverType,
    zoneId: String?,
    minuteOfDay: Int,
    clock: Clock = Clock.systemDefaultZone(),
): LocalDate {
    val zone = rolloverZone(type, zoneId, clock) ?: return LocalDate.now(clock)
    val now = clock.instant().atZone(zone)
    // Shift the time forward to the next rollover. The date of the result is the current
    // puzzle day. With a minuteOfDay of 0, the modulo makes the shift 0.
    val shiftMinutes = ((1440 - minuteOfDay) % 1440).toLong()
    return now.plusMinutes(shiftMinutes).toLocalDate()
}

/** The deadline for today's puzzle, or null where no rollover time is known. */
fun nextRollover(
    type: RolloverType,
    zoneId: String?,
    minuteOfDay: Int,
    clock: Clock = Clock.systemDefaultZone(),
): Instant? {
    val zone = rolloverZone(type, zoneId, clock) ?: return null
    return nextAt(zone, minuteOfDay, clock)
}

/**
 * When [currentPuzzleDay] next changes. A game with no rollover time uses the device date,
 * which changes at local midnight.
 */
fun nextPuzzleDayChange(
    type: RolloverType,
    zoneId: String?,
    minuteOfDay: Int,
    clock: Clock = Clock.systemDefaultZone(),
): Instant {
    val zone = rolloverZone(type, zoneId, clock) ?: return nextAt(clock.zone, 0, clock)
    return nextAt(zone, minuteOfDay, clock)
}

/** Midnight, or the next puzzle day of [games], whichever is sooner. */
fun nextDayChange(
    games: List<Game>,
    clock: Clock = Clock.systemDefaultZone(),
): Instant {
    val puzzleDays =
        games.map {
            nextPuzzleDayChange(it.rolloverType, it.rolloverZoneId, it.rolloverMinuteOfDay, clock)
        }
    val midnight = nextPuzzleDayChange(RolloverType.LOCAL_MIDNIGHT, null, 0, clock)
    return (puzzleDays + midnight).min()
}

/**
 * Emits once now, then again at each [nextDayChange], so that state depending on the current
 * day can be recomputed when it changes.
 */
fun dayChanges(games: List<Game>): Flow<Unit> =
    flow {
        while (true) {
            emit(Unit)
            val wait = Duration.between(Instant.now(), nextDayChange(games))
            delay(wait.toMillis().coerceAtLeast(1).milliseconds)
        }
    }

/** The next instant at which the time of day in [zone] is [minuteOfDay]. */
private fun nextAt(
    zone: ZoneId,
    minuteOfDay: Int,
    clock: Clock,
): Instant {
    val now = clock.instant()
    val time = LocalTime.ofSecondOfDay(Math.floorMod(minuteOfDay, 1440) * 60L)
    val today = now.atZone(zone).toLocalDate()
    val next =
        ZonedDateTime.of(today, time, zone).takeIf { it.toInstant().isAfter(now) }
            ?: ZonedDateTime.of(today.plusDays(1), time, zone)
    return next.toInstant()
}

/** The zone a rollover is measured in, or null when no rollover time is known. */
private fun rolloverZone(
    type: RolloverType,
    zoneId: String?,
    clock: Clock,
): ZoneId? =
    when (type) {
        RolloverType.UNSCHEDULED -> null
        RolloverType.LOCAL_MIDNIGHT -> clock.zone
        RolloverType.FIXED_ZONE -> ZoneId.of(requireNotNull(zoneId))
    }
