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
 * They differ in [nextRollover], which returns null for [UNSCHEDULED], and in
 * [lastPuzzleDay], which is a day later for [UNSCHEDULED].
 */
enum class RolloverType { UNSCHEDULED, LOCAL_MIDNIGHT, FIXED_ZONE }

/**
 * The puzzle day a game is on now.
 *
 * [time] is the time of day when the next day's puzzle becomes available.
 */
fun currentPuzzleDay(
    type: RolloverType,
    zoneId: String?,
    time: LocalTime,
    clock: Clock = Clock.systemDefaultZone(),
): LocalDate {
    val zone = rolloverZone(type, zoneId, clock) ?: return LocalDate.now(clock)
    val now = clock.instant().atZone(zone)
    val today = now.toLocalDate()
    // From [time] until midnight, the puzzle is the next day's. Midnight is the start of a
    // day, so with a time of midnight the puzzle day is always the date.
    val nextDay = time != LocalTime.MIDNIGHT && !now.toLocalTime().isBefore(time)
    return if (nextDay) today.plusDays(1) else today
}

/**
 * The last puzzle day a result can be recorded for.
 *
 * This is equal to [currentPuzzleDay], except where no rollover time is known. In that case
 * the next puzzle may be published before the device date changes, so this is the day after
 * [currentPuzzleDay].
 */
fun lastPuzzleDay(
    type: RolloverType,
    zoneId: String?,
    time: LocalTime,
    clock: Clock = Clock.systemDefaultZone(),
): LocalDate {
    val current = currentPuzzleDay(type, zoneId, time, clock)
    return if (type == RolloverType.UNSCHEDULED) current.plusDays(1) else current
}

/** The deadline for today's puzzle, or null where no rollover time is known. */
fun nextRollover(
    type: RolloverType,
    zoneId: String?,
    time: LocalTime,
    clock: Clock = Clock.systemDefaultZone(),
): Instant? {
    val zone = rolloverZone(type, zoneId, clock) ?: return null
    return nextAt(zone, time, clock)
}

/**
 * When [currentPuzzleDay] next changes. A game with no rollover time uses the device date,
 * which changes at local midnight.
 */
fun nextPuzzleDayChange(
    type: RolloverType,
    zoneId: String?,
    time: LocalTime,
    clock: Clock = Clock.systemDefaultZone(),
): Instant {
    val zone = rolloverZone(type, zoneId, clock) ?: return nextAt(clock.zone, LocalTime.MIDNIGHT, clock)
    return nextAt(zone, time, clock)
}

/** Midnight, or the next puzzle day of [games], whichever is sooner. */
fun nextDayChange(
    games: List<Game>,
    clock: Clock = Clock.systemDefaultZone(),
): Instant {
    val puzzleDays =
        games.map {
            nextPuzzleDayChange(it.rolloverType, it.rolloverZoneId, it.rolloverTime, clock)
        }
    val midnight = nextPuzzleDayChange(RolloverType.LOCAL_MIDNIGHT, null, LocalTime.MIDNIGHT, clock)
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
