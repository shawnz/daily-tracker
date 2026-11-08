package org.shawnz.dailytracker.parse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.MonthDay

class PuzzleIdentityTest {
    private val numbering = DailyNumbering(firstDay = LocalDate.of(2024, 6, 24))
    private val september14 = LocalDate.of(2026, 9, 14)

    private fun PuzzleIdentity.resolve(latest: LocalDate = september14) = resolveDay(numbering, latest)

    @Test
    fun `a number is the day of that puzzle`() {
        assertEquals(LocalDate.of(2026, 11, 6), PuzzleIdentity.DailyNumber(866).resolve(LocalDate.of(2026, 11, 6)))
    }

    @Test
    fun `a number with no numbering has no day`() {
        assertNull(PuzzleIdentity.DailyNumber(866).resolveDay(numbering = null, latest = LocalDate.of(2026, 11, 6)))
    }

    @Test
    fun `a number before the first puzzle has no day`() {
        assertNull(PuzzleIdentity.DailyNumber(0).resolve())
    }

    @Test
    fun `a number after the latest day has no day`() {
        assertNull(PuzzleIdentity.DailyNumber(867).resolve(LocalDate.of(2026, 11, 6)))
        assertNull(PuzzleIdentity.DailyNumber(Int.MAX_VALUE).resolve())
    }

    @Test
    fun `a date is that day`() {
        assertEquals(september14, PuzzleIdentity.Date(september14).resolve())
    }

    @Test
    fun `a date after the latest day has no day`() {
        assertNull(PuzzleIdentity.Date(september14.plusDays(1)).resolve())
    }

    @Test
    fun `a month and day earlier in the year is in the latest day's year`() {
        assertEquals(LocalDate.of(2026, 7, 4), PuzzleIdentity.MonthAndDay(MonthDay.of(7, 4)).resolve())
    }

    @Test
    fun `a month and day just before a new year is in the year before`() {
        assertEquals(
            LocalDate.of(2026, 12, 31),
            PuzzleIdentity.MonthAndDay(MonthDay.of(12, 31)).resolve(LocalDate.of(2027, 1, 1)),
        )
    }

    // The nearest year is the next one, which is after the latest day.
    @Test
    fun `a month and day just after the latest day has no day`() {
        assertNull(PuzzleIdentity.MonthAndDay(MonthDay.of(1, 1)).resolve(LocalDate.of(2026, 12, 31)))
        assertNull(PuzzleIdentity.MonthAndDay(MonthDay.of(9, 16)).resolve())
    }

    // The game's latest day is a day behind the device's date, and the text was shared on the
    // device's next day.
    @Test
    fun `a month and day two days after the latest day has no day`() {
        assertNull(PuzzleIdentity.MonthAndDay(MonthDay.of(9, 15)).resolve(LocalDate.of(2026, 9, 13)))
    }

    @Test
    fun `a month and day over half a year before the latest day has no day`() {
        assertNull(PuzzleIdentity.MonthAndDay(MonthDay.of(3, 1)).resolve())
    }

    @Test
    fun `a leap day is in a nearby leap year or has no day`() {
        assertEquals(
            LocalDate.of(2028, 2, 29),
            PuzzleIdentity.MonthAndDay(MonthDay.of(2, 29)).resolve(LocalDate.of(2028, 3, 1)),
        )
        assertNull(PuzzleIdentity.MonthAndDay(MonthDay.of(2, 29)).resolve())
    }

    // 2026-09-14 is a Monday.
    @Test
    fun `a weekday is the latest one on or before the month and day`() {
        val sunday = PuzzleIdentity.WeekdayOnOrBefore(DayOfWeek.SUNDAY, MonthDay.of(9, 14))
        val monday = PuzzleIdentity.WeekdayOnOrBefore(DayOfWeek.MONDAY, MonthDay.of(9, 14))

        assertEquals(LocalDate.of(2026, 9, 13), sunday.resolve())
        assertEquals(september14, monday.resolve())
    }

    @Test
    fun `an unidentified daily and a play outside the daily series have no day`() {
        assertNull(PuzzleIdentity.UnidentifiedDaily.resolve())
        assertNull(PuzzleIdentity.NotDaily.resolve())
    }
}
