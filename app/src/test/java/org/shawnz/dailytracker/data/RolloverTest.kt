package org.shawnz.dailytracker.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.shawnz.dailytracker.data.catalog.CatalogGame
import org.shawnz.dailytracker.testdoubles.testGame
import java.time.Clock
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class RolloverTest {
    private val newYork = ZoneId.of("America/New_York")
    private val utc = ZoneId.of("UTC")

    private fun instantAt(
        local: String,
        zone: ZoneId,
    ): Instant = LocalDateTime.parse(local).atZone(zone).toInstant()

    private fun clockAt(
        local: String,
        zone: ZoneId,
    ): Clock = Clock.fixed(instantAt(local, zone), zone)

    @Test
    fun `local midnight uses the device date`() {
        val clock = clockAt("2026-09-07T23:30", newYork)
        assertEquals(
            LocalDate.of(2026, 9, 7),
            currentPuzzleDay(RolloverType.LOCAL_MIDNIGHT, null, 0, clock),
        )
    }

    @Test
    fun `utc rollover can be a day ahead of the device`() {
        // 23:30 in New York is 03:30 of the next day in UTC. A game that resets at
        // midnight UTC is therefore on the next puzzle, while the local date is unchanged.
        val clock = clockAt("2026-09-07T23:30", newYork)
        assertEquals(
            LocalDate.of(2026, 9, 8),
            currentPuzzleDay(RolloverType.FIXED_ZONE, "UTC", 0, clock),
        )
    }

    @Test
    fun `evening rollover advances before local midnight`() {
        val minuteOfDay = 22 * 60
        val after = clockAt("2026-09-07T22:30", newYork)
        assertEquals(
            LocalDate.of(2026, 9, 8),
            currentPuzzleDay(RolloverType.FIXED_ZONE, "America/New_York", minuteOfDay, after),
        )

        val before = clockAt("2026-09-07T21:00", newYork)
        assertEquals(
            LocalDate.of(2026, 9, 7),
            currentPuzzleDay(RolloverType.FIXED_ZONE, "America/New_York", minuteOfDay, before),
        )
    }

    @Test
    fun `a fixed zone that cannot be read throws`() {
        val clock = clockAt("2026-09-07T12:00", newYork)
        assertThrows(DateTimeException::class.java) {
            currentPuzzleDay(RolloverType.FIXED_ZONE, "Not/AZone", 0, clock)
        }
    }

    @Test
    fun `unscheduled uses the device date and has no rollover time`() {
        val clock = clockAt("2026-09-07T23:30", newYork)
        assertEquals(
            LocalDate.of(2026, 9, 7),
            currentPuzzleDay(RolloverType.UNSCHEDULED, null, 0, clock),
        )
        assertNull(nextRollover(RolloverType.UNSCHEDULED, null, 0, clock))
    }

    @Test
    fun `an unscheduled day still changes at local midnight`() {
        val clock = clockAt("2026-09-07T23:30", newYork)
        assertEquals(
            instantAt("2026-09-08T00:00", newYork),
            nextPuzzleDayChange(RolloverType.UNSCHEDULED, null, 0, clock),
        )
    }

    @Test
    fun `a scheduled day changes at its own rollover`() {
        val clock = clockAt("2026-09-07T21:00", newYork)
        assertEquals(
            nextRollover(RolloverType.FIXED_ZONE, "UTC", 0, clock),
            nextPuzzleDayChange(RolloverType.FIXED_ZONE, "UTC", 0, clock),
        )
    }

    @Test
    fun `next rollover for local midnight is the coming midnight`() {
        val clock = clockAt("2026-09-07T23:30", newYork)
        assertEquals(
            instantAt("2026-09-08T00:00", newYork),
            nextRollover(RolloverType.LOCAL_MIDNIGHT, null, 0, clock),
        )
    }

    @Test
    fun `next rollover is the moment the puzzle day changes`() {
        val minuteOfDay = 22 * 60
        val boundary = instantAt("2026-09-07T22:00", newYork)
        assertEquals(
            boundary,
            nextRollover(
                RolloverType.FIXED_ZONE,
                "America/New_York",
                minuteOfDay,
                clockAt("2026-09-07T21:00", newYork),
            ),
        )

        // On the boundary the day has already advanced, so the next one is a day later.
        val at = Clock.fixed(boundary, newYork)
        assertEquals(
            LocalDate.of(2026, 9, 8),
            currentPuzzleDay(RolloverType.FIXED_ZONE, "America/New_York", minuteOfDay, at),
        )
        assertEquals(
            instantAt("2026-09-08T22:00", newYork),
            nextRollover(RolloverType.FIXED_ZONE, "America/New_York", minuteOfDay, at),
        )
    }

    @Test
    fun `the next day change is whichever of midnight and the puzzle day is sooner`() {
        // Kinda Hard Golf rolls over at midnight in UTC-04:00, which is 04:00 on a UTC device.
        val golf = testGame(source = CatalogGame.KINDA_HARD_GOLF)

        val night = clockAt("2026-09-07T01:00", utc)
        assertEquals(
            instantAt("2026-09-07T04:00", utc),
            nextDayChange(listOf(golf), night),
        )

        // Past the rollover the game is settled until 04:00 tomorrow, so the next thing to
        // move is the device's own date.
        val afterRollover = clockAt("2026-09-07T05:00", utc)
        assertEquals(
            instantAt("2026-09-08T00:00", utc),
            nextDayChange(listOf(golf), afterRollover),
        )
    }

    @Test
    fun `no games still wakes at midnight`() {
        val clock = clockAt("2026-09-07T21:00", newYork)
        assertEquals(
            instantAt("2026-09-08T00:00", newYork),
            nextDayChange(emptyList(), clock),
        )
    }

    @Test
    fun `a fixed offset holds through a daylight saving change`() {
        // Kinda Hard Golf resets at midnight UTC-04:00 all year, even while New York is at
        // UTC-05:00, as it is on 2026-03-07.
        val zone = "UTC-04:00"
        val winter = clockAt("2026-03-07T03:30", utc)
        assertEquals(
            LocalDate.of(2026, 3, 6),
            currentPuzzleDay(RolloverType.FIXED_ZONE, zone, 0, winter),
        )
        assertEquals(
            instantAt("2026-03-07T04:00", utc),
            nextRollover(RolloverType.FIXED_ZONE, zone, 0, winter),
        )

        val summer = clockAt("2026-09-07T03:30", utc)
        assertEquals(
            instantAt("2026-09-07T04:00", utc),
            nextRollover(RolloverType.FIXED_ZONE, zone, 0, summer),
        )
    }
}
