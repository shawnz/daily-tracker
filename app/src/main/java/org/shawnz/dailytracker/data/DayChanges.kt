package org.shawnz.dailytracker.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.shawnz.dailytracker.time.nextAt
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.milliseconds

/**
 * Returns the next instant after [now] at which the possible puzzle days of a game in [games]
 * change, or at which the device's date changes if that is sooner.
 */
fun nextDayChange(
    games: List<Game>,
    now: ZonedDateTime = ZonedDateTime.now(),
): Instant {
    val puzzleDays = games.map { it.nextPossiblePuzzleDaysChange(now) }
    val midnight = now.nextAt(LocalTime.MIDNIGHT)
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
            val now = ZonedDateTime.now()
            val wait = Duration.between(now.toInstant(), nextDayChange(games, now))
            delay(wait.toMillis().coerceAtLeast(1).milliseconds)
        }
    }
