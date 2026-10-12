package org.shawnz.dailytracker.parse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DatesTest {
    private val date = Regex("^$MONTH_AND_DAY$")

    private fun read(
        text: String,
        today: LocalDate,
    ): LocalDate? = date.find(text)?.value?.let { monthAndDayValue(it, today) }

    @Test
    fun `today's date is in this year`() {
        assertEquals(LocalDate.of(2026, 9, 14), read("Sep 14", LocalDate.of(2026, 9, 14)))
    }

    @Test
    fun `a date earlier in the year is in this year`() {
        assertEquals(LocalDate.of(2026, 7, 4), read("Jul 4", LocalDate.of(2026, 10, 12)))
    }

    @Test
    fun `tomorrow's date is in tomorrow's year`() {
        assertEquals(LocalDate.of(2027, 1, 1), read("Jan 1", LocalDate.of(2026, 12, 31)))
    }

    @Test
    fun `a date after tomorrow is in the year before`() {
        assertEquals(LocalDate.of(2025, 9, 16), read("Sep 16", LocalDate.of(2026, 9, 14)))
    }

    @Test
    fun `a leap day is in the latest leap year`() {
        assertEquals(LocalDate.of(2024, 2, 29), read("Feb 29", LocalDate.of(2026, 9, 14)))
    }

    @Test
    fun `a day the month does not have is null`() {
        assertNull(read("Sep 31", LocalDate.of(2026, 9, 14)))
    }

    @Test
    fun `every English locale's form reads as the same day`() {
        listOf("Sep 4", "4 Sep", "4 Sept", "04 Sept").forEach {
            assertEquals(LocalDate.of(2026, 9, 4), read(it, LocalDate.of(2026, 9, 14)), it)
        }
    }

    @Test
    fun `a month written in four letters is read`() {
        assertEquals(LocalDate.of(2026, 6, 4), read("4 June", LocalDate.of(2026, 9, 14)))
        assertEquals(LocalDate.of(2026, 7, 4), read("4 July", LocalDate.of(2026, 9, 14)))
    }

    @Test
    fun `the forms of other languages are not matched`() {
        listOf("14 sept.", "14. Sept.", "14 sept", "9月14日").forEach {
            assertNull(read(it, LocalDate.of(2026, 9, 14)), it)
        }
    }
}
