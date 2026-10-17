package org.shawnz.dailytracker.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.shawnz.dailytracker.time.nextAt
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import kotlin.time.Duration.Companion.milliseconds

/**
 * Returns the next instant at which the possible puzzle days of a game in [games] change, or at
 * which the device's date changes if that is sooner.
 */
fun nextDayChange(
    games: List<Game>,
    clock: Clock = Clock.systemDefaultZone(),
): Instant {
    val puzzleDays = games.map { it.nextPossiblePuzzleDaysChange(clock) }
    val midnight = nextAt(clock.zone, LocalTime.MIDNIGHT, clock)
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
