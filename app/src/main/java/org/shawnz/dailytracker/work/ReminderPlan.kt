package org.shawnz.dailytracker.work

import org.shawnz.dailytracker.data.Game
import org.shawnz.dailytracker.data.ReminderMode
import org.shawnz.dailytracker.data.ReminderSettings
import org.shawnz.dailytracker.data.nextAt
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * Games whose reminder times are within this time of each other are combined into one
 * reminder.
 */
val BATCH_WINDOW: Duration = Duration.ofHours(1)

/**
 * The first reminder time after [after], or null when there is none.
 *
 * With [ReminderMode.FIXED_TIME], the reminder time is [ReminderSettings.time] each day. With
 * [ReminderMode.BEFORE_RESET], each game in [games] has its own reminder time, which is
 * [ReminderSettings.leadTime] before its puzzle day changes. A game with no known rollover time
 * has the device date as its puzzle day, which changes at midnight in [zone].
 */
fun nextReminder(
    settings: ReminderSettings,
    games: List<Game>,
    after: Instant,
    zone: ZoneId,
): Instant? {
    if (!settings.enabled) return null
    return when (settings.mode) {
        ReminderMode.FIXED_TIME -> {
            nextAt(zone, settings.time, Clock.fixed(after, zone))
        }

        ReminderMode.BEFORE_RESET -> {
            games
                .filter { it.remindersEnabled }
                .minOfOrNull { reminderTimeAfter(it, settings.leadTime, after, zone) }
        }
    }
}

/** The first reminder time of [game] after [after]. */
private fun reminderTimeAfter(
    game: Game,
    leadTime: Duration,
    after: Instant,
    zone: ZoneId,
): Instant {
    // The puzzle day change for that reminder time is more than the lead time after [after].
    val dayChange = game.nextPuzzleDayChange(Clock.fixed(after.plus(leadTime), zone))
    return dayChange.minus(leadTime)
}

/**
 * A reminder posted at [now] includes every game with a reminder time up to this time. The
 * reminder after it is the first reminder time later than this.
 */
fun includedUntil(
    settings: ReminderSettings,
    now: Instant,
): Instant =
    when (settings.mode) {
        ReminderMode.FIXED_TIME -> now
        ReminderMode.BEFORE_RESET -> now.plus(BATCH_WINDOW)
    }

/**
 * The games from [games] to list in a reminder posted at the time of [clock]. Played games
 * are included.
 *
 * With [ReminderMode.BEFORE_RESET], these are the games whose puzzle day changes within
 * [ReminderSettings.leadTime] plus [BATCH_WINDOW].
 */
fun gamesToRemind(
    settings: ReminderSettings,
    games: List<Game>,
    clock: Clock,
): List<Game> {
    val reminded = games.filter { it.remindersEnabled }
    return when (settings.mode) {
        ReminderMode.FIXED_TIME -> {
            reminded
        }

        ReminderMode.BEFORE_RESET -> {
            reminded.filter {
                val timeLeft = Duration.between(clock.instant(), it.nextPuzzleDayChange(clock))
                timeLeft <= settings.leadTime.plus(BATCH_WINDOW)
            }
        }
    }
}
