package org.shawnz.dailytracker.time

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class RolloverRuleTest {
    private val newYork = ZoneId.of("America/New_York")
    private val utc = ZoneId.of("UTC")
    private val tokyo = ZoneId.of("Asia/Tokyo")
    private val honolulu = ZoneId.of("Pacific/Honolulu")

    private val chicagoUnknownTime = RolloverRule(RolloverZone.Named("America/Chicago"), time = null)

    private fun named(
        zone: String,
        time: LocalTime = LocalTime.MIDNIGHT,
    ) = RolloverRule(RolloverZone.Named(zone), time)

    private fun september(day: Int): LocalDate = LocalDate.of(2026, 9, day)

    private fun instantAt(
        local: String,
        zone: ZoneId,
    ): Instant = LocalDateTime.parse(local).atZone(zone).toInstant()

    private fun clockAt(
        local: String,
        zone: ZoneId,
    ): Clock = Clock.fixed(instantAt(local, zone), zone)

    @Test
    fun `with local midnight, the only possible day is the device date`() {
        val clock = clockAt("2026-09-07T23:30", newYork)
        assertEquals(september(7)..september(7), RolloverRule.LOCAL_MIDNIGHT.possiblePuzzleDays(rolloverObserved = false, clock = clock))
    }

    @Test
    fun `utc rollover can be a day ahead of the device`() {
        // 23:30 in New York is 03:30 of the next day in UTC. A game that resets at
        // midnight UTC is therefore on the next puzzle, while the local date is unchanged.
        val clock = clockAt("2026-09-07T23:30", newYork)
        assertEquals(september(8)..september(8), named("UTC").possiblePuzzleDays(rolloverObserved = false, clock = clock))
    }

    @Test
    fun `evening rollover advances before local midnight`() {
        val rule = named("America/New_York", LocalTime.of(22, 0))
        val after = clockAt("2026-09-07T22:30", newYork)
        assertEquals(september(8)..september(8), rule.possiblePuzzleDays(rolloverObserved = false, clock = after))

        val before = clockAt("2026-09-07T21:00", newYork)
        assertEquals(september(7)..september(7), rule.possiblePuzzleDays(rolloverObserved = false, clock = before))
    }

    @Test
    fun `a zone that cannot be read throws`() {
        assertThrows(DateTimeException::class.java) { RolloverZone.Named("Not/AZone") }
    }

    @Test
    fun `the default rule has no rollover time`() {
        assertNull(RolloverRule.DEFAULT.nextRollover(clockAt("2026-09-07T23:30", newYork)))
    }

    @Test
    fun `possible days for the default rule are the device date and the day after`() {
        val clock = clockAt("2026-09-07T23:30", newYork)
        assertEquals(september(7)..september(8), RolloverRule.DEFAULT.possiblePuzzleDays(rolloverObserved = false, clock = clock))
    }

    @Test
    fun `possible days are only the day after when the rollover is observed`() {
        val clock = clockAt("2026-09-07T23:30", newYork)
        assertEquals(september(8)..september(8), RolloverRule.DEFAULT.possiblePuzzleDays(rolloverObserved = true, clock = clock))
    }

    // At 08:00 on 8 September in Tokyo, it is 18:00 on 7 September in Chicago.
    @Test
    fun `possible days for a named zone are the dates in that zone, not on the device`() {
        val clock = clockAt("2026-09-08T08:00", tokyo)
        assertEquals(september(7)..september(8), chicagoUnknownTime.possiblePuzzleDays(rolloverObserved = false, clock = clock))
        assertEquals(september(8)..september(8), chicagoUnknownTime.possiblePuzzleDays(rolloverObserved = true, clock = clock))
    }

    // At 20:00 on 7 September in Honolulu, it is 01:00 on 8 September in Chicago.
    @Test
    fun `earliest possible day can be after the device date`() {
        val clock = clockAt("2026-09-07T20:00", honolulu)
        assertEquals(september(8)..september(9), chicagoUnknownTime.possiblePuzzleDays(rolloverObserved = false, clock = clock))
    }

    @Test
    fun `next change for the default rule is device midnight`() {
        val clock = clockAt("2026-09-08T12:00", tokyo)
        assertEquals(instantAt("2026-09-09T00:00", tokyo), RolloverRule.DEFAULT.nextPossiblePuzzleDaysChange(clock))
    }

    // Midnight in Chicago is 01:00 in New York.
    @Test
    fun `next change for an unknown time is midnight in the zone`() {
        val clock = clockAt("2026-09-07T23:30", newYork)
        assertEquals(instantAt("2026-09-08T01:00", newYork), chicagoUnknownTime.nextPossiblePuzzleDaysChange(clock))
    }

    @Test
    fun `next change for a known time is the rollover time`() {
        val clock = clockAt("2026-09-07T21:00", newYork)
        assertEquals(named("UTC").nextRollover(clock), named("UTC").nextPossiblePuzzleDaysChange(clock))
    }

    @Test
    fun `next rollover for local midnight is the coming midnight`() {
        val clock = clockAt("2026-09-07T23:30", newYork)
        assertEquals(instantAt("2026-09-08T00:00", newYork), RolloverRule.LOCAL_MIDNIGHT.nextRollover(clock))
    }

    @Test
    fun `next rollover is the moment the puzzle day changes`() {
        val rule = named("America/New_York", LocalTime.of(22, 0))
        val boundary = instantAt("2026-09-07T22:00", newYork)
        assertEquals(boundary, rule.nextRollover(clockAt("2026-09-07T21:00", newYork)))

        // On the boundary the day has already advanced, so the next one is a day later.
        val at = Clock.fixed(boundary, newYork)
        assertEquals(september(8)..september(8), rule.possiblePuzzleDays(rolloverObserved = false, clock = at))
        assertEquals(instantAt("2026-09-08T22:00", newYork), rule.nextRollover(at))
    }

    @Test
    fun `a fixed offset does not change with daylight saving`() {
        // A rule at UTC-04:00 changes at 04:00 UTC all year, even while New York is at
        // UTC-05:00, as it is on 2026-03-07.
        val rule = named("UTC-04:00")
        val winter = clockAt("2026-03-07T03:30", utc)
        val march6 = LocalDate.of(2026, 3, 6)
        assertEquals(march6..march6, rule.possiblePuzzleDays(rolloverObserved = false, clock = winter))
        assertEquals(instantAt("2026-03-07T04:00", utc), rule.nextRollover(winter))

        val summer = clockAt("2026-09-07T03:30", utc)
        assertEquals(instantAt("2026-09-07T04:00", utc), rule.nextRollover(summer))
    }
}
