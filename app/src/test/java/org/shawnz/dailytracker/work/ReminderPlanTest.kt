package org.shawnz.dailytracker.work

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.shawnz.dailytracker.data.CustomGame
import org.shawnz.dailytracker.data.ReminderMode
import org.shawnz.dailytracker.data.ReminderSettings
import org.shawnz.dailytracker.data.catalog.CatalogGame
import org.shawnz.dailytracker.testdoubles.testGame
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * The device is in New York in January, which is five hours behind UTC. On the device's
 * clock, the puzzle day of [utcMidnight] changes at 19:00 and the puzzle day of [fixedOffset]
 * at 23:00.
 */
class ReminderPlanTest {
    private val zone = ZoneId.of("America/New_York")

    private val localMidnight = testGame(id = 1, source = CatalogGame.WORDLE)
    private val utcMidnight = testGame(id = 2, source = CatalogGame.FOODGUESSR)
    private val fixedOffset = testGame(id = 3, source = CatalogGame.KINDA_HARD_GOLF)
    private val unscheduled = testGame(id = 4, source = CustomGame("My Game", "https://example.com"))

    private val fixedTime = ReminderSettings(time = LocalTime.of(20, 0))

    private fun beforeReset(hours: Long) = ReminderSettings(mode = ReminderMode.BEFORE_RESET, leadTime = Duration.ofHours(hours))

    private fun at(local: String): Instant = LocalDateTime.parse(local).atZone(zone).toInstant()

    private fun clockAt(local: String): Clock = Clock.fixed(at(local), zone)

    @Test
    fun `a fixed time later today is next`() {
        val next = nextReminder(fixedTime, listOf(localMidnight), at("2026-01-15T12:00"), zone)

        assertEquals(at("2026-01-15T20:00"), next)
    }

    @Test
    fun `a fixed time that has passed is next tomorrow`() {
        val next = nextReminder(fixedTime, listOf(localMidnight), at("2026-01-15T21:00"), zone)

        assertEquals(at("2026-01-16T20:00"), next)
    }

    @Test
    fun `no reminder is due when reminders are off`() {
        val games = listOf(localMidnight)
        val now = at("2026-01-15T12:00")

        assertNull(nextReminder(fixedTime.copy(enabled = false), games, now, zone))
        assertNull(nextReminder(beforeReset(4).copy(enabled = false), games, now, zone))
    }

    @Test
    fun `a reminder time is the lead time before the puzzle day changes`() {
        val next = nextReminder(beforeReset(4), listOf(localMidnight), at("2026-01-15T12:00"), zone)

        assertEquals(at("2026-01-15T20:00"), next)
    }

    @Test
    fun `inside the lead time the next reminder time is on the following day`() {
        val next = nextReminder(beforeReset(4), listOf(localMidnight), at("2026-01-15T21:00"), zone)

        assertEquals(at("2026-01-16T20:00"), next)
    }

    @Test
    fun `the soonest game sets the next reminder`() {
        val games = listOf(localMidnight, utcMidnight)

        val next = nextReminder(beforeReset(2), games, at("2026-01-15T12:00"), zone)

        assertEquals(at("2026-01-15T17:00"), next)
    }

    @Test
    fun `an unscheduled game is reminded before midnight on the device`() {
        val next = nextReminder(beforeReset(4), listOf(unscheduled), at("2026-01-15T12:00"), zone)

        assertEquals(at("2026-01-15T20:00"), next)
    }

    @Test
    fun `no reminder is due before a reset when no game has reminders on`() {
        val muted = localMidnight.copy(remindersEnabled = false)
        val now = at("2026-01-15T12:00")

        assertNull(nextReminder(beforeReset(4), emptyList(), now, zone))
        assertNull(nextReminder(beforeReset(4), listOf(muted), now, zone))
    }

    @Test
    fun `a reminder at a fixed time includes every game with reminders on`() {
        val muted = utcMidnight.copy(remindersEnabled = false)

        val games = gamesToRemind(fixedTime, listOf(localMidnight, muted), clockAt("2026-01-15T20:00"))

        assertEquals(listOf(localMidnight), games)
    }

    @Test
    fun `a reminder before a reset includes the games approaching their reset`() {
        val games = gamesToRemind(beforeReset(2), listOf(localMidnight, utcMidnight), clockAt("2026-01-15T17:00"))

        assertEquals(listOf(utcMidnight), games)
    }

    @Test
    fun `games that reset within the batch window have one reminder`() {
        val settings = beforeReset(2)
        val games = listOf(localMidnight, fixedOffset)
        val clock = clockAt("2026-01-15T21:00")

        val reminded = gamesToRemind(settings, games, clock)
        val next = nextReminder(settings, games, includedUntil(settings, clock.instant()), zone)

        assertEquals(games, reminded)
        assertEquals(at("2026-01-16T21:00"), next)
    }

    @Test
    fun `a game is reminded again until its puzzle day changes`() {
        val games = listOf(localMidnight, utcMidnight)

        val reminded = gamesToRemind(beforeReset(6), games, clockAt("2026-01-15T18:00"))

        assertEquals(games, reminded)
    }

    @Test
    fun `a game whose puzzle day has changed is left out`() {
        val games = listOf(localMidnight, utcMidnight)

        val reminded = gamesToRemind(beforeReset(2), games, clockAt("2026-01-15T22:00"))

        assertEquals(listOf(localMidnight), reminded)
    }
}
