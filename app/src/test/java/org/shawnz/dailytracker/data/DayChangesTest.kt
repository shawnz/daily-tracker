package org.shawnz.dailytracker.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.shawnz.dailytracker.data.catalog.CatalogGame
import org.shawnz.dailytracker.testdoubles.testGame
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class DayChangesTest {
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
    fun `the next day change is whichever of midnight and the puzzle day is sooner`() {
        // Kinda Hard Golf rolls over at midnight in UTC-04:00, which is 04:00 on a UTC device.
        val golf = testGame(source = CatalogGame.KINDA_HARD_GOLF)

        val night = clockAt("2026-09-07T01:00", utc)
        assertEquals(
            instantAt("2026-09-07T04:00", utc),
            nextDayChange(listOf(golf), night),
        )

        // After the rollover, the game's puzzle day does not change until 04:00 tomorrow, so the
        // device's date changes first.
        val afterRollover = clockAt("2026-09-07T05:00", utc)
        assertEquals(
            instantAt("2026-09-08T00:00", utc),
            nextDayChange(listOf(golf), afterRollover),
        )
    }

    @Test
    fun `with no games, the next day change is midnight`() {
        val clock = clockAt("2026-09-07T21:00", newYork)
        assertEquals(
            instantAt("2026-09-08T00:00", newYork),
            nextDayChange(emptyList(), clock),
        )
    }
}
